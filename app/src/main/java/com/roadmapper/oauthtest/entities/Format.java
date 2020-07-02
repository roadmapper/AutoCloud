package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

public class Format {
    @SerializedName("protocol")
    public String protocol;
    @SerializedName("mime_type")
    public String mimeType;
}
