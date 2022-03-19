package com.foogaro.data.redis.resp;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Redis {

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
        this.reader = new Parser(inputStream);
        this.writer = new Encoder(outputStream);
    }

    public <T> T call(Object... args) throws IOException {
        writer.write(Arrays.asList(args));
        writer.flush();
        return read();
    }

    public <T> T read() throws IOException {
        return (T) reader.parse();
    }

    public static abstract class Pipeline {
        public abstract Pipeline call(String... args) throws IOException;
        public abstract List<Object> read() throws IOException;
    }

    public Pipeline pipeline() {
        return new Pipeline() {
            private int n = 0;

            public Pipeline call(String... args) throws IOException {
                writer.write(Arrays.asList((Object[]) args));
                writer.flush();
                n++;
                return this;
            }

            public List<Object> read() throws IOException {
                List<Object> ret = new LinkedList<>();
                while (n-- > 0) {
                    ret.add(reader.parse());
                }
                return ret;
            }
        };
    }

    @FunctionalInterface
    public interface RedisClient<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    public static void run(RedisClient<Redis, IOException> callback, String addr, int port) throws IOException {
        try (RedisConnection redis = connect(addr, port)) {
            callback.accept(redis);
        }
    }

    public static void run(RedisClient<Redis, IOException> callback) throws IOException {
        run(callback,default_redis_host, default_redis_port);
    }

    public static void run(RedisClient<Redis, IOException> callback, Socket s) throws IOException {
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
            public void close() throws IOException {
                call("QUIT");
                s.close();
            }
        };
    }
}