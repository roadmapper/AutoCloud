package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

public class Transcoding {
    @SerializedName("url")
    public String url;
    @SerializedName("preset")
    String preset;
    @SerializedName("duration")
    String duration;
    @SerializedName("snipped")
    String snipped;
    @SerializedName("format")
    public Format format;
    @SerializedName("quality")
    String quality;
}
