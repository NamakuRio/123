package com.inf.os.fileup;

/*
 * Os-FileUp is an Android Open Source Project hosted on GitHub.
 * Developed by Ghazi Khan (https://github.com/mgks) under MIT Open Source License.
 * This program is free to use for private and commercial purposes.
 * It will be generous to mention project source or developer in your Application's License(s) Wiki.
 * Giving right credit to developers encourages them to create better projects :)
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity{
    WebView webView;
    SwipeRefreshLayout swipeRefreshLayout;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;
    ProgressDialog dialog;
    RelativeLayout rl_noConnection;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //Check if response is positive
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null || intent.getData() == null){
                        //Capture Photo if no image available
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
//        }

        webView = (WebView) findViewById(R.id.ifView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.ifSWLayout);
        rl_noConnection = (RelativeLayout) findViewById(R.id.rl_noConnection);
        assert webView != null;

        if(isNetworkConnected()){
            rl_noConnection.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            loadWebsite();
        } else {
            webView.setVisibility(View.GONE);
            rl_noConnection.setVisibility(View.VISIBLE);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Tidak Ada Koneksi")
                    .setMessage("Mohon hidupkan data Anda untuk melanjutkan")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).show();
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed,R.color.colorAccent,R.color.colorMaroon);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(isNetworkConnected()){
                    rl_noConnection.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.reload();
                } else {
                    webView.setVisibility(View.GONE);
                    rl_noConnection.setVisibility(View.VISIBLE);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Tidak Ada Koneksi")
                            .setMessage("Mohon hidupkan data Anda untuk melanjutkan")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            }).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

    }
    public class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), "Failed loading app!", Toast.LENGTH_SHORT).show();
        }
    }
    // Create an image file
    private File createImageFile() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK:
                    if(webView.canGoBack()){
                        webView.goBack();
                    }else{
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    private void loadWebsite(){
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        if(Build.VERSION.SDK_INT >= 21){
            webSettings.setMixedContentMode(0);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT >= 19){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webView.setWebViewClient(new Callback());
        webView.loadUrl("https://instagrampanel.co.id/");
        webView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                swipeRefreshLayout.setRefreshing(true);
            }

            //For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg){
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i,"File Chooser"), FCR);
            }
            // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this
            public void openFileChooser(ValueCallback uploadMsg, String acceptType){
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FCR);
            }
            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), MainActivity.FCR);
            }
            //For Android 5.0+
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams){
                if(mUMA != null){
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                    File photoFile = null;
                    try{
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    }catch(IOException ex){
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if(photoFile != null){
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    }else{
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                Intent[] intentArray;
                if(takePictureIntent != null){
                    intentArray = new Intent[]{takePictureIntent};
                }else{
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
