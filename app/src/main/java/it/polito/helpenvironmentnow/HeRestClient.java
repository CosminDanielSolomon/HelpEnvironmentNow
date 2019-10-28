package it.polito.helpenvironmentnow;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class HeRestClient {
    private final static String HE_WEB_SERVICE_URL = "http://10.1.23.126:8080/HelpEnvironment/helpenvironment/he/newdata";
    private static SyncHttpClient restClient = new SyncHttpClient();
    private boolean sendResult;

    public static void sendToServer(final Context context, final JSONObject dataBlock) {
        StringEntity entity = new StringEntity(dataBlock.toString(), StandardCharsets.UTF_8);
        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("Client", "PUT SUCCESS:"+statusCode);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Client", "PUT FAIL:"+statusCode);
                MyDb myDb = new MyDb(context);
                myDb.storeJsonObject(dataBlock);
                MyWorkerManager.enqueueNetworkWorker(context);
            }

            @Override
            public void onRetry(int retryNo) {
                super.onRetry(retryNo);
            }
        });
    }

    public boolean sendToServerWithResult(final Context context, final String dataBlock) {
        StringEntity entity = new StringEntity(dataBlock, StandardCharsets.UTF_8);
        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("Client", "PUT SUCCESS:"+statusCode);
                sendResult = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Client", "PUT FAIL:"+statusCode);
                sendResult = false;
            }

            @Override
            public void onRetry(int retryNo) {
                super.onRetry(retryNo);
            }
        });

        return sendResult;
    }
}
