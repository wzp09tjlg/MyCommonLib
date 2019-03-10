package com.wuzp.storagelib;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

public abstract class Streams {

    private Streams() {
    }

    public static byte[] readFully(final InputStream in) throws IOException {
        try {
            return readFullyNoClose(in);
        } finally {
            in.close();
        }
    }

    public static byte[] readFullyNoClose(final InputStream in) throws IOException {
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        for (int count = -1; (count = in.read(buffer)) != -1;) {
            bytes.write(buffer, 0, count);
        }

        return bytes.toByteArray();
    }

    public static String readFullyNoClose(final Reader reader) throws IOException {
        final char[] buffer = new char[1024];
        final StringWriter writer = new StringWriter();

        for (int count = -1; (count = reader.read(buffer)) != -1;) {
            writer.write(buffer, 0, count);
        }

        return writer.toString();
    }

    public static String readFully(final Reader reader) throws IOException {
        try {
            return readFullyNoClose(reader);
        } finally {
            reader.close();
        }
    }

    public static void consume(InputStream in) throws IOException {
        do {
            in.skip(Long.MAX_VALUE);
        } while (in.read() != -1);
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is
     * closed. Returns the total number of bytes transferred.
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];

        int total = 0;

        try {
            for (int c = -1; (c = in.read(buffer)) != -1;) {
                total += c;
                out.write(buffer, 0, c);
            }
        } finally {
            out.flush();
        }

        return total;
    }

    public static void closeQuietly(final Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (final IOException e) {
            }
        }
    }
}
