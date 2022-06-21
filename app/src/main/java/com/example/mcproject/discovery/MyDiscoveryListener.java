package com.example.mcproject.discovery;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;


public class MyDiscoveryListener implements  NsdManager.DiscoveryListener{

    private static final String TAG = "PPDiscovery";

    private static final String serviceName = "NsdChat";
    private static final String SERVICE_TYPE = "_nsdchat._tcp.";
    private final NsdManager.ResolveListener resolveListener;
    private NsdManager nsdManager;

    public MyDiscoveryListener(NsdManager nsdManager) {
        this.nsdManager = nsdManager;
        this.resolveListener = initializeResolveListener();
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
        nsdManager.resolveService(service, resolveListener);
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service lost: " + service);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        nsdManager.stopServiceDiscovery(this);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        nsdManager.stopServiceDiscovery(this);
    }

    private NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Resolve Succeeded. " + serviceInfo);

                NsdServiceInfo mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();

                Log.i(TAG, "Resolved Service details are: host: " + host+" port: "+port);
                //TODO here start a socket connection to the host
            }
        };
    }
}
