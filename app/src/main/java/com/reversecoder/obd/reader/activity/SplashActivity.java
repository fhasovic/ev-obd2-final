package com.reversecoder.obd.reader.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.reversecoder.obd.reader.R;
import com.reversecoder.permission.activity.PermissionListActivity;

public class SplashActivity extends AppCompatActivity {

    SplashCountDownTimer splashCountDownTimer;
    private final long startTime = 4 * 1000;
    private final long interval = 1 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ((TextView) findViewById(R.id.application_version)).setText("Version: " + getString(R.string.app_version_name));

        splashCountDownTimer = new SplashCountDownTimer(startTime, interval);
        splashCountDownTimer.start();
    }

    public class SplashCountDownTimer extends CountDownTimer {
        public SplashCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(SplashActivity.this, PermissionListActivity.class);
                startActivityForResult(intent, PermissionListActivity.REQUEST_CODE_PERMISSIONS);
            } else {
                navigateMainActivity();
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }

    private void navigateMainActivity() {
        Intent intent;
//        if (SessionManager.getBooleanSetting(SplashActivity.this, IS_USER_LOGGED_IN, false)) {
//            intent = new Intent(SplashActivity.this, HomeActivity.class);
//        } else {
//            intent = new Intent(SplashActivity.this, LoginActivity.class);
//        }
        intent = new Intent(SplashActivity.this, ObdReaderActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PermissionListActivity.REQUEST_CODE_PERMISSIONS) {
            if (resultCode == RESULT_OK) {
                navigateMainActivity();
            } else if (resultCode == RESULT_CANCELED) {
                finish();
            }
        }
    }
}
