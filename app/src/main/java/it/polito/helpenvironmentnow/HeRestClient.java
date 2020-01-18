package it.polito.helpenvironmentnow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.SyncHttpClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class HeRestClient {

    // Server URL
    private String ipAddress = "10.1.23.126";
    private String port = "8443";
    private String HE_WEB_SERVICE_URL = "https://" + ipAddress + ":" + port + "/HelpEnvironment/helpenvironment/he/measures";

    private Context context;
    private SyncHttpClient restClient;
    private boolean sendResult;

    public HeRestClient(Context context) {
        this.context = context;
        restClient = new SyncHttpClient();
        acceptAllCertificate();
        restClient.setBasicAuth("androidClient","a147_mx5:3");
        //restClient.setTimeout(60*1000);
        restClient.setResponseTimeout(3600*1000);
        updateServerAddress(context);
    }

    public boolean sendToServerWithResult(final String dataBlock) {
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
        StringEntity entity = new StringEntity(dataBlock, StandardCharsets.UTF_8);
        restClient.put(context, HE_WEB_SERVICE_URL, entity, "application/json", responseHandler);

        return sendResult;
    }

    private void acceptAllCertificate() {
        MySSLSocketFactory socketFactory = null;
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            socketFactory = new MySSLSocketFactory(trustStore);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (socketFactory != null) {
            socketFactory.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            restClient.setSSLSocketFactory(socketFactory);
        }
    }

    // This method is only used for development purpose in order to allow the developer to change
    // the ip and port of the remote server to connect to without changing the application code
    // and re-compile it
    private void updateServerAddress(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.config_file), Context.MODE_PRIVATE);
        String IP_KEY = context.getString(R.string.IP_KEY);
        String IP_DEF = context.getString(R.string.IP_DEF);
        String PORT_KEY = context.getString(R.string.PORT_KEY);
        String PORT_DEF = context.getString(R.string.PORT_DEF);
        String ipSave = sharedPref.getString(IP_KEY, IP_DEF);
        String portSave = sharedPref.getString(PORT_KEY, PORT_DEF);
        Log.d("Client", "ipSave:"+ipSave);
        Log.d("Client", "portSave:"+portSave);
        if(!ipSave.equals(IP_DEF))
            ipAddress = ipSave;
        if(!portSave.equals(PORT_DEF))
            port = portSave;
    }
}
