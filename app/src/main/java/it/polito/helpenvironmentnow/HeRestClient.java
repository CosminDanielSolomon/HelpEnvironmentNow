package it.polito.helpenvironmentnow;

import android.content.Context;
import android.util.Log;
import android.util.Patterns;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class HeRestClient {
    private String ipAddress = "192.168.137.1";
    private String port = "8080";
    private String HE_WEB_SERVICE_URL = "http://" + ipAddress + ":" + port + "/HelpEnvironment/helpenvironment/he/newdata";
    private SyncHttpClient restClient;
    private boolean sendResult;

    public HeRestClient() {
        this.restClient = new SyncHttpClient();
        Patterns.IP_ADDRESS.matcher(ipAddress).matches();
    }

    public void sendToServer(final Context context, final JSONObject dataBlock) {

        StringEntity entity = new StringEntity(dataBlock.toString(), StandardCharsets.UTF_8);
        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("Client", "Service PUT SUCCESS:"+statusCode);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Client", "Service PUT FAIL:"+statusCode);
                MyDb myDb = new MyDb(context);
                myDb.storeJsonObject(dataBlock);
                myDb.closeDb();
                MyWorkerManager.enqueueNetworkWorker(context);
            }
        });
    }

    public boolean sendToServerWithResult(final Context context, final String dataBlock) {
        StringEntity entity = new StringEntity(dataBlock, StandardCharsets.UTF_8);
        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("Client", "Worker PUT SUCCESS:"+statusCode);
                sendResult = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Client", "Worker PUT FAIL:"+statusCode);
                sendResult = false;
            }
        };
        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", responseHandler);

        return sendResult;
    }
}
