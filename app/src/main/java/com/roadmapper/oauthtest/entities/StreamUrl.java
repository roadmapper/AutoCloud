package com.roadmapper.oauthtest.entities;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StreamUrl {
    @NonNull
    @SerializedName("url")
    public String url;
}
