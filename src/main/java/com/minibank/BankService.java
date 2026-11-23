package com.minibank;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class BankService {

    private final Properties dbProperties = new Properties();

    public BankService() {
        try (InputStream input = getClass().getResourceAsStream("/liquibase.properties")) {

            if (input == null) {
                throw new IllegalStateException("Файл liquibase.properties не найден");
            }

            dbProperties.load(input);

        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить настройки базы данных", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                dbProperties.getProperty("url"),
                dbProperties.getProperty("username"),
                dbProperties.getProperty("password")
        );
    }

    public Client registerClient(String name, String email) {
        String sql = """
                INSERT INTO clients (name, email)
                VALUES (?, ?)
                """;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Client(keys.getLong(1), name, email);
                }
            }

            throw new SQLException("Не удалось получить ID клиента");

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось зарегистрировать клиента: " + e.getMessage(), e);
        }
    }

    public Account openAccount(Long clientId, String accountNumber) {
        getClient(clientId);

        String sql = """
                INSERT INTO accounts (client_id, account_number, balance)
                VALUES (?, ?, 0)
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, clientId);
            statement.setString(2, accountNumber);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Account(
                            keys.getLong(1),
                            clientId,
                            accountNumber,
                            BigDecimal.ZERO
                    );
                }
            }

            throw new SQLException("Не удалось получить ID счёта");

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось открыть счёт: " + e.getMessage(), e);
        }
    }

    public Account getAccount(Long accountId) {
        String sql = """
                SELECT id, client_id, account_number, balance
                FROM accounts
                WHERE id = ?
                """;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, accountId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Account(
                            result.getLong("id"),
                            result.getLong("client_id"),
                            result.getString("account_number"),
                            result.getBigDecimal("balance")
                    );
                }
            }

            throw new IllegalArgumentException("Счёт не найден: " + accountId);

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось получить счёт", e);
        }
    }

    public Client getClient(Long clientId) {
        String sql = """
                SELECT id, name, email
                FROM clients
                WHERE id = ?
                """;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, clientId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Client(
                            result.getLong("id"),
                            result.getString("name"),
                            result.getString("email")
                    );
                }
            }

            throw new IllegalArgumentException("Клиент не найден: " + clientId);

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось получить клиента", e);
        }
    }

    public void deposit(Long accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть положительной");
        }

        Account account = getAccount(accountId);
        BigDecimal newBalance = account.getBalance().add(amount);

        updateBalance(accountId, newBalance);
        recordTransaction(accountId, amount, "DEPOSIT");
    }

    public void transfer(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount
    ) {
        Account from = getAccount(fromAccountId);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств на счёте " + fromAccountId);
        }

        from.setBalance(from.getBalance().subtract(amount));
        updateBalance(fromAccountId, from.getBalance());
        recordTransaction(fromAccountId, amount.negate(), "TRANSFER_OUT");

        Account to = getAccount(toAccountId);

        to.setBalance(to.getBalance().add(amount));
        updateBalance(toAccountId, to.getBalance());
        recordTransaction(toAccountId, amount, "TRANSFER_IN");
    }

    private void updateBalance(Long accountId, BigDecimal balance) {
        String sql = """
                UPDATE accounts
                SET balance = ?
                WHERE id = ?
                """;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, balance);
            statement.setLong(2, accountId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось обновить баланс счёта", e);
        }
    }

    private void recordTransaction(
            Long accountId,
            BigDecimal amount,
            String type
    ) {
        String sql = """
                INSERT INTO transactions (account_id, amount, type)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, accountId);
            statement.setBigDecimal(2, amount);
            statement.setString(3, type);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось записать транзакцию", e);
        }
    }
}
