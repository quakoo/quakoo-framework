// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package com.quakoo.baseFramework.redis.transcoders;
import com.quakoo.baseFramework.redis.exception.JedisXException;

/**
 * Transcoder is an interface for classes that convert between byte arrays and
 * objects for storage in the cache.
 */
public interface Transcoder<T> {

    /**
     * Encode the given object for storage.
     *
     * @param o the object
     * @return the CachedData representing what should be sent
     */
    CachedData encode(T o);

    /**
     * Decode the cached object into the object it represents.
     *
     * @param d the data
     * @return the return value
     */
    T decode(CachedData d) throws JedisXException;

    /**
     * Set whether store primitive type as string.
     *
     * @param primitiveAsString
     */
    public void setPrimitiveAsString(boolean primitiveAsString);

    /**
     * Set whether pack zeros
     *
     * @param primitiveAsString
     */
    public void setPackZeros(boolean packZeros);

    /**
     * Set compression threshold in bytes
     *
     * @param to
     */
    public void setCompressionThreshold(int to);

    /**
     * Returns if client stores primitive type as string.
     *
     * @return
     */
    public boolean isPrimitiveAsString();

    /**
     * Returns if transcoder packs zero.
     *
     * @return
     */
    public boolean isPackZeros();

    /**
     * Set compress mode,default is ZIP
     *
     * @param compressMode
     * @see CompressionMode
     */
    public void setCompressionMode(CompressionMode compressMode);
}
