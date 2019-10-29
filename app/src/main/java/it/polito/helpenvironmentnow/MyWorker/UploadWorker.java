package it.polito.helpenvironmentnow.MyWorker;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.polito.helpenvironmentnow.HeRestClient;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.StoredJson;

public class UploadWorker extends Worker {
    private Context context;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SensorUpload", "doWork() called");
        boolean sendResult;
        MyDb myDb = new MyDb(context);
        Looper.prepare();
        HeRestClient heRestClient = new HeRestClient();
        for(StoredJson storedJson : myDb.getAllStoredJson()) {
            Log.d("SensorUpload","id" + storedJson.id);
            sendResult = heRestClient.sendToServerWithResult(context, storedJson.jsonSave);
            if(!sendResult)
                return Result.retry();
            myDb.deleteJsonObject(storedJson);
        }
        return Result.success();
    }
}
