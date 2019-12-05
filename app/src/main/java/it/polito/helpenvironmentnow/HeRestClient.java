package it.polito.helpenvironmentnow;

import android.content.Context;
import android.content.SharedPreferences;
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

    private Context context;
    private String ipAddress = "192.168.137.1";
    private String port = "8080";
    private String HE_WEB_SERVICE_URL = "http://" + ipAddress + ":" + port + "/HelpEnvironment/helpenvironment/he/newdata";
    private SyncHttpClient restClient;
    private boolean sendResult;

    public HeRestClient(Context context) {
        this.restClient = new SyncHttpClient();
        this.context = context;
        updateServerAddress(context);
    }

    public void sendToServer(final JSONObject dataBlock) {

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

    public boolean sendToServerWithResult(final String dataBlock) {
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

    private void updateServerAddress(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.config_file), Context.MODE_PRIVATE);
        String IP_KEY = context.getString(R.string.IP_KEY);
        String IP_DEF = context.getString(R.string.IP_DEF);
        String PORT_KEY = context.getString(R.string.PORT_KEY);
        String PORT_DEF = context.getString(R.string.PORT_DEF);
        String ipSave = sharedPref.getString(IP_KEY, IP_DEF);
        String portSave = sharedPref.getString(PORT_KEY, PORT_DEF);
        if(!ipSave.equals(IP_DEF))
            ipAddress = ipSave;
        if(!portSave.equals(PORT_DEF))
            port = portSave;
    }
}
