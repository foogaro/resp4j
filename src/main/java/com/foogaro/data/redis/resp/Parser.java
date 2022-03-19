package com.foogaro.data.redis.resp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Parser {

    private final InputStream input;

    static class ProtocolException extends IOException {
        ProtocolException(String msg) {
            super(msg);
        }
    }

    static class ServerError extends IOException {
        ServerError(String msg) {
            super(msg);
        }
    }

    public Parser(InputStream input) {
        this.input = input;
    }

    public Object parse() throws ProtocolException, IOException {
        Object result;
        int read = this.input.read();
        switch (read) {
            case '+':
                result = this.parseSimpleString();
                break;
            case '-':
                throw new ServerError(new String(this.parseSimpleString()));
            case ':':
                result = this.parseNumber();
                break;
            case '$':
                result = this.parseBulkString();
                break;
            case '*':
                long len = this.parseNumber();
                if (len == -1) {
                    result = null;
                } else {
                    List<Object> objects = new LinkedList<>();
                    for (long i = 0; i < len; i++) {
                        objects.add(this.parse());
                    }
                    result = objects;
                }
                break;
            case -1:
                return null;
            default:
                throw new ProtocolException("Unexpected input: " + (byte) read);
        }
        return result;
    }

    private byte[] parseBulkString() throws IOException {
        final long expectedLength = parseNumber();
        if (expectedLength == -1) {
            return null;
        }
        if (expectedLength > Integer.MAX_VALUE) {
            throw new ProtocolException("Unsupported length for bulk string");
        }
        final int numBytes = (int) expectedLength;
        final byte[] buffer = new byte[numBytes];
        int read = 0;
        while (read < expectedLength) {
            read += input.read(buffer, read, numBytes - read);
        }
        if (input.read() != '\r') {
            throw new ProtocolException("Expected CR");
        }
        if (input.read() != '\n') {
            throw new ProtocolException("Expected LF");
        }

        return buffer;
    }

    private byte[] parseSimpleString() throws IOException {
        return scanForCR();
    }

    private long parseNumber() throws IOException {
        return Long.parseLong(new String(scanForCR()));
    }

    private byte[] scanForCR() throws IOException {
        int size = 1024;
        int idx = 0;
        int ch;
        byte[] buffer = new byte[size];
        while ((ch = input.read()) != '\r') {
            buffer[idx++] = (byte) ch;
            if (idx == size) {
                size *= 2; // double the size of the buffer
                buffer = Arrays.copyOf(buffer, size);
            }
        }
        if (input.read() != '\n') {
            throw new ProtocolException("Expected LF");
        }

        return Arrays.copyOfRange(buffer, 0, idx);
    }
}