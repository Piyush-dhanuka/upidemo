package com.example.upidemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class SignupActivity extends AppCompatActivity {
    private TextInputEditText nameInput, pinInput, phoneInput;
    private Button signupBtn;
    private TextView loginLink;
    private FirebaseFirestore db;
    private PrefManager pref;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);

        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        pinInput = findViewById(R.id.pinInput);
        signupBtn = findViewById(R.id.signupBtn);
        loginLink = findViewById(R.id.loginLink);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        signupBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String pin = pinInput.getText().toString().trim();

            if (name.isEmpty() || phone.length() != 10 || pin.length() != 4) {
                Toast.makeText(this, "Enter name, 10-digit phone, and 4-digit PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection detected.", Toast.LENGTH_LONG).show();
                return;
            }

            progressDialog.show();

            db.collection("users").whereEqualTo("phoneNumber", phone).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                    } else {
                        registerUser(name, phone, pin);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser(String name, String phone, String pin) {
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, name, pin, phone);

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                pref.saveUser(userId, name);
                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Signup Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
