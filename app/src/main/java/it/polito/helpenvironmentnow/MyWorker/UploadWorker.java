package it.polito.helpenvironmentnow.MyWorker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.StoredJson;

public class UploadWorker extends Worker {

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SensorUpload", "doWork() called");
        /*for(int i=0;i<25;i++) {
            SystemClock.sleep(1000);
            Log.d("SensorUpload", "sleep() "+ (i+1) + " executed");
        }*/
        MyDb myDb = new MyDb(getApplicationContext());
        for(StoredJson storedJson : myDb.getAllStoredJson()) {
            Log.d("SensorUpload","id" + storedJson.id);

            myDb.deleteJsonObject(storedJson);
        }
        return Result.success();
    }
}
