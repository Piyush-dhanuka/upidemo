package com.example.upidemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateGroupActivity extends AppCompatActivity {
    private EditText groupNameInput, searchPhoneInput;
    private RecyclerView usersRv;
    private Button createGroupBtn, searchBtn;
    private FirebaseFirestore db;
    private PrefManager pref;
    private SelectedUserAdapter adapter;
    private List<User> selectedUsers;
    private List<String> selectedUserIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);
        groupNameInput = findViewById(R.id.groupNameInput);
        searchPhoneInput = findViewById(R.id.searchPhoneInput);
        usersRv = findViewById(R.id.usersRv);
        createGroupBtn = findViewById(R.id.createGroupBtn);
        searchBtn = findViewById(R.id.searchBtn);

        selectedUsers = new ArrayList<>();
        selectedUserIds = new ArrayList<>();
        
        // Add current user to the group by default
        selectedUserIds.add(pref.getUserId());
        // We don't necessarily need the current user in the visible list, 
        // but it's good for clarity if we did. For now, let's just keep them in IDs.

        adapter = new SelectedUserAdapter(selectedUsers, userId -> {
            selectedUserIds.remove(userId);
            for (int i = 0; i < selectedUsers.size(); i++) {
                if (selectedUsers.get(i).userId.equals(userId)) {
                    selectedUsers.remove(i);
                    break;
                }
            }
            adapter.notifyDataSetChanged();
        });

        usersRv.setLayoutManager(new LinearLayoutManager(this));
        usersRv.setAdapter(adapter);

        searchBtn.setOnClickListener(v -> searchUser());
        createGroupBtn.setOnClickListener(v -> createGroup());
    }

    private void searchUser() {
        String phone = searchPhoneInput.getText().toString().trim();
        if (phone.length() != 10) {
            Toast.makeText(this, "Enter 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").whereEqualTo("phoneNumber", phone).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    } else {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            if (selectedUserIds.contains(user.userId)) {
                                Toast.makeText(this, "User already added", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedUsers.add(user);
                                selectedUserIds.add(user.userId);
                                adapter.notifyDataSetChanged();
                                searchPhoneInput.setText("");
                                Toast.makeText(this, "Added: " + user.name, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createGroup() {
        String groupName = groupNameInput.getText().toString().trim();
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Enter group name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedUserIds.size() < 2) {
            Toast.makeText(this, "Add at least one member", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupId = UUID.randomUUID().toString();
        Group group = new Group(groupId, groupName, pref.getUserId(), selectedUserIds);

        db.collection("groups").document(groupId).set(group)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group Created", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show());
    }

    private static class SelectedUserAdapter extends RecyclerView.Adapter<SelectedUserAdapter.ViewHolder> {
        private List<User> users;
        private OnUserRemovedListener listener;

        public interface OnUserRemovedListener {
            void onUserRemoved(String userId);
        }

        public SelectedUserAdapter(List<User> users, OnUserRemovedListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.text1.setText(user.name);
            holder.text2.setText(user.phoneNumber + " (Tap to remove)");
            holder.itemView.setOnClickListener(v -> listener.onUserRemoved(user.userId));
        }

        @Override
        public int getItemCount() { return users.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
