package com.chds.socialdistancingdetector;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PairActivity extends AppCompatActivity {

    // Declare the variables so that you will be able to reference it later.
    TextView deviceName;
    Button backButton, pairButton;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        deviceName = findViewById(R.id.device_name);
        backButton = findViewById(R.id.back_button);
        pairButton = findViewById(R.id.pair_button);

        name = getIntent().getExtras().getString("device");

        deviceName.setText(name);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}