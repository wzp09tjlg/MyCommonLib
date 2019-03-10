package com.wuzp.storagelib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@SuppressLint("CommitPrefEdits")
class SharedPreferencesStorage<T extends Parcelable> implements IStorage<T> {

    private static final ThreadPoolExecutor SINGLE_THREAD_POOL = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
        new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
        private final AtomicInteger mCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull final Runnable runnable) {
            return new Thread(runnable, "SharedPreferencesRepository#" + mCounter.getAndIncrement());
        }
    }, new ThreadPoolExecutor.DiscardPolicy());

    private static final String KEY_DATA = "Storage.Key";

    private final SharedPreferences mPref;

    private final Object mMutex = new Object();

    private SharedPreferences.Editor mEditor;

    SharedPreferencesStorage(final String name,Context context) {
        this.mPref =context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    @Override
    public void clear() {
        commit(this.mPref.edit().clear());
    }

    @Override
    public T getData() {
        return getParcelable(KEY_DATA);
    }

    @Override
    public void setData(T data) {
        putParcelable(KEY_DATA, data);
    }

    @Override
    public boolean has(final String key) {
        return this.mPref.contains(key);
    }

    @Override
    public void remove(final String key) {
        commit(this.mPref.edit().remove(key));
    }

    @Override
    public boolean getBoolean(final String key) {
        return this.mPref.getBoolean(key, false);
    }

    @Override
    public void putBoolean(final String key, final boolean value) {
        commit(this.mPref.edit().putBoolean(key, value));
    }

    @Override
    public IStorage setBoolean(String key, boolean value) {
        getEditor().putBoolean(key, value);
        return this;
    }

    @Override
    public int getInt(final String key) {
        return this.mPref.getInt(key, 0);
    }

    @Override
    public void putInt(final String key, final int value) {
        commit(this.mPref.edit().putInt(key, value));
    }

    @Override
    public IStorage setInt(String key, int value) {
        getEditor().putInt(key, value);
        return this;
    }

    @Override
    public float getFloat(final String key) {
        return this.mPref.getFloat(key, 0.f);
    }

    @Override
    public void putFloat(final String key, final float value) {
        commit(this.mPref.edit().putFloat(key, value));
    }

    @Override
    public IStorage setFloat(String key, float value) {
        getEditor().putFloat(key, value);
        return this;
    }

    @Override
    public long getLong(final String key) {
        return this.mPref.getLong(key, 0);
    }

    @Override
    public void putLong(final String key, final long value) {
        commit(this.mPref.edit().putLong(key, value));
    }

    @Override
    public IStorage setLong(String key, long value) {
        getEditor().putLong(key, value);
        return this;
    }

    @Override
    public String getString(final String key) {
        return this.mPref.getString(key, null);
    }

    @Override
    public void putString(final String key, final String value) {
        commit(this.mPref.edit().putString(key, value));
    }

    @Override
    public IStorage setString(String key, String value) {
        getEditor().putString(key, value);
        return this;
    }

    public <T extends Parcelable> T getParcelable(final String key) {
        long start = System.currentTimeMillis();
        final String raw = this.mPref.getString(key, null);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        final byte[] data = Base64.decode(raw.getBytes(), Base64.DEFAULT);
//        final byte[] data = raw.getBytes();

        final Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            return (T) parcel.readValue(getClass().getClassLoader());
        } finally {
            parcel.recycle();
            long end = System.currentTimeMillis();
        }
    }

    public <T extends Parcelable> void putParcelable(final String key, final T value) {
        if (null == value) {
            this.remove(key);
        } else {
            final Parcel parcel = Parcel.obtain();
            try {
                parcel.writeValue(value);
                commit(this.mPref.edit().putString(key, Base64.encodeToString(parcel.marshall(), Base64.DEFAULT)));
            } finally {
                parcel.recycle();
            }
        }
    }

    public <T extends Parcelable> IStorage setParcelable(String key, T value) {
        if (null == value) {
            getEditor().remove(key);
        } else {
            final Parcel parcel = Parcel.obtain();

            try {
                parcel.writeValue(value);
                getEditor().putString(key, new String(parcel.marshall()));
            } finally {
                parcel.recycle();
            }
        }
        return this;
    }

    @Override
    public void commit(Callback callback) {
        synchronized (this.mMutex) {
            final SharedPreferences.Editor editor = this.mEditor;
            if (null == editor) {
                if (null != callback) {
                    callback.done(false);
                }
                return;
            }

            try {
                commit(editor, callback);
            } finally {
                this.mEditor = null;
            }
        }
    }

    @Override
    public void putSerializable(String key, Serializable serializable) {
        if (serializable == null || TextUtils.isEmpty(key)) {
            return;
        }
        ObjectOutputStream objectStream = null;
        ByteArrayOutputStream byteArrayStream = null;
        try {
            byteArrayStream = new ByteArrayOutputStream();
            objectStream = new ObjectOutputStream(byteArrayStream);
            objectStream.writeObject(serializable);
            objectStream.flush();
            putString(key, Base64.encodeToString(byteArrayStream.toByteArray(), Base64.DEFAULT));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Streams.closeQuietly(byteArrayStream);
            Streams.closeQuietly(objectStream);
        }

    }

    @Override
    public Serializable getSerializable(String key) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(getString(key))) {
            return null;
        }

        long start = System.currentTimeMillis();

        String value = getString(key);
        byte[] data = Base64.decode(value, Base64.DEFAULT);
        if (data == null) {
            return null;
        }
        ObjectInputStream objectStream = null;
        try {
            objectStream = new ObjectInputStream(new ByteArrayInputStream(data));
            return (Serializable) objectStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Streams.closeQuietly(objectStream);
            long end = System.currentTimeMillis();
        }
        return null;
    }

    private SharedPreferences.Editor getEditor() {
        synchronized (this.mMutex) {
            if (null == this.mEditor) {
                this.mEditor = this.mPref.edit();
            }
            return this.mEditor;
        }
    }

    private static void commit(final SharedPreferences.Editor editor) {
        commit(editor, null);
    }

    private static void commit(final SharedPreferences.Editor editor, final Callback callback) {
        SINGLE_THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                final boolean result = editor.commit();

                if (null != callback) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(result);
                        }
                    });
                }
            }
        });
    }

}
