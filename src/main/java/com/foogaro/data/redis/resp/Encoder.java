package com.foogaro.data.redis.resp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Encoder {

    private final Logger logger = LoggerFactory.getLogger(Encoder.class);

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private final OutputStream out;
    public Encoder(OutputStream out) {
        this.out = out;
    }

    void write(byte[] value) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Command: ${}{}", Long.toString(value.length),value);
        }
        out.write('$');
        out.write(Long.toString(value.length).getBytes());
        out.write(CRLF);
        out.write(value);
        out.write(CRLF);
    }

    void write(long value) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Command: :{}", value);
        }
        out.write(':');
        out.write(Long.toString(value).getBytes());
        out.write(CRLF);
    }
    public void write(List<?> list) throws IOException, IllegalArgumentException {
        if (logger.isDebugEnabled()) {
            logger.debug("Command: *{}", list.size());
        }
        out.write('*');
        out.write(Long.toString(list.size()).getBytes());
        out.write(CRLF);
        for (Object o : list) {
            if (o instanceof byte[]) {
                write((byte[]) o);
            } else if (o instanceof List) {
                write((List<?>) o);
            } else if (o instanceof String) {
                write(((String) o).getBytes());
            } else if (o instanceof Long) {
                write(((Long) o).toString().getBytes());
            } else if (o instanceof Integer) {
                write(((Integer) o).toString().getBytes());
            } else {
                throw new IllegalArgumentException("Unexpected type " + o.getClass().getCanonicalName());
            }
        }
    }

    public void flush() throws IOException {
        out.flush();
    }
}