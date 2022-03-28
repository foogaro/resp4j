package com.foogaro.data.redis.resp.exceptions;

public class RedisError extends Exception {

    public RedisError() {
    }

    public RedisError(String message) {
        super(message);
    }

    public RedisError(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisError(Throwable cause) {
        super(cause);
    }

    public RedisError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
