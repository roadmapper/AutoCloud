package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

public class Track {

     @SerializedName("kind")
     String kind;
     @SerializedName("id")
     public Long id;
     @SerializedName("created_at")
     String createdAt;
     @SerializedName("user_id")
     public Long userId;
     @SerializedName("duration")
     public Long duration;
     @SerializedName("commentable")
     Boolean commentable;
     @SerializedName("state")
     String state;
     @SerializedName("original_content_size")
     Long originalContentSize;
     @SerializedName("last_modified")
     String lastModified;
     @SerializedName("sharing")
     String sharing;
     @SerializedName("tag_list")
     String tagList;
     @SerializedName("permalink")
     String permalink;
     @SerializedName("streamable")
     public Boolean streamable;
     @SerializedName("embeddable_by")
     String embeddableBy;
     @SerializedName("downloadable")
     Boolean downloadable;
     @SerializedName("purchase_url")
     String purchaseUrl;
     @SerializedName("label_id")
     String labelId;
     @SerializedName("purchase_title")
     String purchaseTitle;
     @SerializedName("genre")
     public String genre;
     @SerializedName("title")
     public String title;
     @SerializedName("description")
     public String description;
     @SerializedName("label_name")
     String labelName;
     @SerializedName("release")
     String release;
     @SerializedName("track_type")
     String trackType;
     @SerializedName("key_signature")
     String keySignature;
     @SerializedName("isrc")
     String isrc;
     @SerializedName("video_url")
     String videoUrl;
     @SerializedName("bpm")
     String bpm;
     @SerializedName("release_year")
     String releaseYear;
     @SerializedName("release_month")
     String releaseMonth;
     @SerializedName("release_day")
     String releaseDay;
     @SerializedName("original_format")
     String originalFormat;
     @SerializedName("license")
     String license;
     @SerializedName("uri")
     String uri;
     @SerializedName("user")
     public UserProfile user;
     @SerializedName("permalink_url")
     String permalinkUrl;
     @SerializedName("artwork_url")
     public String artworkUrl;
     @SerializedName("waveform_url")
     String waveformUrl;
     @SerializedName("stream_url")
     public String streamUrl;
     @SerializedName("playback_count")
     Integer playbackCount;
     @SerializedName("download_count")
     Integer downloadCount;
     @SerializedName("favoritingsCount")
     Integer favoritingsCount;
     @SerializedName("comment_count")
     Integer commentCount;
     @SerializedName("attachments_uri")
     String attachmentsUri;
     @SerializedName("policy")
     String policy;
}
