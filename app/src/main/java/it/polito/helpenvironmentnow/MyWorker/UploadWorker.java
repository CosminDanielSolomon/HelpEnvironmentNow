package it.polito.helpenvironmentnow.MyWorker;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

import it.polito.helpenvironmentnow.HeRestClient;
import it.polito.helpenvironmentnow.Helper.JsonBuilder;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class UploadWorker extends Worker {
    private String TAG = "NetworkWorker";
    private Context context;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork() called");
        boolean sendResult;
        if(Looper.myLooper() == null) {
            Log.d(TAG, "Looper prepare called");
            Looper.prepare();
        }
        MyDb myDb = new MyDb(context);
        JsonBuilder jsonBuilder = new JsonBuilder();
        HeRestClient heRestClient = new HeRestClient(context);

        long currentMeasures = 0;
        long totMeasures = myDb.getTotalMeasures();
        Log.d(TAG,"tot measures: " + totMeasures);

        while(currentMeasures < totMeasures) {
            List<Measure> measures = myDb.getSomeMeasures();
            Log.d(TAG,"List size: " + measures.size());

            JSONObject jsonObject = jsonBuilder.buildDataBlock(measures);
            if(jsonObject == null) {
                releaseResources(myDb);
                return Result.retry();
            }
            sendResult = heRestClient.sendToServerWithResult(jsonObject.toString());
            if(!sendResult) {
                releaseResources(myDb);
                return Result.retry();
            }
            myDb.deleteMeasures(measures);
            currentMeasures += measures.size();
        }

        releaseResources(myDb);
        return Result.success();
    }

    public void releaseResources(MyDb myDb) {
        Objects.requireNonNull(Looper.myLooper()).quit();
        myDb.closeDb();
    }
}
