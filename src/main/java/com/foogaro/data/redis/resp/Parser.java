package com.foogaro.data.redis.resp;

import com.foogaro.data.redis.resp.exceptions.RedisError;
import com.foogaro.data.redis.resp.exceptions.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Parser {

    private final Logger logger = LoggerFactory.getLogger(Parser.class);

    private final InputStream input;

    public Parser(InputStream input) {
        this.input = input;
    }

    public Object parse() throws RedisError, RedisException {
        Object result;
        int read = 0;
        try {
            read = this.input.read();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RedisException(e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Read: {}", (char)read);
        }
        switch (read) {
            case '+':
                result = this.parseSimpleString();
                break;
            case '-':
                throw new RedisError(new String(this.parseSimpleString()));
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
                throw new RedisException("Unexpected input: '" + (char) read + "'");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Result: {}", result);
        }
        return result;
    }

    private byte[] parseBulkString() throws RedisException {
        final long expectedLength = parseNumber();
        if (expectedLength == -1) {
            return null;
        }
        if (expectedLength > Integer.MAX_VALUE) {
            throw new RedisException("Unsupported length for bulk string");
        }
        final int numBytes = (int) expectedLength;
        final byte[] buffer = new byte[numBytes];
        int read = 0;
        try {
            while (read < expectedLength) {
                read += input.read(buffer, read, numBytes - read);
            }
            if (input.read() != '\r') {
                throw new RedisException("Expected CR");
            }
            if (input.read() != '\n') {
                throw new RedisException("Expected LF");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RedisException(e);
        }

        return buffer;
    }

    private byte[] parseSimpleString() throws RedisException {
        return scanForCR();
    }

    private long parseNumber() throws RedisException {
        return Long.parseLong(new String(scanForCR()));
    }

    private byte[] scanForCR() throws RedisException {
        int size = 32;
        int idx = 0;
        int ch;
        byte[] buffer = new byte[size];
        try {
            while (true) {
                    if (!((ch = input.read()) != '\r')) break;
                buffer[idx++] = (byte) ch;
                if (idx == size) {
                    size *= 2; // double the size of the buffer
                    buffer = Arrays.copyOf(buffer, size);
                }
            }
            if (input.read() != '\n') {
                throw new RedisException("Expected LF");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RedisException(e);
        }
        return Arrays.copyOfRange(buffer, 0, idx);
    }
}