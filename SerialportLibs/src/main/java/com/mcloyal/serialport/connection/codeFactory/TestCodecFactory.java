package com.mcloyal.serialport.connection.codeFactory;

import com.mcloyal.serialport.connection.decode.TestDecode;
import com.mcloyal.serialport.connection.encode.TestEncode;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;



public class TestCodecFactory implements ProtocolCodecFactory {
    private final TestEncode encoder;
    private final TestDecode decoder;

    public TestCodecFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public TestCodecFactory(ClassLoader classLoader) {
        this.encoder = new TestEncode();
        this.decoder = new TestDecode(classLoader);
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) {
        return this.encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) {
        return this.decoder;
    }

    public int getEncoderMaxObjectSize() {
        return this.encoder.getMaxObjectSize();
    }

    public void setEncoderMaxObjectSize(int maxObjectSize) {
        this.encoder.setMaxObjectSize(maxObjectSize);
    }

    public int getDecoderMaxObjectSize() {
        return this.decoder.getMaxObjectSize();
    }

    public void setDecoderMaxObjectSize(int maxObjectSize) {
        this.decoder.setMaxObjectSize(maxObjectSize);
    }
}
