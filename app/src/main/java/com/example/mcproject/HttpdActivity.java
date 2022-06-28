package com.example.mcproject;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

//import org.json.simple.JSONObject;

import com.example.mcproject.discovery.MyDiscoveryListener;
import com.example.mcproject.discovery.MyRegistrationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import fi.iki.elonen.NanoHTTPD;

public class HttpdActivity extends AppCompatActivity {

    private WebServer server;
    String status_message = "Do nothing";
    String uploadDirectoryName = "fragmentUploads";
    String downloadDirectoryName = "fragmentDownloads";
    File manifestFile = null;
    String manifestFileName = "PiedPiper_Manifest.txt";
    File SDCardRoot = Environment.getExternalStorageDirectory();
    File ppSysDir = new File(SDCardRoot, "/PiedPiper/");
    String selfIP = null;
    String selfPort = "8080";

    private static final String TAG = "HttpdActivity";
//    private static final String serviceName = "NsdChat_";
//    private static final String SERVICE_TYPE = "_nsdchat._tcp.";
    protected String serviceName = "NsdChat_";
    private static final String SERVICE_TYPE = "_nsdchat._tcp.";
    NsdManager nsdManager;
    NsdManager.RegistrationListener registrationListener;
    NsdManager.DiscoveryListener discoveryListener;
    Semaphore manifestWriteSemaphore = new Semaphore(1);

    private volatile boolean isDiscoveryRunning = false;

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
                    Toast.makeText(getApplicationContext(),"Starting to Upload",Toast.LENGTH_LONG).show();

                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_httpd);
        String TAG = "HttpdActivity";

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        selfIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.i(TAG, "Current Device IP: "+ selfIP);
        serviceName = serviceName + selfIP;

        server = new WebServer();
        try {
            server.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");


        // NSD
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
//        startDiscovery();
        stopDiscovery(); //stop discovery on this device before starting service on this device

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(Integer.parseInt(selfPort));
        if(registrationListener==null) {
            registrationListener = new MyRegistrationListener();
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        Log.i(TAG,"Service Start Triggered");
        startDiscovery();


        // Button Actions
        findViewById(R.id.upldButton).setOnClickListener((view)->{

//            mGetContent.launch("image/*");
            //TODO: Upload Action
            // 1. Read possible client IPs from Manifest file (Static for now) - Done
            // 2. Split the File into fragments (To be included by SG)
            // 3. Hit the '/upload' endpoints of the clients
            // 4. Write the fragment name and client IP to the Manifest file

            JSONObject manifestObject = manifestFileReader();
            try {
                if (manifestObject != null) {
                    JSONArray nwDeviceDetails = manifestObject.getJSONArray("nwDeviceDetails");

                    JSONObject uploadDetails = manifestObject.getJSONObject("uploadDetails");

                    Log.i(TAG, "nwDeviceDetails: " + nwDeviceDetails);
                    Log.i(TAG, "uploadDetails: " + uploadDetails);

                    // TODO: Integrate Fragmentation here
                    String fileToUpload = "file1";
                    List<String> fragmentsToUpload = Arrays.asList("file3_1.txt", "file3_2.txt");
                    int fragCount = fragmentsToUpload.size();
                    int deviceCount = nwDeviceDetails.length();

                    for (int i=0; i<fragCount; i++){
                        JSONObject fragObj = nwDeviceDetails.getJSONObject(i%deviceCount); // Round Robin client picking. Can be based on proximity.
                        String fragName = fragmentsToUpload.get(i);
                        String clientIP = (String) fragObj.get("ip");
                        String clientPort = (String) fragObj.get("port");
                        String uploadURL = "http//"+clientIP+":"+clientPort+"/upload";

                        Log.i(TAG, "Upload URL:" + uploadURL);

                        //TODO: Uncomment below line when ready to deploy.
                        // Additional checks for successful upload and retry on Failure can be included

                        // uploadFiles(fragName, clientIP, clientPort);

                        // TODO: Write the details to Manifest File
                        // Add values to the read-in keys.
                        try {
                            JSONArray currFileFrags = null;
                            if (uploadDetails.has(fileToUpload)){
                                currFileFrags = (JSONArray) uploadDetails.get(fileToUpload);
                            }
                            else {
                                currFileFrags = new JSONArray();
                            }
                            JSONObject file1Frag1 = new JSONObject();
                            file1Frag1.put("fragName", fragName);
                            file1Frag1.put("ip", clientIP);
                            file1Frag1.put("port", clientPort);
                            currFileFrags.put(file1Frag1);
                            // TODO: Account for appending to manifest uploadDetails per file
                            uploadDetails.put(fileToUpload, currFileFrags);

                            manifestObject.put("nwDeviceDetails", nwDeviceDetails);
                            manifestObject.put("uploadDetails", uploadDetails);

                            manifestWriter(manifestObject);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.dwnldButton).setOnClickListener((view)->{

//            mGetContent.launch("image/*");
            //TODO: Download Action
            // 1. Get the list of fragment names and their respective client (Where the fragments are stored) IPs - Done
            // 2. Hit the '/download' endpoints of the client IPs with respective file content, fragments names, current device Ip, and port as parameters.
            // 3. The last step would have automatically hit the '/upload' endpoint of current device, which will download the fragments
            // 4. Stitch the fragments into single file (To be included by SG)

            String downloadFileName = "file1";

            JSONObject manifestObject = manifestFileReader();
            try {
                if (manifestObject != null) {
                    JSONArray nwDeviceDetails = manifestObject.getJSONArray("nwDeviceDetails");

                    JSONObject uploadDetails = manifestObject.getJSONObject("uploadDetails");

                    Log.i(TAG, "nwDeviceDetails: " + nwDeviceDetails);
                    Log.i(TAG, "uploadDetails: " + uploadDetails);

                    JSONArray downloadFileFrags = (JSONArray) uploadDetails.get(downloadFileName);

                    int fragCount = downloadFileFrags.length();

                    for (int i=0; i<fragCount; i++){
                        JSONObject fragObj = downloadFileFrags.getJSONObject(i);
                        String fragName = (String) fragObj.get("fragName");
                        String clientIP = (String) fragObj.get("ip");
                        String clientPort = (String) fragObj.get("port");
                        String downloadURL = "http//"+clientIP+":"+clientPort+"/download?filename="+fragName+"&ip="+selfIP+"&port="+selfPort;

                        Log.i(TAG, "Download URL:" + downloadURL);
                        // TODO: Hit the download url

                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }


        });

    }

//    public String encodeBase64(byte [] encodeMe){
//        byte[] encodedBytes = Base64.getEncoder().encode(encodeMe);
//        return new String(encodedBytes) ;
//    }
//
//    public byte[]decodeBase64(String encodedData){
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedData.getBytes());
//        return decodedBytes ;
//    }

    public JSONObject manifestFileReader(){
        //TODO: Read from Manifest File
//            JSONParser jsonParser = new JSONParser();

        JSONObject manifestObject = null;
        manifestFile = new File(ppSysDir, manifestFileName);
        Log.i(TAG, "File " + manifestFile.getName() + " exists. Reading it.");

        try (FileReader reader = new FileReader(manifestFile))
        {
            StringBuilder manifestContent = new StringBuilder("");

            char[] buffer = new char[1024];
            int charsRead = 0;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) >= 0) {
                manifestContent.append(buffer);
            }

            Log.i(TAG, "Read Content: " + String.valueOf(manifestContent));
            manifestObject = new JSONObject(String.valueOf(manifestContent));

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return manifestObject;
    }


    public void manifestWriter(JSONObject manifestObject){
        //Write JSON file
        (new Thread() {
            public void run() {
                Log.i(TAG, "Waiting to write Manifest File");
                try (FileWriter file = new FileWriter(manifestFile)) {
                    manifestWriteSemaphore.acquire();
                    Log.i(TAG, "Writing to Manifest File");
                    file.write(String.valueOf(manifestObject));
                    file.write("\r\n");
                    file.flush();

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                manifestWriteSemaphore.release();
            }}).start();
    }


    public void uploadFiles(String fragmentName, String client_ip, String port){

        String TAG = "uploadFile";
        String url = "http://" + client_ip + ":" + port + "/upload?filename=" + fragmentName;
        String charset = "UTF-8";
        File fragmentFile = new File(Environment.getExternalStorageDirectory()+"/" + uploadDirectoryName + "/"+fragmentName);
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        URLConnection connection;

        try {
            Log.i(TAG, "Opening URL connection on: " + url);
            connection = new URL(url).openConnection();
            connection.setDoOutput(true);
//                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (
                    OutputStream output = connection.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
            ) {
                FileInputStream vf = new FileInputStream(fragmentFile);
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;
                    Log.i(TAG, "Writing to OutputStream");
                    while ((bytesRead = vf.read(buffer, 0, buffer.length)) >= 0) {
                        output.write(buffer, 0, bytesRead);
                        Log.i(TAG, "Bytes Read: " + bytesRead);

                    }
                    //   output.close();
                    //Toast.makeText(getApplicationContext(),"Read Done", Toast.LENGTH_LONG).show();
                } catch (Exception exception) {


                    //Toast.makeText(getApplicationContext(),"output exception in catch....."+ exception + "", Toast.LENGTH_LONG).show();
                    Log.d("Error", String.valueOf(exception));
                    // output.close();

                }

                output.flush(); // Important before continuing with writer!
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.


                // End of multipart/form-data.
//                        writer.append("--" + boundary + "--").append(CRLF).flush();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d("Error", String.valueOf(e));
            }
            // Request is lazily fired whenever you need to obtain information about response.
            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            System.out.println(responseCode); // Should be 200

        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d("Error", String.valueOf(e));
        }
    }

    // DON'T FORGET to stop the server
    @Override
    public void onDestroy()
    {
        tearDown();
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    // NsdHelper's tearDown method
    public void tearDown() {
        nsdManager.unregisterService(registrationListener);
        stopDiscovery();
    }

    private void startDiscovery(){
        if(isDiscoveryRunning){
            Log.i(TAG,"Discovery is already running, not starting again");
            return;
        }
        if(discoveryListener==null){
            discoveryListener = new MyDiscoveryListener(nsdManager, manifestFileName, selfIP);
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        isDiscoveryRunning = true;
        Log.i(TAG,"Service Discovery Triggered");
    }

    private void stopDiscovery(){
        if (isDiscoveryRunning) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            isDiscoveryRunning = false;
        }
    }

    // WEB Server
    private class WebServer extends NanoHTTPD {

        String TAG = "WebServer";

        public WebServer()
        {
            super(Integer.parseInt(selfPort));
        }

        @Override
        public Response serve(IHTTPSession session) {
            Log.i(TAG, "Session: " + session.getParms()); // Gives url parameter as a string
//            Map<String, String> urlParameters = session.getParms();
//            Log.i(TAG, "Given Filename: " + urlParameters.get("filename"));
            String api = session.getUri().toLowerCase(); // Denotes the REST Endpoint like'/upload', '/download', '/', etc.

            if (api.contains("status")){
                // Status message
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT,
                        status_message);
            }
            else if (api.contains("upload")) {
                // upload endpoint
                // Code to handle Upload from Client goes here.
                //  This Server Code will download the file.
                InputStream input = session.getInputStream();
                Map<String, String> urlParameters = session.getParms();
                String fragmentName = urlParameters.get("filename");
                Log.i(TAG, "Downloading File: " + fragmentName);
                File SDCardRoot = Environment.getExternalStorageDirectory(); // location where you want to store
                File directory = new File(SDCardRoot, "/" + downloadDirectoryName + "/"); //create directory to keep your downloaded file
                if (!directory.exists()) {
                    directory.mkdir();
                }
                //input = url.openStream();
                OutputStream output = null;
                try {
                    output = new FileOutputStream(new File(directory, fragmentName));
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
                // Hit the '/upload' API of the client IP with the file to upload

                Map<String, String> urlParameters = session.getParms();
                Log.i(TAG, "Given Filename: " + urlParameters.get("filename"));
                String fragmentName = urlParameters.get("filename");
                String client_ip = urlParameters.get("ip"); // To be picked from manifest file
                String port = urlParameters.get("port"); // To be picked from manifest file

                uploadFiles(fragmentName, client_ip, port);

                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT,
                        "File Sent Successfully");
            }
            else{
                // TODO: Landing Page '/' action goes here
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "No Action for " + api + " API.");
            }

        }
    }
}