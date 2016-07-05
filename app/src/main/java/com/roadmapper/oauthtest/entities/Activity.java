package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * An object to wrap a user activity from a particular stream feed.
 */
public class Activity {

    @SerializedName("origin")
    public Track origin;
    @SerializedName("tags")
    public List<String> tags;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("type")
    public String type;
}
