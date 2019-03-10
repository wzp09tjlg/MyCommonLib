package com.wuzp.storagelib;

import android.content.Context;
import android.os.Parcelable;

import java.io.Serializable;

public abstract class Storage<T extends Parcelable> implements IStorage<T> {

    private IStorage mStorage;

    public Storage(Context context) {
        this.mStorage = new SharedPreferencesStorage(getClass().getName(), context);
    }

    public Storage(String name, Context context) {
        this.mStorage = new SharedPreferencesStorage(name, context);
    }

    @Override
    public void setData(T k) {
        this.mStorage.setData(k);
    }

    @Override
    public T getData() {
        return (T) this.mStorage.getData();
    }

    @Override
    public void clear() {
        this.mStorage.clear();
    }

    @Override
    public boolean has(String key) {
        return this.mStorage.has(key);
    }

    @Override
    public void remove(String key) {
        this.mStorage.remove(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return this.mStorage.getBoolean(key);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        this.mStorage.putBoolean(key, value);
    }

    @Override
    public IStorage setBoolean(String key, boolean value) {
        return this.mStorage.setBoolean(key, value);
    }

    @Override
    public int getInt(String key) {
        return this.mStorage.getInt(key);
    }

    @Override
    public void putInt(String key, int value) {
        this.mStorage.putInt(key, value);
    }

    @Override
    public IStorage setInt(String key, int value) {
        return this.mStorage.setInt(key, value);
    }

    @Override
    public float getFloat(String key) {
        return this.mStorage.getFloat(key);
    }

    @Override
    public void putFloat(String key, float value) {
        this.mStorage.putFloat(key, value);
    }

    @Override
    public IStorage setFloat(String key, float value) {
        return this.mStorage.setFloat(key, value);
    }

    @Override
    public long getLong(String key) {
        return this.mStorage.getLong(key);
    }

    @Override
    public void putLong(String key, long value) {
        this.mStorage.putLong(key, value);
    }

    @Override
    public IStorage setLong(String key, long value) {
        return this.mStorage.setLong(key, value);
    }

    @Override
    public String getString(String key) {
        return this.mStorage.getString(key);
    }

    @Override
    public void putString(String key, String value) {
        this.mStorage.putString(key, value);
    }

    @Override
    public IStorage setString(String key, String value) {
        return this.mStorage.setString(key, value);
    }

    @Override
    public Serializable getSerializable(String key) {
        return this.mStorage.getSerializable(key);
    }

    @Override
    public void putSerializable(String key, Serializable serializable) {
        this.mStorage.putSerializable(key, serializable);
    }

    @Override
    public void commit(Callback callback) {
        this.mStorage.commit(callback);
    }
}
