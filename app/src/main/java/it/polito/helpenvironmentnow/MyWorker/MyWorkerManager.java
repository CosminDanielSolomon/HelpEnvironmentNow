package it.polito.helpenvironmentnow.MyWorker;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MyWorkerManager {
    public static void enqueueNetworkWorker(Context context) {
        final String WORK_TAG = "uploadSensorsData";
        // Create a Constraints object that defines when the task should run
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        // A WorkRequest for non-repeating work
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, uploadWorkRequest);
    }
}
