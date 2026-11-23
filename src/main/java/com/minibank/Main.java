package com.minibank;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        BankService bank = new BankService();

        BankHttpServer server = new BankHttpServer(bank);
        server.start();
    }
}
