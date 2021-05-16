package com.smd.leansnow;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import timber.log.Timber;

public class App extends Application {
    public static final String DATABASE_URL = "https://lean-snow-default-rtdb.europe-west1.firebasedatabase.app/";

    public static final String ENABLE_NOTIFICATIONS_SHARED_PREFERENCE = "enable_notifications_switch";

    public static final String NOTIFICATIONS_CHANNEL_ID = "ski_resorts_update_notification_channel_id";

    public static final int SKI_RESORTS_UPDATE_NOTIFICATION_ID = 0;

    HashMap<String, SkiResort> localSkiResortsHashMap = new HashMap<>();

    NotificationManagerCompat notificationManager;

    SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        notificationManager = NotificationManagerCompat.from(this);

        createNotificationChannel();

        networkChangeReceiver = new NetworkChangeBroadcastReceiver();
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);

        DatabaseReference myRef = database.getReference("ski_resorts/");

        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                GenericTypeIndicator<HashMap<String, SkiResort>> t = new GenericTypeIndicator<HashMap<String, SkiResort>>() {};
                HashMap<String, SkiResort> cloudSkiResortsHashMap = dataSnapshot.getValue(t);
                if (cloudSkiResortsHashMap == null || cloudSkiResortsHashMap.isEmpty()) {
                    return;
                }
                Timber.d(dataSnapshot.getValue().toString());

                HashMap<String, SkiResort> updatedSkiResortsHashMap = new HashMap<>();
                for (String key: cloudSkiResortsHashMap.keySet()) {
                    SkiResort cloudSkiResort = cloudSkiResortsHashMap.get(key);
                    SkiResort localSkiResort = localSkiResortsHashMap.getOrDefault(key, null);

                    if (localSkiResort == null || !localSkiResort.equals(cloudSkiResort)) {
                        localSkiResortsHashMap.put(key, cloudSkiResort);
                        updatedSkiResortsHashMap.put(key, cloudSkiResort);
                    }
                }

                if (updatedSkiResortsHashMap.size() == 0) {
                    return;
                }

                String message;
                if (updatedSkiResortsHashMap.size() == 1) {
                    String skiResortKey = (String) updatedSkiResortsHashMap.keySet().toArray()[0];
                    SkiResort skiResort = updatedSkiResortsHashMap.get(skiResortKey);
                    message = skiResort.getName() + " is now " + skiResort.status.toString().toLowerCase() + ".";
                } else {
                    message = updatedSkiResortsHashMap.size() + " ski resorts status changed.";
                }
                showNotification(message);
            }

            @Override
            public void onCancelled(@NotNull DatabaseError error) {
                // Failed to read value
                Timber.w("Failed to read value: %s", error.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(NOTIFICATIONS_CHANNEL_ID, name, importance);
        channel.setDescription(description);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void showNotification(String content) {
        // Only show notification if these are enabled by the user. By default they are shown.
        if (!sharedPreferences.getBoolean(ENABLE_NOTIFICATIONS_SHARED_PREFERENCE, true)) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("LeanSnow")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(SKI_RESORTS_UPDATE_NOTIFICATION_ID, builder.build());
    }

}
