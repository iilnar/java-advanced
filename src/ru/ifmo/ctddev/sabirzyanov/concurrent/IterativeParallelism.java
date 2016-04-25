package ru.ifmo.ctddev.sabirzyanov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class implements {@link ListIP}.
 *
 * @author ilnar
 * @see info.kgeorgiy.java.advanced.concurrent.ListIP
 * @see info.kgeorgiy.java.advanced.concurrent.ScalarIP
 * Created by ilnar on 21.03.16.
 */
public class IterativeParallelism implements ListIP {
    private class MyRunnable<T, R> implements Runnable {
        private List<? extends T> data;
        private Function<List<? extends T>, R> function;
        private R result;

        public MyRunnable(List<? extends T> data, Function<List<? extends T>, R> function) {
            this.data = data;
            this.function = function;
        }

        @Override
        public void run() {
            result = function.apply(data);
        }

        public R getResult() {
            return result;
        }
    }

    private ParallelMapper mapper;

    public IterativeParallelism() {}

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<List <? extends T>> split(int n, List<? extends T> data) {
        List<List<? extends T>> res = new ArrayList<>();
        int k = Math.max(1, (data.size() + n - 1) / n);
        for (int i = 0; i * k < data.size(); i++) {
            res.add(data.subList(i * k, Math.min((i + 1) * k, data.size())));
        }
        return res;
    }

    private <T, R> List<R> apply(int n, List<? extends T> data, Function<List<? extends T>, R> function) throws InterruptedException {
        if (mapper != null) {
            return mapper.map(function, split(n, data));
        }
        List<MyRunnable<T, R>> run = new ArrayList<>();
        int k = Math.max(1, (data.size() + n - 1) / n);
        for (int i = 0; i * k < data.size(); i++) {
            run.add(new MyRunnable<>(data.subList(i * k, Math.min((i + 1) * k, data.size())), function));
        }
        List<Thread> threads = run.stream().map(Thread::new).collect(Collectors.toList());
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        return run.stream().map(MyRunnable::getResult).collect(Collectors.toList());
    }

    /**
     * Finds the first maximum element in {@code values}.
     *
     * Finds first maximum in {@code values} via {@link Thread}.
     * @param threads Maximum number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param comparator {@link java.util.Comparator}.
     * @return Maximal element in list.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function = data -> data.stream().max(comparator).get();
        return function.apply(apply(threads, values, function));
    }

    /**
     * Finds the first minimum element in {@code values}.
     *
     * Finds first minimum in {@code values} via {@link Thread}.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param comparator {@link java.util.Comparator}.
     * @return Minimal element in list.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function = data -> data.stream().min(comparator).get();
        return function.apply(apply(threads, values, function));
    }

    /**
     * Returns whether all elements of {@code values} match the provided {@code predicate}.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param predicate {@link Predicate} to match
     * @return {@code True} if all elements of {@code values} match the {@code predicate}, {@code False} otherwise.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return apply(threads, values, data -> data.stream().allMatch(predicate)).stream().allMatch(Predicate.isEqual(true));
    }

    /**
     * Returns whether any element of {@code values} match the provided {@code predicate}.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param predicate {@link Predicate} to match
     * @return {@code True} if any element of {@code values} match the {@code predicate}, {@code False} otherwise.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return apply(threads, values, data -> data.stream().anyMatch(predicate)).stream().anyMatch(Predicate.isEqual(true));
    }

    /**
     * Concatenates elements of {@code values} as {@link String} representations.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values
     * @return Concatenation of {@link String} representations of {@code values} elements.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        StringBuilder res = new StringBuilder();
        apply(threads, values, data -> {
            StringBuilder result = new StringBuilder();
            data.stream().map(Object::toString).forEach(result::append);
            return result.toString();
        }).forEach(res::append);
        return res.toString();
    }

    /**
     * Filters the {@code values} with {@code predicate}.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param predicate {@link Predicate} to filter the {@code values}.
     * @return {@link java.util.List} of elements that match the {@code predicate}.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<T> res = new ArrayList<>();
        apply(threads, values, data -> {
            List<T> result = new ArrayList<>();
            data.stream().filter(predicate).forEach(result::add);
            return result;
        }).forEach(res::addAll);
        return res;
    }

    /**
     * Returns a {@link java.util.List} consisting of the results of applying the given function to the elements of {@code values}.
     * @param threads Maximal number of allowed threads.
     * @param values {@link java.util.List} of values.
     * @param f {@link Function} to apply.
     * @return {@link java.util.List} of elements that are results of {@code f}.
     * @throws InterruptedException if thread was interrupted
     * @see InterruptedException
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        List<U> res = new ArrayList<>();
        apply(threads, values, data -> {
            List<U> result = new ArrayList<>();
            data.stream().map(f).forEach(result::add);
            return result;
        }).forEach(res::addAll);
        return res;
    }
}
