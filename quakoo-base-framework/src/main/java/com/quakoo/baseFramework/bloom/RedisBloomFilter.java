package com.quakoo.baseFramework.bloom;

import com.quakoo.baseFramework.redis.JedisX;

import java.io.Serializable;
import java.util.Collection;

/**
 * redis bit版本 布隆过滤
 * class_name: RedisBloomFilter
 * package: com.quakoo.baseFramework.bloom
 * creat_user: lihao
 * creat_date: 2019/12/26
 * creat_time: 16:05
 **/
public class RedisBloomFilter<E> implements Serializable {

    private JedisX cache;

    private int bitSetSize;
    private double bitsPerElement;
    private int expectedNumberOfFilterElements; // expected (maximum) number of elements to be added
    private int numberOfAddedElements; // number of elements actually added to the Bloom filter
    private int k; // number of hash functions


    /**
     * Constructs an empty Bloom filter. The total length of the Bloom filter will be
     * c*n.
     *
     * @param c is the number of bits used per element.
     * @param n is the expected number of elements the filter will contain.
     * @param k is the number of hash functions used.
     */
    private RedisBloomFilter(JedisX cache, double c, int n, int k) {
        if(null == cache) throw new IllegalArgumentException("RedisBloomFilter construction error!");
        this.cache = cache;
        this.expectedNumberOfFilterElements = n;
        this.k = k;
        this.bitsPerElement = c;
        this.bitSetSize = (int) Math.ceil(c * n);
        numberOfAddedElements = 0;
    }


    /**
     * Constructs an empty Bloom filter with a given false positive probability. The number of bits per
     * element and the number of hash functions is estimated
     * to match the false positive probability.
     *
     * @param falsePositiveProbability is the desired false positive probability.
     * @param expectedNumberOfElements is the expected number of elements in the Bloom filter.
     */
    public RedisBloomFilter(JedisX cache, double falsePositiveProbability, int expectedNumberOfElements) {
        this(cache, Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2.0))) / Math.log(2.0), // c = k / ln(2)
                expectedNumberOfElements,
                (int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2.0)))); // k = ceil(-log_2(false prob.))
    }

    /**
     * Compares the contents of two instances to see if they are equal.
     *
     * @param obj is the object to compare to.
     * @return True if the contents of the objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RedisBloomFilter<E> other = (RedisBloomFilter<E>) obj;
        if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements) {
            return false;
        }
        if (this.k != other.k) {
            return false;
        }
        if (this.bitSetSize != other.bitSetSize) {
            return false;
        }
        if (this.cache != other.cache) {
            return false;
        }
        return true;
    }

    /**
     * Calculates a hash code for this class.
     *
     * @return hash code representing the contents of an instance of this class.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.cache.hashCode();
        hash = 61 * hash + this.expectedNumberOfFilterElements;
        hash = 61 * hash + this.bitSetSize;
        hash = 61 * hash + this.k;
        return hash;
    }


    /**
     * Calculates the expected probability of false positives based on
     * the number of expected filter elements and the size of the Bloom filter.
     *
     * The value returned by this method is the <i>expected</i> rate of false
     * positives, assuming the number of inserted elements equals the number of
     * expected elements. If the number of elements in the Bloom filter is less
     * than the expected value, the true probability of false positives will be lower.
     *
     *
     * @return expected probability of false positives.
     */
    public double expectedFalsePositiveProbability() {
        return getFalsePositiveProbability(expectedNumberOfFilterElements);
    }

    /**
     * Calculate the probability of a false positive given the specified
     * number of inserted elements.
     *
     * @param numberOfElements number of inserted elements.
     * @return probability of a false positive.
     */
    public double getFalsePositiveProbability(double numberOfElements) {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-k * (double) numberOfElements
                / (double) bitSetSize)), k);

    }

    /**
     * Get the current probability of a false positive. The probability is calculated from
     * the size of the Bloom filter and the current number of elements added to it.
     *
     * @return probability of false positives.
     */
    public double getFalsePositiveProbability() {
        return getFalsePositiveProbability(numberOfAddedElements);
    }


    /**
     * Returns the value chosen for K.
     *
     * K is the optimal number of hash functions based on the size
     * of the Bloom filter and the expected number of inserted elements.
     *
     * @return optimal k.
     */
    public int getK() {
        return k;
    }

    /**
     * Sets all bits to false in the Bloom filter.
     */
    public void clear(String key) {
        cache.delete(key);
        numberOfAddedElements = 0;
    }

    /**
     * Adds an object to the Bloom filter. The output from the object's
     * toString() method is used as input to the hash functions.
     *
     * @param element is an element to register in the Bloom filter.
     */
    public void add(String key, int timeout, E element) {
        add(key, timeout, element.toString().getBytes(MessageDigestUtils.CHARSET));
    }

    /**
     * Adds an array of bytes to the Bloom filter.
     *
     * @param bytes array of bytes to add to the Bloom filter.
     */
    public void add(String key, int timeout, byte[] bytes) {
        add(key, bytes);
        if(timeout > 0) cache.expire(key, timeout);
    }

    private void add(String key, byte[] bytes) {
        int[] hashes = MessageDigestUtils.createHashes(bytes, k);
        for (int hash : hashes) {
            cache.setbit(key, Math.abs(hash % bitSetSize), true);
        }
        numberOfAddedElements++;
    }

    /**
     * Adds all elements from a Collection to the Bloom filter.
     *
     * @param c Collection of elements.
     */
    public void addAll(String key, int timeout, Collection<? extends E> c) {
        for (E element : c)
            add(key, timeout, element);
        if(timeout > 0) cache.expire(key, timeout);
    }

    /**
     * Returns true if the element could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param element element to check.
     * @return true if the element could have been inserted into the Bloom filter.
     */
    public boolean contains(String key, E element) {
        return contains(key, element.toString().getBytes(MessageDigestUtils.CHARSET));
    }

    /**
     * Returns true if the array of bytes could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param bytes array of bytes to check.
     * @return true if the array could have been inserted into the Bloom filter.
     */
    public boolean contains(String key, byte[] bytes) {
        int[] hashes = MessageDigestUtils.createHashes(bytes, k);
        for (int hash : hashes) {
            boolean sign = cache.getbit(key, Math.abs(hash % bitSetSize));
            if (!sign) return false;
        }
        return true;
    }

    /**
     * Returns true if all the elements of a Collection could have been inserted
     * into the Bloom filter. Use getFalsePositiveProbability() to calculate the
     * probability of this being correct.
     *
     * @param c elements to check.
     * @return true if all the elements in c could have been inserted into the Bloom filter.
     */
    public boolean containsAll(String key, Collection<? extends E> c) {
        for (E element : c)
            if (!contains(key, element))
                return false;
        return true;
    }

    /**
     * Read a single bit from the Bloom filter.
     *
     * @param bit the bit to read.
     * @return true if the bit is set, false if it is not.
     */
    public boolean getBit(String key, int bit) {
        boolean sign = cache.getbit(key, bit);
        return sign;
    }

    /**
     * Set a single bit in the Bloom filter.
     *
     * @param bit   is the bit to set.
     * @param value If true, the bit is set. If false, the bit is cleared.
     */
    public void setBit(String key, int bit, boolean value) {
        cache.setbit(key, bit, value);
    }


    /**
     * Returns the number of bits in the Bloom filter. Use count() to retrieve
     * the number of inserted elements.
     *
     * @return the size of the bitSet used by the Bloom filter.
     */
    public int size() {
        return this.bitSetSize;
    }

    /**
     * Returns the number of elements added to the Bloom filter after it
     * was constructed or after clear() was called.
     *
     * @return number of elements added to the Bloom filter.
     */
    public int count() {
        return this.numberOfAddedElements;
    }

    /**
     * Returns is the bit set empty, bit set is empty means no any elements added to bloom filter.
     *
     * @return is the bit set empty
     */
    public boolean isEmpty() {
        return count() <= 0;
    }

    /**
     * Returns the expected number of elements to be inserted into the filter.
     * This value is the same value as the one passed to the constructor.
     *
     * @return expected number of elements.
     */
    public int getExpectedNumberOfElements() {
        return expectedNumberOfFilterElements;
    }

    /**
     * Get expected number of bits per element when the Bloom filter is full. This value is set by the constructor
     * when the Bloom filter is created. See also getBitsPerElement().
     *
     * @return expected number of bits per element.
     */
    public double getExpectedBitsPerElement() {
        return this.bitsPerElement;
    }

    /**
     * Get actual number of bits per element based on the number of elements that have currently been inserted and the length
     * of the Bloom filter. See also getExpectedBitsPerElement().
     *
     * @return number of bits per element.
     */
    public double getBitsPerElement() {
        return this.bitSetSize / (double) numberOfAddedElements;
    }

}
