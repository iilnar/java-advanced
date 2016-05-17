package ru.ifmo.ctddev.sabirzyanov.hello;

/**
 * Created by ilnar on 09.05.16.
 */

public class Tester {

    public static void main(String[] args) throws ClassNotFoundException {
        try (HelloUDPServer server = new HelloUDPServer()) {
            server.start(8742, 10);
            HelloUDPClient client = new HelloUDPClient();
            client.start("localhost", 8742, "Коля", 10, 10);
        }
    }
}
