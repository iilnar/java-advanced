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

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        System.err.printf("ds: %d, es: %d, ph: %d\n", downloaders, extractors, perHost);
    }

    @Override
    public Result download(String url, int depth) {
        System.err.printf("URL: %s, depth = %d\n", url, depth);
        final ConcurrentHashMap<String, Object> inQueue = new ConcurrentHashMap<>();
        inQueue.put(url, DEFAULT_OBJECT);
        final ConcurrentHashMap<String, Object> visited = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Semaphore> hostAvailable = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();
        final AtomicInteger waiting = new AtomicInteger();
        waiting.incrementAndGet();
        downloadService.submit(() -> download(waiting, inQueue, visited, errors, hostAvailable, new Pair<>(url, depth)));
        while (waiting.get() != 0) {
        }
        return new Result(visited.keySet().stream().collect(Collectors.toList()), errors);
    }

    private void download(final AtomicInteger waiting,
                          final ConcurrentHashMap<String, Object> queue,
                          final ConcurrentHashMap<String, Object> visited,
                          final ConcurrentHashMap<String, IOException> errors,
                          final ConcurrentHashMap<String, Semaphore> hostAvailable,
                          Pair<String, Integer> current) {
        try {
            String url = current.getKey();
            Integer depth = current.getValue();
//            System.err.printf("Downloading: %s\n", url);
            String host = URLUtils.getHost(url);
            hostAvailable.putIfAbsent(host, new Semaphore(perHost));
            if (hostAvailable.get(host).tryAcquire()) {
                try {
                    Document document = downloader.download(url);
                    visited.put(url, DEFAULT_OBJECT);
                    waiting.incrementAndGet();
                    extractService.submit(() -> extract(document, url, depth, waiting, queue, visited, errors, hostAvailable));
                } catch (IOException ignored) {
                    errors.put(url, ignored);
                } finally {
                    hostAvailable.get(host).release();
                }
            } else {
                waiting.incrementAndGet();
                downloadService.submit(() -> download(waiting, queue, visited, errors, hostAvailable, current));
            }
        } catch (MalformedURLException ignored) {
        } finally {
            waiting.decrementAndGet();
        }
    }

    private void extract(Document document, String urll, int depth,
                         final AtomicInteger waiting,
                         final ConcurrentHashMap<String, Object> queue,
                         final ConcurrentHashMap<String, Object> visited,
                         final ConcurrentHashMap<String, IOException> errors,
                         final ConcurrentHashMap<String, Semaphore> hostAvailable) {
        try {
            if (depth > 1) {
                List<String> urls = document.extractLinks();
                urls.parallelStream().distinct().filter(url -> queue.putIfAbsent(url, DEFAULT_OBJECT) == null).forEach(url -> {
                        waiting.incrementAndGet();
                        downloadService.submit(() -> download(waiting, queue, visited, errors, hostAvailable, new Pair<>(url, depth - 1)));
                });
            }
        } catch (IOException e) {
            errors.put(urll, e);
        } finally {
            waiting.decrementAndGet();
        }
    }

    @Override
    public void close() {
        downloadService.shutdownNow();
        extractService.shutdownNow();
    }
}
