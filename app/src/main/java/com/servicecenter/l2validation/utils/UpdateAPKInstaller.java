package com.servicecenter.l2validation.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import io.esper.devicesdk.EsperDeviceSDK;


public class UpdateAPKInstaller extends AsyncTask<String, String, String> {

    ProgressDialog progressDialog;
    int status = 0;

    private Context context;
    private EsperDeviceSDK sdk;
    private Boolean esperSDKActivated;

    public void setContext(Context context, ProgressDialog progress, EsperDeviceSDK sdk, Boolean esperSDKActivated) {
        this.context = context;
        this.progressDialog = progress;
        this.sdk = sdk;
        this.esperSDKActivated = esperSDKActivated;
    }

    public void onPreExecute() {
        progressDialog.show();

    }

  /*  protected void onProgressUpdate(String... progress) {
        // setting progress percentage
        progressDialog.setProgress(Integer.parseInt(progress[0]));
    }*/

    @Override
    protected String doInBackground(String... arg0) {
        int count;

        try {
            URL url = new URL(arg0[0]);
            URLConnection conexion = url.openConnection();
            conexion.connect();
            int lenghtOfFile = conexion.getContentLength();
            Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);
            InputStream input = new BufferedInputStream(url.openStream());
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
            boolean isCreate = file.mkdirs();
            File outputFile = new File(file, "serviceCentre.apk");
            if (outputFile.exists()) {
                boolean isDelete = outputFile.delete();
            }
            OutputStream output = new FileOutputStream(outputFile);
            byte[] data = new byte[1024];
            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
            //   installApp();

            File sdcard = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/serviceCentre.apk");
            Uri fileProvider = FileProvider.getUriForFile(context, "com.servicecenter.l2validation.fileprovider", sdcard);
            //  Uri fileProvider = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, sdcard);
            if (esperSDKActivated) {
                if (sdk != null) {

                    sdk.installApp(context.getPackageName(), sdcard.getPath(), new EsperDeviceSDK.Callback<Boolean>() {
                        @Override
                        public void onResponse(Boolean esperApkInstall) {

                        }

                        @Override
                        public void onFailure(Throwable t) {
                            //  showFailureResult(t);
                            Log.d("check_failure", t.toString());
                        }
                    });
                } else {
                    Log.d("check_failure", "SDK not activated");
                }
            } else {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    install.setData(fileProvider);
                    context.startActivity(install);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileProvider, "application/vnd.android.package-archive");
                    List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        context.grantUriPermission(context.getApplicationContext().getPackageName() + ".fileprovider", fileProvider, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            context.startActivity(intent);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
                    context.startActivity(intent);

                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    protected void onProgressUpdate(String... progress) {
        Log.d("ANDRO_ASYNC", progress[0]);
        progressDialog.setProgress(Integer.parseInt(progress[0]));
    }

    @Override
    protected void onPostExecute(String unused) {
        progressDialog.dismiss();
    }
}
