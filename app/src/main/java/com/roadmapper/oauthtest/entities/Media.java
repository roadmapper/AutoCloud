package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Media {
    @SerializedName("transcodings")
    public List<Transcoding> transcodings;
}
