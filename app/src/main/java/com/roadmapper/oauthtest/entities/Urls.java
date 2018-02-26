package com.roadmapper.oauthtest.entities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Urls {
    @NonNull
    @SerializedName("http_mp3_128_url")
    public String http_url;

    @Nullable
    @SerializedName("hls_mp3_128_url")
    public String hls_url;

    @Nullable
    @SerializedName("hls_opus_64_url")
    public String hls_opus_url;

    @Nullable
    @SerializedName("rtmp_mp3_128_url")
    public String rtmp_url;

    @NonNull
    @SerializedName("preview_mp3_128_url")
    public String preview_url;
}
