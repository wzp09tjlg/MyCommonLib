package com.wuzp.storagelib;

import android.os.Parcel;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache implementation that caches files directly onto the hard disk in the specified
 * directory. The default disk usage size is 5MB, but is configurable.
 *
 */
class DiskCache implements Cache {

    /**
     * Default maximum disk usage in bytes.
     */
    protected static final int DEFAULT_DISK_USAGE_BYTES = 5 * 1024 * 1024;
    /**
     * Log tag
     */
    private static final String TAG = "DiskCache";
    /**
     * High water mark percentage for the cache
     */
    private static final float HYSTERESIS_FACTOR = 0.9f;

    /**
     * Magic number for current version of cache file format.
     */
    private static final int CACHE_MAGIC = 0x20160306;

    /**
     * Map of the Key, CacheHeader pairs
     */
    private final Map<String, CacheHeader> mEntries = new LinkedHashMap<String, CacheHeader>(16, .75f, true);

    /**
     * The root directory to use for the cache.
     */
    private final File mRootDirectory;
    /**
     * The maximum size of the cache in bytes.
     */
    private final long mMaxCacheSizeInBytes;
    /**
     * Total amount of space currently used by the cache in bytes.
     */
    private long mTotalSize = 0;

    /**
     * Constructs an instance of the DiskCache at the specified directory.
     *
     * @param rootDirectory       The root directory of the cache.
     * @param maxCacheSizeInBytes The maximum size of the cache in bytes.
     */
    DiskCache(final File rootDirectory, final long maxCacheSizeInBytes) {
        this.mRootDirectory = rootDirectory;
        this.mMaxCacheSizeInBytes = maxCacheSizeInBytes;
    }

    /**
     * Constructs an instance of the DiskCache at the specified directory using
     * the default maximum cache size of 5MB.
     *
     * @param rootDirectory The root directory of the cache.
     */
    DiskCache(final File rootDirectory) {
        this(rootDirectory, DEFAULT_DISK_USAGE_BYTES);
    }

    /**
     * Reads the contents of an InputStream into a byte[].
     */
    private static byte[] streamToBytes(final InputStream in, final int length) throws IOException {
        final byte[] bytes = new byte[length];
        int count;
        int pos = 0;

        while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
            pos += count;
        }

        if (pos != length) {
            throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
        }

        return bytes;
    }

    /**
     * Simple wrapper around {@link InputStream#read()} that throws EOFException
     * instead of returning -1.
     */
    private static int read(final InputStream is) throws IOException {
        final int b = is.read();
        if (b == -1) {
            throw new EOFException();
        }

        return b;
    }

    static void writeInt(final OutputStream os, final int n) throws IOException {
        os.write((n >> 0) & 0xff);
        os.write((n >> 8) & 0xff);
        os.write((n >> 16) & 0xff);
        os.write((n >> 24) & 0xff);
    }

    static int readInt(final InputStream is) throws IOException {
        int n = 0;
        n |= (read(is) << 0);
        n |= (read(is) << 8);
        n |= (read(is) << 16);
        n |= (read(is) << 24);
        return n;
    }

    static void writeLong(final OutputStream os, final long n) throws IOException {
        os.write((byte) (n >>> 0));
        os.write((byte) (n >>> 8));
        os.write((byte) (n >>> 16));
        os.write((byte) (n >>> 24));
        os.write((byte) (n >>> 32));
        os.write((byte) (n >>> 40));
        os.write((byte) (n >>> 48));
        os.write((byte) (n >>> 56));
    }

    static long readLong(final InputStream is) throws IOException {
        long n = 0;
        n |= ((read(is) & 0xFFL) << 0);
        n |= ((read(is) & 0xFFL) << 8);
        n |= ((read(is) & 0xFFL) << 16);
        n |= ((read(is) & 0xFFL) << 24);
        n |= ((read(is) & 0xFFL) << 32);
        n |= ((read(is) & 0xFFL) << 40);
        n |= ((read(is) & 0xFFL) << 48);
        n |= ((read(is) & 0xFFL) << 56);
        return n;
    }

    static void writeString(final OutputStream os, final String s) throws IOException {
        final byte[] b = s.getBytes("UTF-8");
        writeLong(os, b.length);
        os.write(b, 0, b.length);
    }

    static String readString(final InputStream is) throws IOException {
        final int n = (int) readLong(is);
        final byte[] b = streamToBytes(is, n);
        return new String(b, "UTF-8");
    }

    static void writeStringStringMap(final Map<String, String> map, final OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, entry.getKey());
                writeString(os, entry.getValue());
            }
        } else {
            writeInt(os, 0);
        }
    }

    static Map<String, String> readStringStringMap(final InputStream is) throws IOException {
        final int size = readInt(is);
        final Map<String, String> result = (size == 0) ? Collections.<String, String>emptyMap() : new HashMap<String, String>(size);

        for (int i = 0; i < size; i++) {
            final String key = readString(is).intern();
            final String value = readString(is).intern();
            result.put(key, value);
        }

        return result;
    }

    /**
     * Clears the cache. Deletes all cached files from disk.
     */
    @Override
    public synchronized void clear() {
        final File[] files = this.mRootDirectory.listFiles();
        if (files != null) {
            for (final File file : files) {
                file.delete();
            }
        }

        this.mEntries.clear();
        this.mTotalSize = 0;
        Log.d(TAG, "Cache cleared.");
    }

    @Override
    public boolean has(final String key) {
        return this.mEntries.containsKey(key);
    }

    @Override
    public synchronized <T> T get(final String key) {
        final Entry entry = getEntry(key);
        if (null == entry || entry.data == null || entry.isExpired()) {
            return null;
        }

        long start = System.currentTimeMillis();
        final Parcel parcel = Parcel.obtain();

        try {
            parcel.unmarshall(entry.data, 0, entry.data.length);
            parcel.setDataPosition(0);
            return (T) parcel.readValue(getClass().getClassLoader());
        } finally {
            parcel.recycle();
            long end = System.currentTimeMillis();
            Log.d("" + this.getClass().getName(), "finish time readValue is " + (end - start) + (parcel.readValue(getClass().getClassLoader())));
        }

    }

    /**
     * Initializes the DiskCache by scanning for all files currently in the
     * specified root directory. Creates the root directory if necessary.
     */
    @Override
    public synchronized void initialize() {
        if (!this.mRootDirectory.exists()) {
            if (!this.mRootDirectory.mkdirs()) {
                Log.e(TAG, String.format("Unable to create cache dir %s", this.mRootDirectory.getAbsolutePath()));
            }
            return;
        }

        final File[] files = this.mRootDirectory.listFiles();
        if (files == null) {
            return;
        }

        BufferedInputStream fis = null;

        for (final File file : files) {
            try {
                fis = new BufferedInputStream(new FileInputStream(file));
                final CacheHeader entry = CacheHeader.readHeader(fis);
                entry.mSize = file.length();
                putEntry(entry.mKey, entry);
            } catch (final IOException e) {
                e.printStackTrace();
                file.delete();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    /**
     * Invalidates an entry in the cache.
     *
     * @param key        Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    @Override
    public synchronized void invalidate(final String key, final boolean fullExpire) {
        final Entry entry = getEntry(key);
        if (entry != null) {
            entry.softTtl = 0;
            if (fullExpire) {
                entry.ttl = 0;
            }
            put(key, entry, 0);
        }

    }

    /*
     * Homebrewed simple serialization system used for reading and writing cache
     * headers on disk. Once upon a time, this used the standard Java
     * Object{Input,Output}Stream, but the default implementation relies heavily
     * on reflection (even for standard types) and generates a ton of garbage.
     */

    /**
     * Puts the entry with the specified key into the cache.
     */
    @Override
    public synchronized boolean put(final String key, final Object value, long ttl) {
        final Entry entry = new Entry();
        final Parcel parcel = Parcel.obtain();

        try {
            parcel.setDataPosition(0);
            parcel.writeValue(value);
            if (ttl >= (Long.MAX_VALUE >> 1)) {
                ttl = Long.MAX_VALUE;
            } else {
                ttl = BigDecimal.valueOf(ttl)
                    .add(BigDecimal.valueOf(System.currentTimeMillis())).longValue();
            }
            entry.ttl = entry.softTtl = ttl;
            entry.data = parcel.marshall();
            return putEntry(key, entry);
        } finally {
            parcel.recycle();
        }
    }

    /**
     * Removes the specified key from the cache if it exists.
     */
    @Override
    public synchronized void remove(final String key) {
        final boolean deleted = getFileForKey(key).delete();
        removeEntry(key);
        if (!deleted) {
            Log.d(TAG, String.format("Could not delete cache entry for key=%s, filename=%s", key, getFilenameForKey(key)));
        }
    }

    protected synchronized Entry getEntry(final String key) {
        final CacheHeader entry = this.mEntries.get(key);
        if (entry == null) {
            return null;
        }

        final File file = getFileForKey(key);
        CountingInputStream cis = null;

        try {
            cis = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
            CacheHeader.readHeader(cis); // eat header
            final byte[] data = streamToBytes(cis, (int) (file.length() - cis.mBytesRead));
            return entry.toCacheEntry(data);
        } catch (final IOException e) {
            remove(key);
            return null;
        } catch (final NegativeArraySizeException e) {
            remove(key);
            return null;
        } finally {
            if (cis != null) {
                try {
                    cis.close();
                } catch (IOException ioe) {
                    return null;
                }
            }
        }
    }

    protected synchronized boolean putEntry(final String key, final Entry entry) {
        pruneIfNeeded(entry.data.length);
        final File file = getFileForKey(key);

        BufferedOutputStream fos = null;

        try {
            fos = new BufferedOutputStream(new FileOutputStream(file));
            final CacheHeader e = new CacheHeader(key, entry);
            if (!e.writeHeader(fos)) {
                fos.close();
                Log.d(TAG, String.format("Failed to write header for %s", file.getAbsolutePath()));
                throw new IOException();
            }

            fos.write(entry.data);
            fos.close();
            putEntry(key, e);
            return true;
        } catch (final IOException e) {
            if (!file.delete()) {
                Log.d(TAG, String.format("Could not clean up file %s", file.getAbsolutePath()));
            }
            return false;
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                fos = null;
            }
        }
    }

    /**
     * Creates a pseudo-unique filename for the specified cache key.
     *
     * @param key The key to generate a file name for.
     * @return A pseudo-unique filename.
     */
    private String getFilenameForKey(final String key) {
        final int firstHalfLength = key.length() / 2;
        String localFilename = String.valueOf(key.substring(0, firstHalfLength).hashCode());
        localFilename += String.valueOf(key.substring(firstHalfLength).hashCode());
        return localFilename;
    }

    /**
     * Returns a file object for the given cache key.
     */
    public File getFileForKey(final String key) {
        return new File(this.mRootDirectory, getFilenameForKey(key));
    }

    /**
     * Prunes the cache to fit the amount of bytes specified.
     *
     * @param neededSpace The amount of bytes we are trying to fit into the cache.
     */
    private void pruneIfNeeded(final long neededSpace) {
        if ((this.mTotalSize + neededSpace) < this.mMaxCacheSizeInBytes) {
            return;
        }

        for (final Iterator<Map.Entry<String, CacheHeader>> i = this.mEntries.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<String, CacheHeader> entry = i.next();
            final CacheHeader e = entry.getValue();
            final boolean deleted = getFileForKey(e.mKey).delete();
            if (deleted) {
                this.mTotalSize -= e.mSize;
            } else {
                Log.d(TAG, String.format("Could not delete cache entry for key=%s, filename=%s", e.mKey, getFilenameForKey(e.mKey)));
            }

            i.remove();

            if ((this.mTotalSize + neededSpace) < this.mMaxCacheSizeInBytes * HYSTERESIS_FACTOR) {
                break;
            }
        }
    }

    /**
     * Puts the entry with the specified key into the cache.
     *
     * @param key   The key to identify the entry by.
     * @param entry The entry to cache.
     */
    private void putEntry(final String key, CacheHeader entry) {
        if (!this.mEntries.containsKey(key)) {
            this.mTotalSize += entry.mSize;
        } else {
            final CacheHeader oldEntry = this.mEntries.get(key);
            this.mTotalSize += (entry.mSize - oldEntry.mSize);
        }

        this.mEntries.put(key, entry);
    }

    /**
     * Removes the entry identified by 'key' from the cache.
     */
    private void removeEntry(final String key) {
        final CacheHeader entry = this.mEntries.get(key);
        if (entry != null) {
            this.mTotalSize -= entry.mSize;
            this.mEntries.remove(key);
        }
    }

    /**
     * Handles holding onto the cache headers for an entry.
     */
    // Visible for testing.
    static class CacheHeader {
        /**
         * The size of the data identified by this CacheHeader. (This is not
         * serialized to disk.
         */
        public long mSize;

        /**
         * The key that identifies the cache entry.
         */
        public String mKey;

        /**
         * ETag for cache coherence.
         */
        public String mEtag;

        /**
         * Date of this response as reported by the server.
         */
        public long mServerDate;

        /**
         * The last modified date for the requested object.
         */
        public long mLastModified;

        /**
         * TTL for this record.
         */
        public long mTtl;

        /**
         * Soft TTL for this record.
         */
        public long mSoftTtl;

        /**
         * Headers from the response resulting in this cache entry.
         */
        public Map<String, String> mResponseHeaders;

        private CacheHeader() {
        }

        /**
         * Instantiates a new CacheHeader object
         *
         * @param key   The key that identifies the cache entry
         * @param entry The cache entry.
         */
        CacheHeader(final String key, final Entry entry) {
            this.mKey = key;
            this.mSize = entry.data.length;
            this.mEtag = entry.etag;
            this.mServerDate = entry.serverDate;
            this.mLastModified = entry.lastModified;
            this.mTtl = entry.ttl;
            this.mSoftTtl = entry.softTtl;
            this.mResponseHeaders = entry.responseHeaders;
        }

        /**
         * Reads the header off of an InputStream and returns a CacheHeader object.
         *
         * @param is The InputStream to read from.
         * @throws IOException
         */
        public static CacheHeader readHeader(final InputStream is) throws IOException {
            final CacheHeader entry = new CacheHeader();
            final int magic = readInt(is);
            if (magic != CACHE_MAGIC) {
                // don't bother deleting, it'll get pruned eventually
                throw new IOException();
            }

            entry.mKey = readString(is);
            entry.mEtag = readString(is);
            if (entry.mEtag.equals("")) {
                entry.mEtag = null;
            }
            entry.mServerDate = readLong(is);
            entry.mLastModified = readLong(is);
            entry.mTtl = readLong(is);
            entry.mSoftTtl = readLong(is);
            entry.mResponseHeaders = readStringStringMap(is);
            return entry;
        }

        /**
         * Creates a cache entry for the specified data.
         */
        public Entry toCacheEntry(final byte[] data) {
            final Entry e = new Entry();
            e.data = data;
            e.etag = mEtag;
            e.serverDate = this.mServerDate;
            e.lastModified = this.mLastModified;
            e.ttl = this.mTtl;
            e.softTtl = this.mSoftTtl;
            e.responseHeaders = this.mResponseHeaders;
            return e;
        }


        /**
         * Writes the contents of this CacheHeader to the specified OutputStream.
         */
        public boolean writeHeader(final OutputStream os) {
            try {
                writeInt(os, CACHE_MAGIC);
                writeString(os, mKey);
                writeString(os, mEtag == null ? "" : mEtag);
                writeLong(os, mServerDate);
                writeLong(os, mLastModified);
                writeLong(os, mTtl);
                writeLong(os, mSoftTtl);
                writeStringStringMap(mResponseHeaders, os);
                os.flush();
                return true;
            } catch (final IOException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }

    }

    private static class CountingInputStream extends FilterInputStream {

        private int mBytesRead = 0;

        private CountingInputStream(final InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            final int result = super.read();
            if (result != -1) {
                this.mBytesRead++;
            }

            return result;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int count) throws IOException {
            final int result = super.read(buffer, offset, count);
            if (result != -1) {
                this.mBytesRead += result;
            }

            return result;
        }
    }

}
