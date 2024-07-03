package com.bunny.entertainment.factoid.utils;

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_DOWNLOAD_FAILED;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.bunny.entertainment.factoid.R;

public class DownloadService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "download_channel";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String imageUrl = intent.getStringExtra("image_url");
        if (imageUrl != null) {
            startForeground(NOTIFICATION_ID, createNotification("Downloading image..."));
            downloadImage(imageUrl);
        }
        return START_NOT_STICKY;
    }

    private void downloadImage(String imageUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("Downloading Anime Image");
        request.setDescription("Downloading image from Factoid widget");

        String fileName = "factoid_image_" + System.currentTimeMillis() + ".jpg";

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Factoid/" + fileName);


        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final long downloadId = downloadManager.enqueue(request);

        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);
                try (Cursor cursor = downloadManager.query(q)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                        if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1 && statusIndex != -1) {
                            int bytesDownloaded = cursor.getInt(bytesDownloadedIndex);
                            int bytesTotal = cursor.getInt(bytesTotalIndex);
                            int status = cursor.getInt(statusIndex);

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                downloading = false;
                                updateNotification("Download complete");
                                sendDownloadCompleteBroadcast();
                                // Show toast on the main thread
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> Toast.makeText(DownloadService.this, "Download complete", Toast.LENGTH_SHORT).show());
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                downloading = false;
                                updateNotification("Download failed");
                                sendDownloadFailedBroadcast();
                                // Show toast on the main thread
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> Toast.makeText(DownloadService.this, "Download failed", Toast.LENGTH_SHORT).show());
                            } else if (bytesTotal > 0) {
                                int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                updateNotification("Downloading: " + progress + "%");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("DownloadService", "Error querying download status", e);
                    downloading = false;
                }

                // Add a small delay to avoid excessive CPU usage
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    downloading = false;
                }
            }


            stopForeground(STOP_FOREGROUND_REMOVE);

            stopSelf();
        }).start();
    }

    private Notification createNotification(String content) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Anime Image Download")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        return builder.build();
    }

    private void updateNotification(String content) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, createNotification(content));
    }

    private void createNotificationChannel() {

        CharSequence name = "Download Channel";
        String description = "Channel for Download Service";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

    }

    private void sendDownloadCompleteBroadcast() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
            intent.setPackage(getPackageName());
            sendOrderedBroadcast(intent, null);
            Log.d("DownloadService", "Sent download complete broadcast");
        });
    }

    private void sendDownloadFailedBroadcast() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(ACTION_DOWNLOAD_FAILED);
            intent.setPackage(getPackageName());
            sendOrderedBroadcast(intent, null);
            Log.d("DownloadService", "Sent download failed broadcast");
        });
    }

}
