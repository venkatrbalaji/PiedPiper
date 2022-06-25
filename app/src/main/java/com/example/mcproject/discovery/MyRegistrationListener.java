package com.example.mcproject.discovery;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class MyRegistrationListener implements NsdManager.RegistrationListener {

    private static final String TAG = "PPRegistration";

    @Override
    public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
        // Save the service name. Android may have changed it in order to
        // resolve a conflict, so update the name you initially requested
        // with the name Android actually used.
        String serviceName = NsdServiceInfo.getServiceName();
        Log.i(TAG, "Service Registered serviceName: " + serviceName);
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        // Registration failed! Put debugging code here to determine why.
        Log.e(TAG, "Registration failed! " + errorCode);
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo arg0) {
        // Service has been unregistered. This only happens when you call
        // NsdManager.unregisterService() and pass in this listener.
        Log.i(TAG, "Service has been unregistered:  "+arg0);
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        // Unregistration failed. Put debugging code here to determine why.
        Log.e(TAG, "Unregistration failed! " + errorCode);
    }

}
