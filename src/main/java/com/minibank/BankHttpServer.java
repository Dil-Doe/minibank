package com.minibank;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

public class BankHttpServer {

    private final BankService bankService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BankHttpServer(BankService bankService) {this.bankService = bankService;}

    public void start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0
        );

        server.createContext("/transfer", this::handleTransfer);
        server.createContext("/clients", this::handleClients);
        server.createContext("/accounts", this::handleAccounts);

        server.start();

        System.out.println("HTTP-сервер запущен: http://localhost:8080");
    }

    private void handleTransfer(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Разрешён только метод POST");
            return;
        }

        JsonNode request;

        try {
            request = objectMapper.readTree(exchange.getRequestBody());
        } catch (IOException e) {
            sendResponse(exchange, 400, "Некорректный JSON");
            return;
        }

        if (request == null
                || !request.hasNonNull("fromAccountId")
                || !request.hasNonNull("toAccountId")
                || !request.hasNonNull("amount")
                || !request.get("fromAccountId").canConvertToLong()
                || !request.get("toAccountId").canConvertToLong()
                || !request.get("amount").isNumber()) {
            sendResponse(exchange, 400, "Некорректные данные запроса");
            return;
        }

        Long fromAccountId = request.get("fromAccountId").asLong();
        Long toAccountId = request.get("toAccountId").asLong();
        BigDecimal amount = request.get("amount").decimalValue();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            sendResponse(exchange, 400, "Сумма должна быть больше нуля");
            return;
        }

        try {
            bankService.transfer(fromAccountId, toAccountId, amount);
            sendResponse(exchange, 200, "Перевод успешно выполнен");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            if (e.getMessage() != null
                    && e.getMessage().contains("Недостаточно средств")) {
                sendResponse(exchange, 409, e.getMessage());
            } else {
                sendResponse(exchange, 500, "Внутренняя ошибка сервера");
            }
        }
    }

    private void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response
    ) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (var output = exchange.getResponseBody()) {output.write(bytes);}
    }

    private void handleClients(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Разрешён только метод POST");
            return;
        }

        JsonNode request;

        try {
            request = objectMapper.readTree(exchange.getRequestBody());
        } catch (IOException e) {
            sendResponse(exchange, 400, "Некорректный JSON");
            return;
        }

        if (request == null
                || !request.hasNonNull("name")
                || !request.hasNonNull("email")
                || !request.get("name").isTextual()
                || !request.get("email").isTextual()
                || request.get("name").asText().isBlank()
                || request.get("email").asText().isBlank()) {
            sendResponse(exchange, 400, "Необходимо указать имя и email");
            return;
        }

        try {
            Client client = bankService.registerClient(
                    request.get("name").asText(),
                    request.get("email").asText()
            );

            sendResponse(exchange, 200,
                    "Клиент успешно создан. ID: " + client.getId());

        } catch (Exception e) {
            sendResponse(exchange, 500, "Не удалось создать клиента");
        }
    }

    private void handleAccounts(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Разрешён только метод POST");
            return;
        }

        JsonNode request;

        try {
            request = objectMapper.readTree(exchange.getRequestBody());
        } catch (IOException e) {
            sendResponse(exchange, 400, "Некорректный JSON");
            return;
        }

        if (request == null
                || !request.hasNonNull("clientId")
                || !request.hasNonNull("accountNumber")
                || !request.get("clientId").canConvertToLong()
                || !request.get("accountNumber").isTextual()
                || request.get("accountNumber").asText().isBlank()) {
            sendResponse(exchange, 400, "Необходимо указать clientId и accountNumber");
            return;
        }

        try {
            Account account = bankService.openAccount(
                    request.get("clientId").asLong(),
                    request.get("accountNumber").asText()
            );

            sendResponse(exchange, 200,
                    "Счёт успешно создан. ID: " + account.getId());

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 404, e.getMessage());
        } catch (Exception e) {
            sendResponse(exchange, 500, "Не удалось создать счёт");
        }
    }
}
