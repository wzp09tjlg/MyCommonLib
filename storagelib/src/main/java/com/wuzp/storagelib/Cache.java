package com.wuzp.storagelib;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Map;

public interface Cache {

    /**
     * Returns a boolean indicating whether an entry with the specified key can be found in the cache
     *
     * @param key Cache key
     * @return
     */
    boolean has(final String key);

    /**
     * Retrieves an entry from the cache.
     *
     * @param key Cache key
     * @return The value or null in the event of a cache miss
     */
    <T> T get(final String key);

    /**
     * Adds or replaces an entry to the cache.
     *
     * @param key   Cache key
     * @param value Data to store and metadata for cache coherency, TTL, etc.
     * @param ttl   TTL for this record.
     */
    boolean put(final String key, final Object value, long ttl);

    /**
     * Performs any potentially long-running actions needed to initialize the
     * cache; will be called from a worker thread.
     */
    void initialize();

    /**
     * Invalidates an entry in the cache.
     *
     * @param key        Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    void invalidate(final String key, final boolean fullExpire);

    /**
     * Removes an entry from the cache.
     *
     * @param key Cache key
     */
    void remove(final String key);

    /**
     * Empties the cache.
     */
    void clear();

    /**
     * Data and metadata for an entry returned by the cache.
     */
    class Entry implements Parcelable {

        public static final Creator<Entry> CREATOR = new Creator<Entry>() {
            @Override
            public Entry createFromParcel(Parcel in) {
                return new Entry(in);
            }

            @Override
            public Entry[] newArray(int size) {
                return new Entry[size];
            }
        };

        /**
         * The data returned from cache.
         */
        public byte[] data;

        /**
         * ETag for cache coherency.
         */
        public String etag;

        /**
         * Date of this response as reported by the server.
         */
        public long serverDate;

        /**
         * The last modified date for the requested object.
         */
        public long lastModified;

        /**
         * TTL for this record.
         */
        public long ttl;

        /**
         * Soft TTL for this record.
         */
        public long softTtl;

        /**
         * Immutable response headers as received from server; must be non-null.
         */
        public Map<String, String> responseHeaders = Collections.emptyMap();

        public Entry() {
        }

        protected Entry(Parcel in) {
            this.data = in.createByteArray();
            this.etag = in.readString();
            this.serverDate = in.readLong();
            this.lastModified = in.readLong();
            this.ttl = in.readLong();
            this.softTtl = in.readLong();
        }

        /**
         * True if the entry is expired.
         */
        public boolean isExpired() {
            return this.ttl < System.currentTimeMillis();
        }

        /**
         * True if a refresh is needed from the original data source.
         */
        public boolean refreshNeeded() {
            return this.softTtl < System.currentTimeMillis();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeByteArray(data);
            dest.writeString(etag);
            dest.writeLong(serverDate);
            dest.writeLong(lastModified);
            dest.writeLong(ttl);
            dest.writeLong(softTtl);
        }
    }

}
