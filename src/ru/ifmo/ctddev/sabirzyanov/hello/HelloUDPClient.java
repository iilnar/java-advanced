package ru.ifmo.ctddev.sabirzyanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ilnar on 07.05.16.
 */
public class HelloUDPClient implements HelloClient {
    private final Charset charset = Charset.forName("UTF-8");
    private final String RESPONSE_PREFIX = new String("Hello, %s".getBytes(), charset);

    /**
     * Main function
     * @param args [host port requestPrefix countOfRequests countOfThreads]
     */
    public static void main(String[] args) {
        if (args == null || args.length < 5) {
            System.out.println("Usage: host port requestPrefix countOfRequests countOfThreads");
            return;
        }

        new HelloUDPClient().start(args[0], Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]));
    }

    /**
     * Starts sending request to host
     * @param host host address
     * @param port host's port
     * @param prefix prefix of request
     * @param requests requests count
     * @param threads threads count
     */
    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        assert 0 < port && port < 0xffff;
        assert 0 <= requests;
        assert 1 < threads;
        final String correctPrefix = new String(prefix.getBytes(), charset);
        ExecutorService service = Executors.newFixedThreadPool(threads);
        try {
            InetAddress address = InetAddress.getByName(host);
            for (int i = 0; i < threads; i++) {
                final int threadID = i;
                service.submit(() -> {
                    try (DatagramSocket datagramSocket = new DatagramSocket()) {
                        datagramSocket.setSoTimeout(50);
                        for (int j = 0; j < requests; j++) {
                            String request = String.format("%s%d_%d", correctPrefix, threadID, j);
                            String expectation = String.format(RESPONSE_PREFIX, request);
                            String reality = "";
                            DatagramPacket out = new DatagramPacket(request.getBytes(charset), request.getBytes(charset).length, address, port);
                            DatagramPacket in = new DatagramPacket(new byte[expectation.getBytes().length + 1],
                                    expectation.getBytes(charset).length + 1);
                            while (!expectation.equals(reality)) {
                                try {
                                    datagramSocket.send(out);
                                    System.out.printf("Sent %s\n", request);
                                    datagramSocket.receive(in);
                                    reality = new String(in.getData(), in.getOffset(), in.getLength(), charset);
                                    System.out.printf("Received %s\n", reality);
                                } catch (IOException ignored) {
                                    System.out.printf("Error %s\n", request);
                                }
                            }

                        }
                    } catch (SocketException ignored) {
                    }
                });
            }
            service.shutdownNow();
            service.awaitTermination(10, TimeUnit.SECONDS);
        } catch (UnknownHostException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
