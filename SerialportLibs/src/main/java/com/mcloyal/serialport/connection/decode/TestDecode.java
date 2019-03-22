package com.mcloyal.serialport.connection.decode;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class TestDecode extends ProtocolDecoderAdapter {
    private final ClassLoader classLoader;
    private int maxObjectSize;

    public TestDecode() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public TestDecode(ClassLoader classLoader) {
        this.maxObjectSize = 1048576;
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader");
        } else {
            this.classLoader = classLoader;
        }
    }

    public int getMaxObjectSize() {
        return this.maxObjectSize;
    }

    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
        } else {
            this.maxObjectSize = maxObjectSize;
        }
    }

    @Override
    public void decode(IoSession ioSession, IoBuffer ioBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
        int limit = ioBuffer.limit();
        byte[] bytes = new byte[limit];

        ioBuffer.get(bytes);

        protocolDecoderOutput.write(bytes);
    }
}
