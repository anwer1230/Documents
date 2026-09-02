package org.telegram.messenger;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

/**
 * ApplicationLoader - Base context and global configuration holder.
 */
public class ApplicationLoader extends Application {
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
    }
}
