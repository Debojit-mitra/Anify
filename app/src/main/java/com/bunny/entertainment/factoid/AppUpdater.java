package com.bunny.entertainment.factoid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppUpdater {
    private static final String TAG = "AppUpdater";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Debojit-mitra/Factoid/releases/latest";
    public static final String INSTALL_ACTION = "com.bunny.entertainment.factoid.INSTALL_COMPLETE";

    private final Context context;
    private final OkHttpClient client;
    public MaterialAlertDialogBuilder progressDialogBuilder;
    private androidx.appcompat.app.AlertDialog progressDialog;
    private static final String PREFS_NAME = "AppUpdaterPrefs";
    private static final String PREF_DOWNLOADED_VERSION = "downloadedVersion";

    public AppUpdater(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void checkForUpdates(boolean showNoUpdateDialog) {
        if (!isReleaseBuild()) {
            Log.d(TAG, "Skipping update check for non-release build");
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(GITHUB_API_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String latestVersion = jsonResponse.getString("tag_name");
                        String currentVersion = getCurrentVersion();

                        if (isUpdateAvailable(currentVersion, latestVersion)) {
                            String downloadUrl = jsonResponse.getJSONArray("assets")
                                    .getJSONObject(0)
                                    .getString("browser_download_url");

                            if (isUpdateAlreadyDownloaded(latestVersion)) {
                                notifyUpdateReady(latestVersion);
                            } else {
                                notifyUpdate(latestVersion, downloadUrl);
                            }
                        } else if (showNoUpdateDialog) {
                            Log.e("Updater Checked", "No Update Available! You are using the latest version of the app.");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
        }).start();
    }

    private boolean isUpdateAlreadyDownloaded(String latestVersion) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String downloadedVersion = prefs.getString(PREF_DOWNLOADED_VERSION, "");
        File updateFile = new File(context.getExternalCacheDir(), "update.apk");
        return latestVersion.equals(downloadedVersion) && updateFile.exists();
    }

    private void notifyUpdateReady(String newVersion) {
        context.getMainExecutor().execute(() -> showUpdateReadyDialog(newVersion));
    }

    private void showUpdateReadyDialog(String newVersion) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Update Ready")
                .setMessage("A new version (" + newVersion + ") is ready to install. Do you want to install it now?")
                .setPositiveButton("Install", (dialog, which) -> installExistingUpdate())
                .setNegativeButton("Later", null)
                .show();
    }

    private void installExistingUpdate() {
        File updateFile = new File(context.getExternalCacheDir(), "update.apk");
        if (updateFile.exists()) {
            installUpdate(updateFile);
        } else {
            Toast.makeText(context, "Update file not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isReleaseBuild() {
        return !BuildConfig.DEBUG;
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting current version", e);
            return "";
        }
    }

    private boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        // Remove any leading 'v' if present
        currentVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;
        latestVersion = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;

        // Split versions into components
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        // Compare each component
        for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
            int currentPart = Integer.parseInt(currentParts[i]);
            int latestPart = Integer.parseInt(latestParts[i]);
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        // If all components are equal, check if latest has more components
        return latestParts.length > currentParts.length;
    }

    private void notifyUpdate(String newVersion, String downloadUrl) {
        context.getMainExecutor().execute(() -> showUpdateDialog(newVersion, downloadUrl));
    }

    private void showUpdateDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Update Available")
                .setMessage("A new version (" + newVersion + ") is available. Do you want to update?")
                .setPositiveButton("Update", (dialog, which) -> handleUpdateClick(newVersion, downloadUrl))
                .setNegativeButton("Later", null)
                .show();
    }


    private void handleUpdateClick(String newVersion, String downloadUrl) {
        if (!context.getPackageManager().canRequestPackageInstalls()) {
            showInstallPermissionDialog(newVersion, downloadUrl);
        } else {
            showDownloadConfirmationDialog(newVersion, downloadUrl);
        }
    }

    private void showInstallPermissionDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Permission Required")
                .setMessage("To install updates, this app needs permission to install unknown apps. Would you like to grant this permission?")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + context.getPackageName()));
                    if (context instanceof Activity) {
                        ((Activity) context).startActivityForResult(intent, MainActivity.REQUEST_INSTALL_PACKAGES);
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    storeDownloadInfo(newVersion, downloadUrl);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void storeDownloadInfo(String newVersion, String downloadUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("pendingNewVersion", newVersion)
                .putString("pendingDownloadUrl", downloadUrl)
                .apply();
    }

    public void checkAndDownloadPendingUpdate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String pendingNewVersion = prefs.getString("pendingNewVersion", null);
        String pendingDownloadUrl = prefs.getString("pendingDownloadUrl", null);
        if (pendingNewVersion != null && pendingDownloadUrl != null) {
            prefs.edit().remove("pendingNewVersion").remove("pendingDownloadUrl").apply();
            downloadAndInstallUpdate(pendingNewVersion, pendingDownloadUrl);
        }
    }


    private void showDownloadConfirmationDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Download Update")
                .setMessage("Are you ready to download the update?")
                .setPositiveButton("Download", (dialog, which) -> downloadAndInstallUpdate(newVersion, downloadUrl))
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void downloadAndInstallUpdate(String downloadUrl) {
        downloadAndInstallUpdate(null, downloadUrl);
    }

    public void downloadAndInstallUpdate(String newVersion, String downloadUrl) {
        progressDialogBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle("Downloading Update")
                .setView(R.layout.progress_dialog_layout)
                .setCancelable(false);
        progressDialog = progressDialogBuilder.create();
        progressDialog.show();

        new Thread(() -> {
            try {
                Log.d(TAG, "Downloading update from: " + downloadUrl);
                Request request = new Request.Builder().url(downloadUrl).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Failed to download update: " + response);
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new IOException("Empty response body");
                    }

                    long totalBytes = body.contentLength();
                    File file = new File(context.getExternalCacheDir(), "update.apk");
                    try (InputStream in = body.byteStream();
                         OutputStream out = Files.newOutputStream(file.toPath())) {
                        byte[] buffer = new byte[8192];
                        long downloadedBytes = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            downloadedBytes += bytesRead;
                            updateProgressDialog(downloadedBytes, totalBytes);
                        }
                    }

                    Log.d(TAG, "Download completed. Prompting user to install.");
                    if (newVersion != null) {
                        storeDownloadedVersion(newVersion);
                    }
                    promptUserToInstall(file);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error downloading update", e);
                dismissProgressDialog();
                context.getMainExecutor().execute(() ->
                        Toast.makeText(context, "Update download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void storeDownloadedVersion(String version) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_DOWNLOADED_VERSION, version).apply();
    }

    private void updateProgressDialog(long downloadedBytes, long totalBytes) {
        int progress = (int) ((downloadedBytes * 100) / totalBytes);
        context.getMainExecutor().execute(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                ProgressBar progressBar = progressDialog.findViewById(R.id.progressBar);
                TextView progressText = progressDialog.findViewById(R.id.progressText);
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                }
                if (progressText != null) {
                    String progressTxt = progress + "%";
                    progressText.setText(progressTxt);
                }
            }
        });
    }

    private void dismissProgressDialog() {
        context.getMainExecutor().execute(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }

    private void promptUserToInstall(File file) {
        context.getMainExecutor().execute(() -> {
            dismissProgressDialog();
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Update Downloaded")
                    .setMessage("Would you like to install the update now?")
                    .setPositiveButton("Install", (dialog, which) -> {
                        dialog.dismiss();
                        installUpdate(file);
                    })
                    .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void installUpdate(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (INSTALL_ACTION.equals(intent.getAction())) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                switch (status) {
                    case PackageInstaller.STATUS_SUCCESS:
                        Toast.makeText(context, "Update installed successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case PackageInstaller.STATUS_FAILURE:
                    case PackageInstaller.STATUS_FAILURE_ABORTED:
                    case PackageInstaller.STATUS_FAILURE_BLOCKED:
                    case PackageInstaller.STATUS_FAILURE_CONFLICT:
                    case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                    case PackageInstaller.STATUS_FAILURE_INVALID:
                    case PackageInstaller.STATUS_FAILURE_STORAGE:
                        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                        Toast.makeText(context, "Update failed: " + message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Update failed with message: " + message);
                        break;
                    default:
                        Toast.makeText(context, "Update failed with unknown error", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Update failed with unknown status: " + status);
                        break;
                }
            }
        }
    }
}