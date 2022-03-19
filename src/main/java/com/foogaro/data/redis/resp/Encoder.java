package com.foogaro.data.redis.resp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Encoder {

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private final OutputStream out;
    public Encoder(OutputStream out) {
        this.out = out;
    }

    void write(byte[] value) throws IOException {
        out.write('$');
        out.write(Long.toString(value.length).getBytes());
        out.write(CRLF);
        out.write(value);
        out.write(CRLF);
    }

    void write(long val) throws IOException {
        out.write(':');
        out.write(Long.toString(val).getBytes());
        out.write(CRLF);
    }
    public void write(List<?> list) throws IOException, IllegalArgumentException {
        out.write('*');
        out.write(Long.toString(list.size()).getBytes());
        out.write(CRLF);
        for (Object o : list) {
            if (o instanceof byte[]) {
                write((byte[]) o);
            } else if (o instanceof String) {
                write(((String) o).getBytes());
            } else if (o instanceof Long) {
                write(((Long) o).toString().getBytes());
            } else if (o instanceof Integer) {
                write(((Integer) o).toString().getBytes());
            } else if (o instanceof List) {
                write((List<?>) o);
            } else {
                throw new IllegalArgumentException("Unexpected type " + o.getClass().getCanonicalName());
            }
        }
    }

    public void flush() throws IOException {
        out.flush();
    }
}