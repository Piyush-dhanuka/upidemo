package com.example.upidemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            PrefManager pref = new PrefManager(this);
            // If user is already logged in (has a session), we still want them to enter PIN for security
            // Or if they logged out, they need to sign in again.
            // In both cases, LoginActivity is now the universal entry point.
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 2000);
    }
}
