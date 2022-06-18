package com.example.mc_project;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private final static int PICKFILE_RESULT_CODE=1;
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
                    MainActivity.UploadTask up1 = new MainActivity.UploadTask(uri);
                    Toast.makeText(getApplicationContext(),"Starting to Upload",Toast.LENGTH_LONG).show();
                    up1.execute();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button upldButton = (Button)findViewById(R.id.uploadButton);
        Button dwnldButton = (Button)findViewById(R.id.downloadButton);
        Button listButton = (Button)findViewById(R.id.listButton);

        upldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("image/*");

            }
        });

        dwnldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mGetContent.launch("image/*");
                // TODO:
                //  1. Get the list of device IPs to download file fragments from (via broadcast of fragment names from Meta FILE)
                //  2. Download the fragments
                //  3. Stitch the fragments together and store in local device
            }
        });

        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mGetContent.launch("image/*");
                // TODO:
                //  1. Display the uploaded file list from local meta file
                //  META File will contain uploaded file fragment names and stored file fragment names

            }
        });
    }


    // UPLOAD Thread
    public class UploadTask extends AsyncTask<String, String, String> {

        Uri recDirectory;
        protected UploadTask(Uri recDirectory){
            this.recDirectory = recDirectory;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Starting to execute Background Upload Task", Toast.LENGTH_LONG).show();
        }

//        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(String... strings) {
            try {

//                String uploadUrl = "http://"+backendBaseUrl+"/upload/";
                String uploadUrl = "";
                String charset = "UTF-8";
                String group_id = "9";
                String ASUid = "1200072576";
                String accept = "1";


//                File videos[] = recDirectory.listFiles();
//                File videos[] = (new File(recDirectory.getPath())).listFiles();
//                Log.d("Files", "Size: "+ videos.length);
//                for (int i=0; i < videos.length; i++)
//                {
//                    Log.d("Files", "FileName:" + videos[i].getName());

//                    File videoFile = videos[i];
                    File videoFile = new File(recDirectory.getPath());
                    String url = uploadUrl + videoFile.getName();
                    String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
                    String CRLF = "\r\n"; // Line separator required by multipart/form-data.

                    URLConnection connection;

                    Log.d("info", "UploadFileName: " + videoFile);

                    connection = new URL(url).openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    Log.i("info", "Upload connection");
                    connection.connect();

                    try (
                            OutputStream output = connection.getOutputStream();
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
                    ) {
                        Log.i("info", "getOutputStream");
                        Base64OutputStream encodedOutput = new Base64OutputStream(output, 0);
                        // Send normal accept.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"accept\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                        writer.append(CRLF).append(accept).append(CRLF).flush();

                        writer.append("Content-Disposition: form-data; name=\"filename\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                        writer.append(CRLF).append(videoFile.getName()).append(CRLF).flush();

                        // Send normal accept.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"id\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                        writer.append(CRLF).append(ASUid).append(CRLF).flush();

                        // Send normal accept.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"group_id\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                        writer.append(CRLF).append(group_id).append(CRLF).flush();


                        // Send video file.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"file\"").append(CRLF);
                        writer.append("Content-Type: video/mp4; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                        writer.append(CRLF).flush();
                        Log.i("info", "FileName: " + videoFile.getName());
                        FileInputStream vf = new FileInputStream(videoFile);
                        try {
                            byte[] buffer = new byte[1024];
                            int bytesRead = 0;
                            while ((bytesRead = vf.read(buffer, 0, buffer.length)) >= 0) {
                                Log.i("info", "Writing to Output Stream");
                                encodedOutput.write(buffer, 0, bytesRead);

                            }
                        } catch (Exception exception) {
                            //Toast.makeText(getApplicationContext(),"output exception in catch....."+ exception + "", Toast.LENGTH_LONG).show();
                            Log.d("Error", String.valueOf(exception));
                            publishProgress(String.valueOf(exception));
                            // output.close();
                        }

                        output.flush(); // Important before continuing with writer!
                        writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

                        // End of multipart/form-data.
                        writer.append("--" + boundary + "--").append(CRLF).flush();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Request is lazily fired whenever you need to obtain information about response.
                    int responseCode = ((HttpURLConnection) connection).getResponseCode();
                    System.out.println(responseCode); // Should be 200
                } catch (MalformedURLException malformedURLException) {
                malformedURLException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onProgressUpdate(String... text) {
            Toast.makeText(getApplicationContext(), "In Background Task " + text[0], Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(String text){

            Intent openhomepage = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(openhomepage);
        }
    }
}