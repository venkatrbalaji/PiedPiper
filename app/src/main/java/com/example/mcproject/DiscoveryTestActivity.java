package com.example.mcproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;

import com.example.mcproject.discovery.MyDiscoveryListener;
import com.example.mcproject.discovery.MyRegistrationListener;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;


public class DiscoveryTestActivity extends AppCompatActivity {
    private static final String TAG = "PPDiscovery";

    private static final String serviceName = "NsdChat";
    private static final String SERVICE_TYPE = "_nsdchat._tcp.";

    NsdManager nsdManager;
    NsdManager.RegistrationListener registrationListener;
    NsdManager.DiscoveryListener discoveryListener;

    private volatile boolean isDiscoveryRunning = false;

//    ArrayList<Client>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery_test);


        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        startDiscovery();
        //START SERVICE BUTTON
        findViewById(R.id.button).setOnClickListener((view)->{
            stopDiscovery(); //stop discovery on this device before starting service on this device

            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(serviceName);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(8081);
            if(registrationListener==null) {
                registrationListener = new MyRegistrationListener();
            }
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            Log.i(TAG,"Service Start Triggered");
        });

        //START DISCOVERY BUTTON
        findViewById(R.id.button2).setOnClickListener((view)->{
            startDiscovery();
        });
    }

    @Override
    protected void onDestroy() {
        tearDown();
//        connection.tearDown();
        super.onDestroy();
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
            discoveryListener = new MyDiscoveryListener(nsdManager);
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        isDiscoveryRunning = true;
        Log.i(TAG,"Service Discovery Triggered");
    }

    private void stopDiscovery(){
        nsdManager.stopServiceDiscovery(discoveryListener);
        isDiscoveryRunning = false;
    }

    public class Clients {
        Socket socket;
        String name;
        PrintWriter writer;
        public Clients(Socket socket, String name, PrintWriter writer){
            this.socket = socket;
            this.name = name;
            this.writer = writer;
        }
    }

}