package com.jon.cotgenerator.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jon.cotgenerator.service.CotService;

import timber.log.Timber;

/* Simple base class shared between all app activities, which allows them all to receive state updates from the service. */
abstract class ServiceBoundActivity
        extends AppCompatActivity
        implements CotService.StateListener {

    protected CotService service;

    protected ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            Timber.i("onServiceConnected");
            service = ((CotService.ServiceBinder) binder).getService();
            service.registerStateListener(ServiceBoundActivity.this);
            onStateChanged(service.getState(), null);
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            Timber.i("onServiceDisconnected");
            service.unregisterStateListener(ServiceBoundActivity.this);
            service = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Start the service and bind to it */
        Intent intent = new Intent(this, CotService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (service != null) {
            service.unregisterStateListener(this);
            service = null;
            if (serviceConnection != null) {
                unbindService(serviceConnection);
                serviceConnection = null;
            }
        }
    }
}