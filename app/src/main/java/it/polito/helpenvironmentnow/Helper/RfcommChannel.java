package it.polito.helpenvironmentnow.Helper;

import java.io.InputStream;
import java.io.OutputStream;

public class RfcommChannel {

    private InputStream channelInputStream;
    private OutputStream channelOutputStream;

    public RfcommChannel(InputStream channelInputStream, OutputStream channelOutputStream) {
        this.channelInputStream = channelInputStream;
        this.channelOutputStream = channelOutputStream;
    }

    public InputStream getChannelInputStream() {
        return channelInputStream;
    }

    public void setChannelInputStream(InputStream channelInputStream) {
        this.channelInputStream = channelInputStream;
    }

    public OutputStream getChannelOutputStream() {
        return channelOutputStream;
    }

    public void setChannelOutputStream(OutputStream channelOutputStream) {
        this.channelOutputStream = channelOutputStream;
    }
}
