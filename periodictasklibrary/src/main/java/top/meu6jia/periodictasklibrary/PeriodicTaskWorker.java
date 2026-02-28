package top.meu6jia.periodictasklibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PeriodicTaskWorker extends Worker {
    private static final String TAG = "PeriodicTaskWorker";

    public static String dateNowString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private static void logToFile(Context context, String logLine) {
        Log.d(TAG, logLine);
        try {
            File logFile = new File(context.getFilesDir(), "PeriodicTaskWorker_log.txt");
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(logLine.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log to file", e);
        }
    }

    public static boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = powerManager.isInteractive();
        Log.d(TAG, "isScreenOn: " + isScreenOn);
        return isScreenOn;
    }

    public static void shutDown(Context context) {
        Intent intent = new Intent("com.xinyi.action.Shut_down");
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.MyControlledReceiver"));
        Log.d(TAG, "shutDown: ");
        context.sendBroadcast(intent);
    }

    public static void enqueue(Context context, long repeatMinutes, long delayMinutes) {
        Constraints constraints = new Constraints.Builder()
//                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest
                .Builder(PeriodicTaskWorker.class, repeatMinutes, TimeUnit.MINUTES)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager wm = WorkManager.getInstance(context);
        wm.enqueueUniquePeriodicWork("PeriodicTaskWorker", ExistingPeriodicWorkPolicy.KEEP, workRequest);
        String logLine = "enqueue: " + dateNowString();
        logToFile(context, logLine + "\n");
    }

    public static void cancel(Context applicationContext) {
        WorkManager.getInstance(applicationContext).cancelUniqueWork("PeriodicTaskWorker");
        String logLine = "cancel: " + dateNowString();
        logToFile(applicationContext, logLine + "\n");
    }

    public PeriodicTaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "doWork: running");
            boolean isScreenOn = isScreenOn(getApplicationContext());
            String logLine = "doWork：" + dateNowString() + " " + (isScreenOn ? "ScreenOn" : "ScreenOff");
            logToFile(getApplicationContext(), logLine + "\n");
            if (isScreenOn) {
                Log.d(TAG, "doWork：ScreenOn！");
                return Result.success();
            }
            Log.d(TAG, "doWork：exec");
            shutDown(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "doWork...catch", e);
            String logLine = "doWork：" + dateNowString() + " catch" + e.getMessage();
            logToFile(getApplicationContext(), logLine + "\n");
            return Result.failure();
        }
    }
}
