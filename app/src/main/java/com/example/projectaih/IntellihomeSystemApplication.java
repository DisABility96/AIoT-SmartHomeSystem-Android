package com.example.projectaih;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

public class IntellihomeSystemApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("IntellihomeSystemApplication", "Before Stetho Init");
        Stetho.initializeWithDefaults(this);
        Log.d("IntellihomeSystemApplication", "After Stetho Init");
    }
}
