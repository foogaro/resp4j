package com.foogaro.data.redis.resp;

import com.foogaro.data.redis.resp.exceptions.RedisError;
import com.foogaro.data.redis.resp.exceptions.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Redis {

    private final Logger logger = LoggerFactory.getLogger(Redis.class);

    private final Encoder writer;
    private final Parser reader;
    private static final String default_redis_host = "localhost";
    private static final int default_redis_port = 6379;
    private static final int default_redis_input_buffer_size = 32;
    private static final int default_redis_output_buffer_size = 32;


    public Redis() throws IOException {
            this(new Socket(default_redis_host, default_redis_port), 1 << default_redis_input_buffer_size, 1 << default_redis_output_buffer_size);
    }

    public Redis(Socket socket) throws IOException {
        this(socket, 1 << default_redis_input_buffer_size, 1 << default_redis_output_buffer_size);
    }

    public Redis(Socket socket, int inputBufferSize, int outputBufferSize) throws IOException {
        this(
                new BufferedInputStream(socket.getInputStream(), inputBufferSize),
                new BufferedOutputStream(socket.getOutputStream(), outputBufferSize)
        );
    }

    public Redis(InputStream inputStream, OutputStream outputStream) {
        reader = new Parser(inputStream);
        writer = new Encoder(outputStream);
    }

    public <T> T call(Object... args) throws RedisException, RedisError {
        if (logger.isDebugEnabled()) {
            Arrays.stream(args).forEach(o -> logger.debug("Arguments: {}", args));
        }
        try {
            writer.write(Arrays.asList(args));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        T result = read();
        if (logger.isDebugEnabled()) {
            logger.debug("Result: {}", result);
        }
        return result;
    }

    public <T> T read() throws RedisException, RedisError {
        return (T) reader.parse();
    }

    @FunctionalInterface
    public interface RedisClient<T, E extends Throwable> {
        void accept(T t) throws E, RedisError;
    }

    public static void run(RedisClient<Redis, IOException> callback, String addr, int port) throws IOException, RedisError {
        try (RedisConnection redis = connect(addr, port)) {
            callback.accept(redis);
        }
    }

    public static void run(RedisClient<Redis, IOException> callback) throws IOException, RedisError {
        run(callback,default_redis_host, default_redis_port);
    }

    public static void run(RedisClient<Redis, IOException> callback, Socket s) throws IOException, RedisError {
        callback.accept(new Redis(s));
    }

    public abstract static class RedisConnection extends Redis implements AutoCloseable {
        RedisConnection(Socket s) throws IOException {
            super(s);
        }

        abstract public void close() throws IOException;
    }

    public static RedisConnection connect(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        return new RedisConnection(s) {
            @Override
            public void close() throws RedisException {
                try {
                    call("QUIT");
                    s.close();
                } catch (IOException | RedisError e) {
                    e.printStackTrace();
                    throw new RedisException(e);
                }
            }
        };
    }
}