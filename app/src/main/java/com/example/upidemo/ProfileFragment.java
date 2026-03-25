package com.example.upidemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        PrefManager pref = new PrefManager(requireContext());
        TextView userName = view.findViewById(R.id.userName);
        TextView userId = view.findViewById(R.id.userId);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        userName.setText(pref.getUserName());
        userId.setText("ID: " + pref.getUserId());

        logoutBtn.setOnClickListener(v -> {
            pref.logout();
            startActivity(new Intent(requireContext(), SignupActivity.class));
            requireActivity().finish();
        });

        return view;
    }
}
