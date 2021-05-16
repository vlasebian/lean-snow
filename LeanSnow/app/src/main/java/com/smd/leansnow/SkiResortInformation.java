package com.smd.leansnow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SkiResortInformation extends AppCompatActivity {
    private com.smd.leansnow.databinding.ActivitySkiResortInformationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create bindings for the UI elements.
        binding = com.smd.leansnow.databinding.ActivitySkiResortInformationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Intent launchingIntent = getIntent();
        String skiResortName = launchingIntent.getStringExtra("skiResortName");

        binding.skiResortNameTextView.setText(skiResortName);
    }
}