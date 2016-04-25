package ru.ifmo.ctddev.sabirzyanov.arrayset;

import java.util.*;

/**
 * Created by Ilnar Sabirzyanov on 01.03.2016.
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comp;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends T> col) {
        this(col, null);
    }

    public ArraySet(Collection<? extends T> col, Comparator<? super T> comp) {
        this.data = new ArrayList<>();
        this.comp = comp;
        TreeSet<T> set = new TreeSet<>(comp);
        set.addAll(col);
        data.addAll(set);
    }

    private ArraySet(List<T> data, Comparator<? super T> comp) {
        this.data = data;
        this.comp = comp;
    }

    /**
     * Returns the greatest element in this set strictly less than the
     * given element, or {@code null} if there is no such element.
     *
     * @param t the value to match
     * @return the greatest element less than {@code e},
     * or {@code null} if there is no such element
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with the elements currently in the set
     * @throws NullPointerException if the specified element is null
     *                              and this set does not permit null elements
     */
    @Override
    public T lower(T t) {
        int res = Collections.binarySearch(data, t, comp);
        if (res < 0) {
            res = -res - 1;
        }
        return (0 < res && res <= size()) ? data.get(res - 1) : null;
    }

    /**
     * Returns the greatest element in this set less than or equal to
     * the given element, or {@code null} if there is no such element.
     *
     * @param t the value to match
     * @return the greatest element less than or equal to {@code e},
     * or {@code null} if there is no such element
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with the elements currently in the set
     * @throws NullPointerException if the specified element is null
     *                              and this set does not permit null elements
     */
    @Override
    public T floor(T t) {
        int res = Collections.binarySearch(data, t, comp);
        if (0 <= res && res < size()) {
            return data.get(res);
        }
        res = -(res + 1);
        return (0 < res && res <= size()) ? data.get(res - 1) : null;
    }

    /**
     * Returns the least element in this set greater than or equal to
     * the given element, or {@code null} if there is no such element.
     *
     * @param t the value to match
     * @return the least element greater than or equal to {@code e},
     * or {@code null} if there is no such element
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with the elements currently in the set
     * @throws NullPointerException if the specified element is null
     *                              and this set does not permit null elements
     */
    @Override
    public T ceiling(T t) {
        int res = Collections.binarySearch(data, t, comp);
        if (res < 0) {
            res = -(res + 1);
        }
        return (0 <= res && res < size()) ? data.get(res) : null;
    }

    /**
     * Returns the least element in this set strictly greater than the
     * given element, or {@code null} if there is no such element.
     *
     * @param t the value to match
     * @return the least element greater than {@code e},
     * or {@code null} if there is no such element
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with the elements currently in the set
     * @throws NullPointerException if the specified element is null
     *                              and this set does not permit null elements
     */
    @Override
    public T higher(T t) {
        int res = Collections.binarySearch(data, t, comp);
        if (0 <= res && res < size()) {
            res++;
        }
        if (res < 0) {
            res = -res - 1;
        }
        return (0 <= res && res < size()) ? data.get(res) : null;
    }

    /**
     * Retrieves and removes the first (lowest) element,
     * or returns {@code null} if this set is empty.
     *
     * @return the first element, or {@code null} if this set is empty
     */
    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst");
    }

    /**
     * Retrieves and removes the last (highest) element,
     * or returns {@code null} if this set is empty.
     *
     * @return the last element, or {@code null} if this set is empty
     */
    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast");
    }

    /**
     * Returns an iterator over the elements in this set, in ascending order.
     *
     * @return an iterator over the elements in this set, in ascending order
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int pos = 0;
            @Override
            public boolean hasNext() {
                return pos < size();
            }

            @Override
            public T next() {
                return data.get(pos++);
            }
        };
    }

    /**
     * Returns a reverse order view of the elements contained in this set.
     * The descending set is backed by this set, so changes to the set are
     * reflected in the descending set, and vice-versa.  If either set is
     * modified while an iteration over either set is in progress (except
     * through the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.
     * <p>
     * <p>The returned set has an ordering equivalent to
     * <tt>{@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator())</tt>.
     * The expression {@code s.descendingSet().descendingSet()} returns a
     * view of {@code s} essentially equivalent to {@code s}.
     *
     * @return a reverse order view of this set
     */
    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data, true), comp);
    }

    /**
     * Returns an iterator over the elements in this set, in descending order.
     * Equivalent in effect to {@code descendingSet().iterator()}.
     *
     * @return an iterator over the elements in this set, in descending order
     */
    @Override
    public Iterator<T> descendingIterator() {
        return new Iterator<T>() {
            private int pos = data.size();
            @Override
            public boolean hasNext() {
                return pos > 0;
            }

            @Override
            public T next() {
                return data.get(--pos);
            }
        };
    }

    /**
     * Returns a view of the portion of this set whose elements range from
     * {@code fromElement} to {@code toElement}.  If {@code fromElement} and
     * {@code toElement} are equal, the returned set is empty unless {@code
     * fromInclusive} and {@code toInclusive} are both true.  The returned set
     * is backed by this set, so changes in the returned set are reflected in
     * this set, and vice-versa.  The returned set supports all optional set
     * operations that this set supports.
     * <p>
     * <p>The returned set will throw an {@code IllegalArgumentException}
     * on an attempt to insert an element outside its range.
     *
     * @param fromElement   low endpoint of the returned set
     * @param fromInclusive {@code true} if the low endpoint
     *                      is to be included in the returned view
     * @param toElement     high endpoint of the returned set
     * @param toInclusive   {@code true} if the high endpoint
     *                      is to be included in the returned view
     * @return a view of the portion of this set whose elements range from
     * {@code fromElement}, inclusive, to {@code toElement}, exclusive
     * @throws ClassCastException       if {@code fromElement} and
     *                                  {@code toElement} cannot be compared to one another using this
     *                                  set's comparator (or, if the set has no comparator, using
     *                                  natural ordering).  Implementations may, but are not required
     *                                  to, throw this exception if {@code fromElement} or
     *                                  {@code toElement} cannot be compared to elements currently in
     *                                  the set.
     * @throws NullPointerException     if {@code fromElement} or
     *                                  {@code toElement} is null and this set does
     *                                  not permit null elements
     * @throws IllegalArgumentException if {@code fromElement} is
     *                                  greater than {@code toElement}; or if this set itself
     *                                  has a restricted range, and {@code fromElement} or
     *                                  {@code toElement} lies outside the bounds of the range.
     */
    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (fromElement == null || toElement == null) {
            throw new NullPointerException();
        }
        int fromPos = Collections.binarySearch(data, fromElement, comp);
        int toPos = Collections.binarySearch(data, toElement, comp);
        if (!fromInclusive && 0 <= fromPos && fromPos < data.size()) {
            fromPos++;
        }
        if (!toInclusive && 0 <= toPos && toPos < data.size()) {
            toPos--;
        }
        fromPos = (fromPos < 0) ? -fromPos - 1 : fromPos;
        toPos = (toPos < 0) ? -toPos - 2 : toPos;
        return new ArraySet<>(data.subList(fromPos, Math.max(toPos + 1, fromPos)), comp);
    }

    /**
     * Returns a view of the portion of this set whose elements are less than
     * (or equal to, if {@code inclusive} is true) {@code toElement}.  The
     * returned set is backed by this set, so changes in the returned set are
     * reflected in this set, and vice-versa.  The returned set supports all
     * optional set operations that this set supports.
     * <p>
     * <p>The returned set will throw an {@code IllegalArgumentException}
     * on an attempt to insert an element outside its range.
     *
     * @param toElement high endpoint of the returned set
     * @param inclusive {@code true} if the high endpoint
     *                  is to be included in the returned view
     * @return a view of the portion of this set whose elements are less than
     * (or equal to, if {@code inclusive} is true) {@code toElement}
     * @throws ClassCastException       if {@code toElement} is not compatible
     *                                  with this set's comparator (or, if the set has no comparator,
     *                                  if {@code toElement} does not implement {@link Comparable}).
     *                                  Implementations may, but are not required to, throw this
     *                                  exception if {@code toElement} cannot be compared to elements
     *                                  currently in the set.
     * @throws NullPointerException     if {@code toElement} is null and
     *                                  this set does not permit null elements
     * @throws IllegalArgumentException if this set itself has a
     *                                  restricted range, and {@code toElement} lies outside the
     *                                  bounds of the range
     */
    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(first(), true, toElement, inclusive);
    }

    /**
     * Returns a view of the portion of this set whose elements are greater
     * than (or equal to, if {@code inclusive} is true) {@code fromElement}.
     * The returned set is backed by this set, so changes in the returned set
     * are reflected in this set, and vice-versa.  The returned set supports
     * all optional set operations that this set supports.
     * <p>
     * <p>The returned set will throw an {@code IllegalArgumentException}
     * on an attempt to insert an element outside its range.
     *
     * @param fromElement low endpoint of the returned set
     * @param inclusive   {@code true} if the low endpoint
     *                    is to be included in the returned view
     * @return a view of the portion of this set whose elements are greater
     * than or equal to {@code fromElement}
     * @throws ClassCastException       if {@code fromElement} is not compatible
     *                                  with this set's comparator (or, if the set has no comparator,
     *                                  if {@code fromElement} does not implement {@link Comparable}).
     *                                  Implementations may, but are not required to, throw this
     *                                  exception if {@code fromElement} cannot be compared to elements
     *                                  currently in the set.
     * @throws NullPointerException     if {@code fromElement} is null
     *                                  and this set does not permit null elements
     * @throws IllegalArgumentException if this set itself has a
     *                                  restricted range, and {@code fromElement} lies outside the
     *                                  bounds of the range
     */
    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>Equivalent to {@code subSet(fromElement, true, toElement, false)}.
     *
     * @param fromElement
     * @param toElement
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>Equivalent to {@code headSet(toElement, false)}.
     *
     * @param toElement
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>Equivalent to {@code tailSet(fromElement, true)}.
     *
     * @param fromElement
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    /**
     * Returns the comparator used to order the elements in this set,
     * or <tt>null</tt> if this set uses the {@linkplain Comparable
     * natural ordering} of its elements.
     *
     * @return the comparator used to order the elements in this set,
     * or <tt>null</tt> if this set uses the natural ordering
     * of its elements
     */
    @Override
    public Comparator<? super T> comparator() {
        return comp;
    }

    /**
     * Returns the first (lowest) element currently in this set.
     *
     * @return the first (lowest) element currently in this set
     * @throws NoSuchElementException if this set is empty
     */
    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    /**
     * Returns the last (highest) element currently in this set.
     *
     * @return the last (highest) element currently in this set
     * @throws NoSuchElementException if this set is empty
     */
    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(size() - 1);
    }

    /**
     * Returns the number of elements in this set (its cardinality).  If this
     * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this set (its cardinality)
     */
    @Override
    public int size() {
        return data.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this set
     * contains an element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this set is to be tested
     * @return <tt>true</tt> if this set contains the specified element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this set
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              set does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) o, comp) >= 0;
    }

    class ReversedList<E> extends AbstractList<E> implements RandomAccess {
        List<E> data;
        boolean reversed;

        public ReversedList(List<E> data, boolean reversed) {
            this.data = data;
            this.reversed = reversed;
        }

        /**
         * {@inheritDoc}
         *
         * @param index
         * @throws IndexOutOfBoundsException {@inheritDoc}
         */
        @Override
        public E get(int index) {
            return reversed ? data.get(size() - index - 1) : data.get(index);
        }

        @Override
        public int size() {
            return data.size();
        }
    }
}