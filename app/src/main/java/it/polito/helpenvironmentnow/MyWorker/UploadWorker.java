package it.polito.helpenvironmentnow.MyWorker;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Objects;

import it.polito.helpenvironmentnow.HeRestClient;
import it.polito.helpenvironmentnow.Storage.JsonTypes;
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
        if(Looper.myLooper() == null) {
            Log.d("SensorUpload", "Looper prepare called");
            Looper.prepare();
        }
        MyDb myDb = new MyDb(context);
        HeRestClient heRestClient = new HeRestClient(context);
        for(StoredJson storedJson : myDb.getAllStoredJson()) {
            Log.d("SensorUpload","id " + storedJson.id);
            sendResult = heRestClient.sendToServerWithResult(storedJson.jsonSave, getJsonTypes(storedJson.type));
            if(!sendResult) {
                Objects.requireNonNull(Looper.myLooper()).quit();
                myDb.closeDb();
                return Result.retry();
            }
            myDb.deleteJsonObject(storedJson);
        }
        Objects.requireNonNull(Looper.myLooper()).quit();
        myDb.closeDb();
        return Result.success();
    }

    private JsonTypes getJsonTypes(String type) {
        if(type.equals(JsonTypes.CLASSIC.getType()))
            return JsonTypes.CLASSIC;
        return JsonTypes.MOVEMENT;
    }
}
