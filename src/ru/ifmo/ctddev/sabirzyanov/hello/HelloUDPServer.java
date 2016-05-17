package ru.ifmo.ctddev.sabirzyanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ilnar on 07.05.16.
 */
public class HelloUDPServer implements HelloServer {
    private static int BUFFER_SIZE;
    private final ConcurrentLinkedQueue<DatagramSocket> receive = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ExecutorService> executors = new ConcurrentLinkedQueue<>();
    private final Charset charset = Charset.forName("UTF-8");
    private final String RESPONSE_PREFIX = new String("Hello, %s".getBytes(), charset);

    /**
     * Public constructor.
     * @param args Usage: port threadsCount
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("Usage: port threadsCount");
            return;
        }
        new HelloUDPServer().start(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
    }

    /**
     * Starts listening {@code port} in {@code threads} threads.
     * @param port number of port to listen
     * @param threads number of threads
     */
    @Override
    public void start(int port, int threads) {
        assert 0 < port && port < 0xffff;
        assert 0 < threads;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        executors.add(service);
        try {
            DatagramSocket socket = new DatagramSocket(port);
            receive.add(socket);
            BUFFER_SIZE = socket.getReceiveBufferSize();
            for (int i = 0; i < threads; i++) {
                service.submit(() -> {
                    try (DatagramSocket sendingSocket = new DatagramSocket()) {
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                        while (!Thread.interrupted() && !socket.isClosed()) {
                            socket.receive(datagramPacket);
                            String received = new String(datagramPacket.getData(), datagramPacket.getOffset(),
                                    datagramPacket.getLength(), charset);
                            System.out.printf("Received %s\n", received);
                            String response = String.format(RESPONSE_PREFIX, received);
                            sendingSocket.send(new DatagramPacket(response.getBytes(charset), response.getBytes(charset).length,
                                    datagramPacket.getAddress(), datagramPacket.getPort()));
                        }
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (SocketException ignored) {
        }
    }

    /**
     * Closes class.
     */
    @Override
    public void close() {
        synchronized (executors) {
            executors.forEach(ExecutorService::shutdownNow);
            executors.clear();
        }
        synchronized (receive) {
            receive.forEach(DatagramSocket::close);
            receive.clear();
        }
    }
}
