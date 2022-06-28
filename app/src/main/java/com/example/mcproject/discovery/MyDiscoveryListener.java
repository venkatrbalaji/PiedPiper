package com.example.mcproject.discovery;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;


public class MyDiscoveryListener implements  NsdManager.DiscoveryListener{

    private static final String TAG = "PPDiscovery";

    private static final String serviceName = "NsdChat";
    private static final String SERVICE_TYPE = "_nsdchat._tcp.";
    private final NsdManager.ResolveListener resolveListener;
    private NsdManager nsdManager;
//    public String discoveredPort, discoveredIP;
    File manifestFile = null;
    String manifestFileName = null;
    String selfIp = null;
    Semaphore semaphore = new Semaphore(1);
    Semaphore manifestWriteSemaphore = new Semaphore(1);

    public MyDiscoveryListener(NsdManager nsdManager, String manifestFileName, String selfIp) {
        this.nsdManager = nsdManager;
        Log.i(TAG, "Initializing Resolve LListener");
        this.resolveListener = initializeResolveListener();
        this.manifestFileName = manifestFileName;
        this.selfIp = selfIp;


    }

    // Called as soon as service discovery begins.
    @Override
    public void onDiscoveryStarted(String regType) {
        Log.d(TAG, "Service discovery started");
    }

    @Override
    public void onServiceFound(NsdServiceInfo service) {
        // A service was found! Do something with it.
        Log.d(TAG, "Service discovery success" + service);
//        if (!service.getServiceType().equals(SERVICE_TYPE)) {
//            // Service type is the string containing the protocol and
//            // transport layer for this service.
//            Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
//        }else if (service.getServiceName().contains("NsdChat")){
//            nsdManager.resolveService(service,resolveLis );
//        }
        (new Thread() {
            public void run() {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                nsdManager.resolveService(service, resolveListener);
            }
        }).start();
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service lost: " + service);
        semaphore.release();
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
        semaphore.release();
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        nsdManager.stopServiceDiscovery(this);
        semaphore.release();
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        nsdManager.stopServiceDiscovery(this);
        semaphore.release();
    }

    private NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
                semaphore.release();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Resolve Succeeded. " + serviceInfo);
                NsdServiceInfo mService = serviceInfo;
                int port = mService.getPort();
                String host = String.valueOf(mService.getHost());
//                ClientThread.start(host,port);
                Log.i(TAG, "Resolved Service details are: host: " + host+" port: "+port);

                //TODO: Write the discovered service details ot Manifest file
                // Create PP SystemFiles Directory
                File SDCardRoot = Environment.getExternalStorageDirectory();
                File ppSysDir = new File(SDCardRoot, "/PiedPiper/"); //create directory to keep your downloaded file
                if (!ppSysDir.exists()) {
                    ppSysDir.mkdir();
                }

                // Sample Manifest File Structure:
                // {
                //  "nwDeviceDetails": [{"ip":"192.168.0.12", "port":"8000"}, {} ... {}],
                //  "uploadDetails": {
                //      "file1": [{fragName:frag1_name, ip:192.168.0.12, port:8080}, ...{}],
                //      "file2": [{},...{}]
                //  }
                // }

                // Create Manifest File
                manifestFile = new File(ppSysDir, manifestFileName);
                JSONObject manifestObject = null;
                JSONArray nwDeviceDetails = null;
                JSONObject uploadDetails = null;
                if (!manifestFile.exists()){
                    Log.i(TAG, "File " + manifestFile.getName() + " does not exist. Creating a new one");
                    manifestObject = new JSONObject();
                    try {
                        nwDeviceDetails = new JSONArray(); // Value for key "nwDeviceDetails"
                        uploadDetails = new JSONObject(); // Value for key "uploadDetails"

                    }
                    catch (Exception e) {
                        Log.i(TAG, "Found Exception in Manifest File Creation: " + e);
                    }
                }
                else {

                    manifestObject = manifestFileReader();
                    try {
                        if (manifestObject != null) {
                            nwDeviceDetails = manifestObject.getJSONArray("nwDeviceDetails");
                            uploadDetails = manifestObject.getJSONObject("uploadDetails");

                            Log.i(TAG, "nwDeviceDetails: " + nwDeviceDetails);
                            Log.i(TAG, "uploadDetails: " + uploadDetails);

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // Add values to the above keys.
                //TODO: Check if the IP is already present or current Device IP
                try {
                    Boolean skipWrite = false;
                    if (host == selfIp) {
                        skipWrite = true;
                    }
                    if (nwDeviceDetails != null) {
                        int deviceCount = nwDeviceDetails.length();
                        for (int i = 0; i < deviceCount; i++) {
                            JSONObject currObj = (JSONObject) nwDeviceDetails.get(i);
                            String currIp = (String) currObj.get("ip");
                            Log.i(TAG, "Existing IP: "+ currIp + ", New IP: " + host +".");
                            if (currIp.equals(host)) {
                                skipWrite = true;
                            }
                        }
                    }

                    if (!skipWrite) {
                        // New Device details
                        JSONObject newDevice = new JSONObject();
                        newDevice.put("ip", host);
                        newDevice.put("port", String.valueOf(port));
                        nwDeviceDetails.put(newDevice);

                        manifestObject.put("nwDeviceDetails", nwDeviceDetails);
                        manifestObject.put("uploadDetails", uploadDetails);

                        manifestWriter(manifestObject);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                semaphore.release();
            }
        };
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

    public JSONObject manifestFileReader(){
        //Read from Manifest File

        JSONObject manifestObject = null;
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
}
