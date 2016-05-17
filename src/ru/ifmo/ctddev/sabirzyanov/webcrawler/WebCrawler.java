package ru.ifmo.ctddev.sabirzyanov.webcrawler;

import info.kgeorgiy.java.advanced.crawler.*;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by ilnar on 18.04.16.
 */
public class WebCrawler implements Crawler {
    private final static int DEFAULT_DOWNLOADS = 100;
    private final static int DEFAULT_EXTRACTORS = 100;
    private final static int DEFAULT_PER_HOST = 100;
    private final static Object DEFAULT_OBJECT = new Object();
    private final ExecutorService downloadService;
    private final ExecutorService extractService;
    private final Downloader downloader;
    private final int perHost;

    /**
     * Main function for running from terminal.
     * @param args argument [url [downloads [extractors [perHost]]]]
     */
    public static void main(String args[]) {
        if (args == null || args.length == 0) {
            System.out.println("Usage: WebCrawler url [downloads [extractors [perHost]]]");
            return;
        }
        int downloads = (args.length < 2 ? DEFAULT_DOWNLOADS : Integer.parseInt(args[2]));
        int extractors = (args.length < 3 ? DEFAULT_EXTRACTORS : Integer.parseInt(args[3]));
        int perHost = (args.length < 4 ? DEFAULT_PER_HOST : Integer.parseInt(args[4]));
        try (Crawler crawler = new WebCrawler(new CachingDownloader(new File("./tmp/")),
                downloads, extractors, perHost)) {
            Result res = crawler.download("http://neerc.ifmo.ru/subregions/index.html", 3);
            System.out.println("Results:");
            res.getDownloaded().forEach(System.out::println);
            System.out.println("Errors:");
            res.getErrors().keySet().forEach(System.out::println);
        } catch (IOException ignored){
        }
    }

    /**
     * Public constructor.
     * @param downloader {@link Downloader} file downloader
     * @param downloaders number of parallel downloads permitted
     * @param extractors number of parallel extractions permitted
     * @param perHost number off maximum parallel downloads per one host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        System.err.printf("ds: %d, es: %d, ph: %d\n", downloaders, extractors, perHost);
    }

    /**
     * Gets all URL's reachable from {@code url} using {@code depth} jumps.
     * @param url URL to start
     * @param depth depth of walking
     * @return {@link Result} of reachable sites.
     */
    @Override
    public Result download(String url, int depth) {
        System.err.printf("URL: %s, depth = %d\n", url, depth);
        final PriorityBlockingQueue<Pair<String, Integer>> queue = new PriorityBlockingQueue<>(100,
                (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        queue.add(new Pair<>(url, depth));
        final ConcurrentHashMap<String, Object> inQueue = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Object> visited = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Semaphore> hostAvailable = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();
        final AtomicInteger waiting = new AtomicInteger();
        waiting.incrementAndGet();
        inQueue.put(url, DEFAULT_OBJECT);
        downloadService.submit(() -> download(waiting, queue, inQueue, visited, errors, hostAvailable));
        while (waiting.get() != 0) {
        }
        return new Result(visited.keySet().stream().collect(Collectors.toList()), errors);
    }

    private void download(final AtomicInteger waiting,
                          final PriorityBlockingQueue<Pair<String, Integer>> queue,
                          final ConcurrentHashMap<String, Object> inQueue,
                          final ConcurrentHashMap<String, Object> visited,
                          final ConcurrentHashMap<String, IOException> errors,
                          final ConcurrentHashMap<String, Semaphore> hostAvailable) {
        try {
            Pair<String, Integer> current = queue.poll();
            String url = current.getKey();
            Integer depth = current.getValue();
            String host = URLUtils.getHost(url);
            hostAvailable.putIfAbsent(host, new Semaphore(perHost));
            if (hostAvailable.get(host).tryAcquire()) {
                try {
                    Document document = downloader.download(url);
                    visited.put(url, DEFAULT_OBJECT);
                    waiting.incrementAndGet();
                    extractService.submit(() -> extract(document, url, depth, waiting, queue, inQueue, visited, errors, hostAvailable));
                } catch (IOException ignored) {
                    errors.put(url, ignored);
                } finally {
                    hostAvailable.get(host).release();
                }
            } else {
                waiting.incrementAndGet();
                queue.add(current);
                downloadService.submit(() -> download(waiting, queue, inQueue, visited, errors, hostAvailable));
            }
        } catch (MalformedURLException ignored) {
        } finally {
            waiting.decrementAndGet();
        }
    }

    private void extract(Document document, String urll, int depth,
                         final AtomicInteger waiting,
                         final PriorityBlockingQueue<Pair<String, Integer>> queue,
                         final ConcurrentHashMap<String, Object> inQueue,
                         final ConcurrentHashMap<String, Object> visited,
                         final ConcurrentHashMap<String, IOException> errors,
                         final ConcurrentHashMap<String, Semaphore> hostAvailable) {
        try {
            if (depth > 1) {
                List<String> urls = document.extractLinks();
                urls.parallelStream().distinct().filter(url -> inQueue.putIfAbsent(url, DEFAULT_OBJECT) == null).forEach(url -> {
                        waiting.incrementAndGet();
                        queue.add(new Pair<>(url, depth - 1));
                        downloadService.submit(() -> download(waiting, queue, inQueue, visited, errors, hostAvailable));
                });
            }
        } catch (IOException e) {
            errors.put(urll, e);
        } finally {
            waiting.decrementAndGet();
        }
    }

    /**
     * Closes.
     */
    @Override
    public void close() {
        downloadService.shutdownNow();
        extractService.shutdownNow();
    }
}
