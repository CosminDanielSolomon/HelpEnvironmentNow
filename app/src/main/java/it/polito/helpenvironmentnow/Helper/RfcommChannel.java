package it.polito.helpenvironmentnow.Helper;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class RfcommChannel {

    private final String TAG = "RfcommChannel";
    private InputStream channelInputStream;
    private OutputStream channelOutputStream;
    private InputStreamReader isr;
    private OutputStreamWriter osw;
    private JsonReader jsonReader;
    private JsonWriter jsonWriter;

    RfcommChannel(InputStream channelInputStream, OutputStream channelOutputStream) {
        this.channelInputStream = channelInputStream;
        this.channelOutputStream = channelOutputStream;
        this.isr = new InputStreamReader(channelInputStream, StandardCharsets.UTF_8);
        this.osw = new OutputStreamWriter(channelOutputStream, StandardCharsets.UTF_8);
    }

    public InputStream getChannelInputStream() {
        return channelInputStream;
    }

    public OutputStream getChannelOutputStream() {
        return channelOutputStream;
    }

    public JsonReader getJsonReader() {
        this.jsonReader = new JsonReader(isr);
        return jsonReader;
    }

    public JsonWriter getJsonWriter() {
        this.jsonWriter = new JsonWriter(osw);
        return  jsonWriter;
    }

    public void close() {
        closeReader();
        closeWriter();
    }

    private void closeReader() {
        try {
            jsonReader.close();
        } catch (IOException e) {
            Log.e(TAG, "Close JsonReader failed!");
        }
    }

    private void closeWriter() {
        try {
            jsonWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Close JsonWriter failed!");
        }
    }
}
