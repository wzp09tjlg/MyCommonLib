package com.wuzp.storagelib;

import java.io.Serializable;

public interface IStorage<T> {

    void clear();

    T getData();

    void setData(T t);

    boolean has(final String key);

    void remove(final String key);

    boolean getBoolean(final String key);

    void putBoolean(final String key, final boolean value);

    IStorage setBoolean(final String key, final boolean value);

    int getInt(final String key);

    void putInt(final String key, final int value);

    IStorage setInt(final String key, final int value);

    float getFloat(final String key);

    void putFloat(final String key, final float value);

    IStorage setFloat(final String key, final float value);

    long getLong(final String key);

    void putLong(final String key, final long value);

    IStorage setLong(final String key, final long value);

    String getString(final String key);

    void putString(final String key, final String value);

    IStorage setString(final String key, final String value);

    void commit(final Callback callback);

    void putSerializable(String key, Serializable serializable);

    Serializable getSerializable(String key);

    interface Callback {
        void done(final boolean result);
    }
}
