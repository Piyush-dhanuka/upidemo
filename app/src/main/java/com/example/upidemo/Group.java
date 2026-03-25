package com.example.upidemo;

import java.util.List;

public class Group {
    public String groupId;
    public String groupName;
    public String createdBy;
    public List<String> members;

    public Group() {}
    public Group(String groupId, String groupName, String createdBy, List<String> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.createdBy = createdBy;
        this.members = members;
    }
}
