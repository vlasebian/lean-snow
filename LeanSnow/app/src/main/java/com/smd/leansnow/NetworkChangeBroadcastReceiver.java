package com.smd.leansnow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import timber.log.Timber;

import static com.smd.leansnow.BaseActivity.dialog;

public class NetworkChangeBroadcastReceiver extends BroadcastReceiver {
    NetworkChangeBroadcastReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try
        {
            if (isOnline(context)) {
                dialog(true);
                Timber.d("Connected to the internet.");
            } else {
                dialog(false);
                Timber.d("Cannot connect to the internet.");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            // Should check null,  because in airplane mode it will be null.
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

}