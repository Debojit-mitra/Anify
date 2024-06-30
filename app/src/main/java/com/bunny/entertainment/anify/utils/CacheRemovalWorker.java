package com.bunny.entertainment.anify.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.Objects;

public class CacheRemovalWorker extends Worker {

    public CacheRemovalWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        clearCache(getApplicationContext());
        return Result.success();
    }

    private void clearCache(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            try {
                for (File child : Objects.requireNonNull(cacheDir.listFiles())) {
                    deleteDir(child);
                }
            } catch (Exception e) {
                Log.e("CacheRemovalWorker", e.toString());
            }
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
