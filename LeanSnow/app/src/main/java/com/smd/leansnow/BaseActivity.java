package com.smd.leansnow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Base activity for the application that contains common elements for all the activities: the
 * action bar and the network change broadcast receiver.
 */
public class BaseActivity extends AppCompatActivity {
    private BroadcastReceiver networkChangeReceiver;

    private Toolbar toolbar = null;

    static TextView checkConnectionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.plant(new Timber.DebugTree());
    }

    @Override
    public void setContentView(int layoutResID) {
        Timber.d("Setting content view.");

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_base, null);

        toolbar = coordinatorLayout.findViewById(R.id.my_actionbar);
        setSupportActionBar(toolbar);

        checkConnectionTextView = coordinatorLayout.findViewById(R.id.tv_check_connection);

        FrameLayout activityContainer = coordinatorLayout.findViewById(R.id.layout_container);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(coordinatorLayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User chose the "Settings" item, show the app settings UI.
        if (item.getItemId() == R.id.settings) {
            Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsActivityIntent);

            return true;
        }

        // User action was not recognized. Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    public static void dialog(boolean value) {
        if (value) {
            checkConnectionTextView.setText(R.string.network_change_receiver_conn_up);
            checkConnectionTextView.setBackgroundColor(Color.GREEN);
            checkConnectionTextView.setTextColor(Color.WHITE);

            Handler handler = new Handler();
            Runnable delayRunnable = new Runnable() {
                @Override
                public void run() {
                    checkConnectionTextView.setVisibility(View.GONE);
                }
            };
            handler.postDelayed(delayRunnable, 3000);
        } else {
            checkConnectionTextView.setVisibility(View.VISIBLE);
            checkConnectionTextView.setText(R.string.network_change_receiver_conn_down);
            checkConnectionTextView.setBackgroundColor(Color.RED);
            checkConnectionTextView.setTextColor(Color.WHITE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}