package com.minibank;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BankHttpServerGet {

    private final BankService bankService;

    public BankHttpServerGet(BankService bankService) {this.bankService = bankService;}

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/transfer", exchange -> {
            String response;

            try {
                Map<String, String> params = parseQuery(
                        exchange.getRequestURI().getRawQuery()
                );

                Long fromAccountId = Long.parseLong(params.get("fromAccountId"));
                Long toAccountId = Long.parseLong(params.get("toAccountId"));
                BigDecimal amount = new BigDecimal(params.get("amount"));

                bankService.transfer(fromAccountId, toAccountId, amount);

                response = "Перевод успешно выполнен";

            } catch (Exception e) {
                response = "Ошибка: " + e.getMessage();
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "text/plain; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, bytes.length);

            try (var output = exchange.getResponseBody()) {output.write(bytes);}
        });

        server.start();

        System.out.println("HTTP-сервер запущен: http://localhost:8080");
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();

        if (query == null) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);

            if (pair.length == 2) {
                params.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                );
            }
        }

        return params;
    }
}
