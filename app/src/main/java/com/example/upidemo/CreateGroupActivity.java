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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CreateGroupActivity extends AppCompatActivity {
    private EditText groupNameInput, searchPhoneInput;
    private RecyclerView usersRv, recentUsersRv;
    private TextView recentTitle;
    private Button createGroupBtn, searchBtn;
    private FirebaseFirestore db;
    private PrefManager pref;
    private SelectedUserAdapter adapter;
    private RecentUserAdapter recentAdapter;
    private List<User> selectedUsers;
    private List<User> recentUsers;
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
        recentUsersRv = findViewById(R.id.recentUsersRv);
        recentTitle = findViewById(R.id.recentTitle);
        createGroupBtn = findViewById(R.id.createGroupBtn);
        searchBtn = findViewById(R.id.searchBtn);

        selectedUsers = new ArrayList<>();
        recentUsers = new ArrayList<>();
        selectedUserIds = new ArrayList<>();
        
        selectedUserIds.add(pref.getUserId());

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

        recentAdapter = new RecentUserAdapter(recentUsers, user -> {
            if (!selectedUserIds.contains(user.userId)) {
                selectedUsers.add(user);
                selectedUserIds.add(user.userId);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "User already added", Toast.LENGTH_SHORT).show();
            }
        });

        recentUsersRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentUsersRv.setAdapter(recentAdapter);

        loadRecentPeople();

        searchBtn.setOnClickListener(v -> searchUser());
        createGroupBtn.setOnClickListener(v -> createGroup());
    }

    private void loadRecentPeople() {
        String currentUserId = pref.getUserId();
        db.collection("groups")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> memberIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Group group = doc.toObject(Group.class);
                        if (group.members != null) {
                            for (String id : group.members) {
                                if (!id.equals(currentUserId)) {
                                    memberIds.add(id);
                                }
                            }
                        }
                    }

                    if (memberIds.isEmpty()) {
                        recentTitle.setVisibility(View.GONE);
                        recentUsersRv.setVisibility(View.GONE);
                        return;
                    }

                    // Fetch user details for these IDs
                    List<String> idsList = new ArrayList<>(memberIds);
                    // Firestore 'in' query supports up to 10 elements.
                    // For simplicity, we'll just take the first 10 recent unique members.
                    List<String> limitedIds = idsList.subList(0, Math.min(idsList.size(), 10));

                    db.collection("users")
                            .whereIn("userId", limitedIds)
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                recentUsers.clear();
                                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                                    User user = userDoc.toObject(User.class);
                                    recentUsers.add(user);
                                }
                                if (!recentUsers.isEmpty()) {
                                    recentTitle.setVisibility(View.VISIBLE);
                                    recentUsersRv.setVisibility(View.VISIBLE);
                                    recentAdapter.notifyDataSetChanged();
                                }
                            });
                });
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

    private static class RecentUserAdapter extends RecyclerView.Adapter<RecentUserAdapter.ViewHolder> {
        private List<User> users;
        private OnUserClickListener listener;

        public interface OnUserClickListener {
            void onUserClick(User user);
        }

        public RecentUserAdapter(List<User> users, OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.name.setText(user.name);
            // Hide the checkbox for the horizontal recent list to keep it clean
            View checkBox = holder.itemView.findViewById(R.id.userCheckBox);
            if (checkBox != null) checkBox.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() { return users.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.userNameText);
            }
        }
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
