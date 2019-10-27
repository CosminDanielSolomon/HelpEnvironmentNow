package it.polito.helpenvironmentnow.Helper;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UploadWorker extends Worker {

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SensorUpload", "doWork() called");
        for(int i=0;i<25;i++) {
            SystemClock.sleep(1000);
            Log.d("SensorUpload", "sleep() "+ (i+1) + " executed");
        }
        return Result.success();
    }
}
