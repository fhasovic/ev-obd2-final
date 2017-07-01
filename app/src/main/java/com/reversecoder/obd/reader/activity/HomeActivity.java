package com.reversecoder.obd.reader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.reversecoder.obd.reader.R;

public class HomeActivity extends AppCompatActivity {

    RelativeLayout relativeLayoutItemDashboard, relativeLayoutItemMap, relativeLayoutItemSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initLoginUI();
        initLoginAction();
    }

    private void initLoginUI() {
        relativeLayoutItemDashboard = (RelativeLayout) findViewById(R.id.item_dashboard);
        relativeLayoutItemMap = (RelativeLayout) findViewById(R.id.item_map);
        relativeLayoutItemSettings = (RelativeLayout) findViewById(R.id.item_settings);
    }

    private void initLoginAction() {
        relativeLayoutItemDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ObdReaderActivity.class));
            }
        });

        relativeLayoutItemMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MapActivity.class));
            }
        });

        relativeLayoutItemSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(HomeActivity.this, ConfigActivity.class));
            }
        });
    }
}
