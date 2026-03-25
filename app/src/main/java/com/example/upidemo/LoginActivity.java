package com.example.upidemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText phoneInput, pinInput;
    private Button loginBtn;
    private TextView signupLink;
    private FirebaseFirestore db;
    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);

        phoneInput = findViewById(R.id.phoneInput);
        pinInput = findViewById(R.id.pinInput);
        loginBtn = findViewById(R.id.loginBtn);
        signupLink = findViewById(R.id.signupLink);

        loginBtn.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            String pin = pinInput.getText().toString().trim();

            if (phone.length() != 10 || pin.length() != 4) {
                Toast.makeText(this, "Enter 10-digit phone and 4-digit PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users")
                .whereEqualTo("phoneNumber", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    } else {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null && user.pin.equals(pin)) {
                            pref.saveUser(user.userId, user.name);
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }
}
