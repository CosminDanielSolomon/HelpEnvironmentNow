package it.polito.helpenvironmentnow;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class HeRestClient {
    private static final String TAG = "AppHelpNow";
    private static final String HE_WEB_SERVICE_URL = "http://10.1.23.126:8080/HelpEnvironment/helpenvironment/he/newdata";

    public void sendToServer(Context context, JSONObject dataBlock) {
        StringEntity entity = new StringEntity(dataBlock.toString(), StandardCharsets.UTF_8);
        SyncHttpClient restClient = new SyncHttpClient();

        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                Log.d(TAG, "PUT START");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "PUT SUCCESS:"+statusCode);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "PUT FAIL:"+statusCode);
            }

            @Override
            public void onRetry(int retryNo) {
                super.onRetry(retryNo);
                Log.d(TAG, "PUT RETRY:"+retryNo);
            }
        });
    }
}
