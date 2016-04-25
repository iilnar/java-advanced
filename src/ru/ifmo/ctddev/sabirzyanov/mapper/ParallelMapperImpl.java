package ru.ifmo.ctddev.sabirzyanov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ilnar on 11.04.16.
 */
public class ParallelMapperImpl implements ParallelMapper {
    private enum Status {
        INITIALIZED, RUNNING, FINISHED
    }

    private class Task<T, R> {
        private T arguments;
        private R result;
        private Status status;
        private Function<? super T, ? extends R> function;

        Task(Function<? super T, ? extends R> function, T arguments) {
            this.function = function;
            this.arguments = arguments;
            status = Status.INITIALIZED;
        }

        public synchronized void execute() {
            status = Status.RUNNING;
            result = function.apply(arguments);
            status = Status.FINISHED;
            notifyAll();
        }

        public synchronized R getResult() {
            while (status != Status.FINISHED) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
            return result;
        }
    }

    private final Queue<Task> taskQueue;
    private final Thread[] threads;

    /**
     * Makes {@code threadsCount} threads.
     * @param threadsCount count of threads to make
     */
    public ParallelMapperImpl(int threadsCount) {
        taskQueue = new ArrayDeque<>();
        threads = new Thread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Task task;
                            while (!Thread.currentThread().isInterrupted()) {
                                synchronized (taskQueue) {
                                    while (taskQueue.isEmpty()) {
                                        try {
                                            taskQueue.wait();
                                        } catch (InterruptedException e) {
                                            return;
                                        }
                                    }
                                    task = taskQueue.poll();
                                }
                                task.execute();
                                //Thread.currentThread().interrupt();
                            }
                        }
                    }
            );
            threads[i].start();
        }
    }

    /**
     * Applies {@code f} function to all {@code args}.
     * @param f function
     * @param args list of elements
     * @return list of results
     * @throws InterruptedException if interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<Task<T, R>> tasks = args.stream().map(arg -> new Task<>(f, arg)).collect(Collectors.toList());
        synchronized (taskQueue) {
            taskQueue.addAll(tasks);
            taskQueue.notifyAll();
        }
        List<R> res = new ArrayList<>(args.size());
        tasks.forEach(task -> res.add(task.getResult()));
        return tasks.stream().map(Task::getResult).collect(Collectors.toList());
    }

    /**
     * Closes all threads
     * @throws InterruptedException if interrupted
     */
    @Override
    public void close() throws InterruptedException {
        Arrays.stream(threads).forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
