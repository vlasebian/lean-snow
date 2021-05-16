package com.smd.leansnow;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
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

public class ApplicationState {
    private static final String DATABASE_URL = "https://lean-snow-default-rtdb.europe-west1.firebasedatabase.app/";

    public static final String ENABLE_NOTIFICATIONS_SHARED_PREFERENCE = "enable_notifications_switch";

    public static final String NOTIFICATIONS_CHANNEL_ID = "ski.resorts.update.notification.channel.id";

    public static final int SKI_RESORTS_UPDATE_NOTIFICATION_ID = 0;

    NotificationManagerCompat notificationManager;

    SharedPreferences sharedPreferences;

    private static ApplicationState instance;

    private final Context applicationContext;

    private final HashMap<String, SkiResort> localSkiResortsHashMap = new HashMap<>();

    private ApplicationState(Context context) {
        this.applicationContext = context.getApplicationContext();
        setup();
    }

    public static ApplicationState getInstance(Context context) {
        if (instance == null) {
            instance = new ApplicationState(context);
        }

        return instance;
    }

    private void setup() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

        notificationManager = NotificationManagerCompat.from(applicationContext);

        createNotificationChannel();

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
//                    Toast.makeText(BaseActivity.this, skiResort.getName() + " is now " + skiResort.status.toString().toLowerCase() + ".", Toast.LENGTH_LONG).show();
                } else {
                    message = updatedSkiResortsHashMap.size() + " ski resorts status changed.";
//                    Toast.makeText(BaseActivity.this, updatedSkiResortsHashMap.size() + " ski resorts status changed.", Toast.LENGTH_LONG).show();
                }

                showNotification("LeanSnow", message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Timber.w("Failed to read value: %s", error.getMessage());

            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = applicationContext.getString(R.string.channel_name);
        String description = applicationContext.getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(NOTIFICATIONS_CHANNEL_ID, name, importance);
        channel.setDescription(description);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = applicationContext.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void showNotification(String title, String content) {
        // Only show notification if these are enabled by the user. By default they are shown.
        if (!sharedPreferences.getBoolean(ENABLE_NOTIFICATIONS_SHARED_PREFERENCE, true)) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(SKI_RESORTS_UPDATE_NOTIFICATION_ID, builder.build());
    }
}
