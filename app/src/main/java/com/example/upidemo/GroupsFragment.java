package com.example.upidemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment {
    private RecyclerView groupsRv;
    private GroupAdapter adapter;
    private List<Group> groupList;
    private FirebaseFirestore db;
    private PrefManager pref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(requireContext());
        groupsRv = view.findViewById(R.id.groupsRv);
        FloatingActionButton addGroupFab = view.findViewById(R.id.addGroupFab);

        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(getContext(), GroupDetailActivity.class);
            intent.putExtra("groupId", group.groupId);
            intent.putExtra("groupName", group.groupName);
            startActivity(intent);
        });

        groupsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        groupsRv.setAdapter(adapter);

        addGroupFab.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateGroupActivity.class)));

        loadGroups();

        return view;
    }

    private void loadGroups() {
        db.collection("groups")
                .whereArrayContains("members", pref.getUserId())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        groupList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Group group = doc.toObject(Group.class);
                            if (group != null) groupList.add(group);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
        private List<Group> groups;
        private OnGroupClickListener listener;

        public interface OnGroupClickListener {
            void onGroupClick(Group group);
        }

        public GroupAdapter(List<Group> groups, OnGroupClickListener listener) {
            this.groups = groups;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Group group = groups.get(position);
            holder.name.setText(group.groupName);
            holder.members.setText(group.members.size() + " members");
            holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
        }

        @Override
        public int getItemCount() { return groups.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, members;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.groupNameText);
                members = itemView.findViewById(R.id.memberCountText);
            }
        }
    }
}
