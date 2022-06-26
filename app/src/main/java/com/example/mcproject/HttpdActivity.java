package com.example.mcproject;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoFileUpload;
import fi.iki.elonen.NanoHTTPD;

public class HttpdActivity extends AppCompatActivity {

    private WebServer server;

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    Log.d("Info", "Received Uri: "+uri.getPath());
                    Toast.makeText(getApplicationContext(), "Selected File for Upload"+uri.getPath(), Toast.LENGTH_LONG).show();
                    // TODO:
                    //  1. Split File into fragments and Get the list of target device IPs to upload
                    //  2. Upload and store the fragment names in local META File
//                    MainActivity.UploadTask up1 = new HttpdActivity().UploadTask(uri);
                    Toast.makeText(getApplicationContext(),"Starting to Upload",Toast.LENGTH_LONG).show();
//                    up1.execute();
                    server = new WebServer();
                    try {
                        server.start();
                    } catch(IOException ioe) {
                        Log.w("Httpd", "The server could not start.");
                    }
                    Log.w("Httpd", "Web server initialized.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_httpd);

        findViewById(R.id.httpdButton).setOnClickListener((view)->{

//            mGetContent.launch("image/*");
            server = new WebServer();
            try {
                server.start();
            } catch(IOException ioe) {
                Log.w("Httpd", "The server could not start.");
            }
            Log.w("Httpd", "Web server initialized.");

        });
    }

    // DON'T FORGET to stop the server
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    private class WebServer extends NanoHTTPD {

        String TAG = "WebServer";

        public WebServer()
        {
            super(8080);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Log.i(TAG, "Session: " + session.getQueryParameterString()); // Gives url parameter as a string
            String api = session.getUri().toLowerCase(); // Denotes the REST Endpoint like'/upload', '/download', '/', etc.
            // /uplaod endpoint
            if (api.contains("upload")) {
                // Code to handle Upload from Client goes here.
                //  This Server Code will download the file.
                InputStream input = session.getInputStream();
                File SDCardRoot = Environment.getExternalStorageDirectory(); // location where you want to store
                File directory = new File(SDCardRoot, "/Uploads/"); //create directory to keep your downloaded file
                if (!directory.exists()) {
                    directory.mkdir();
                }
                //input = url.openStream();
                OutputStream output = null;
                try {
                    output = new FileOutputStream(new File(directory, "UploadedFile.txt"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                        output.write(buffer, 0, bytesRead);

                    }
                    output.close();
                    //Toast.makeText(getApplicationContext(),"Read Done", Toast.LENGTH_LONG).show();
                } catch (Exception exception) {


                    //Toast.makeText(getApplicationContext(),"output exception in catch....."+ exception + "", Toast.LENGTH_LONG).show();
                    Log.d("Error", String.valueOf(exception));
//                publishProgress(String.valueOf(exception));
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT,
                        "Uploaded files Successfully");
            }
            else if(api.contains("download")){
                // TODO: Code to handle request for download from Client goes here.
                //  This Server Code will upload the file.
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "No Action for " + api + " API Yet.");
            }
            else{
                // TODO: Landing Page '/' action goes here
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "No Action for " + api + " API.");
            }

        }
    }
}