package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

/**
 * A SoundCloud user profile associated with a sound.
 */
public class UserProfile {

    @SerializedName("id")
    public Long id;
    @SerializedName("kind")
    String kind;
    @SerializedName("permalink")
    String permalink;
    @SerializedName("username")
    public String username;
    @SerializedName("last_modified")
    String lastModified;
    @SerializedName("uri")
    String uri;
    @SerializedName("permalink_url")
    String permalinkUrl;
    @SerializedName("avatar_url")
    String avatarUrl;
    @SerializedName("country")
    String country;
    @SerializedName("first_name")
    String firstName;
    @SerializedName("last_name")
    String lastName;
    @SerializedName("full_name")
    public String fullName;
    @SerializedName("description")
    String description;
    @SerializedName("city")
    String city;
    @SerializedName("discogs_name")
    String discogsName;
    @SerializedName("myspace_name")
    String myspaceName;
    @SerializedName("website")
    String website;
    @SerializedName("website_title")
    String websiteTitle;
    @SerializedName("online")
    Boolean online;
    @SerializedName("track_count")
    Integer trackCount;
    @SerializedName("playlist_count")
    Integer playlistCount;
    @SerializedName("plan")
    String plan;
    @SerializedName("public_favorites_count")
    Integer publicFavoritesCount;
    @SerializedName("followers_count")
    Integer followersCount;
    @SerializedName("followings_count")
    Integer followingsCount;

    // mini version of user profile in tracks listing
    /*id: 237200,
    kind: "user",
    permalink: "anjunabeats",
    username: "Anjunabeats",
    last_modified: "2016/11/04 12:46:58 +0000",
    uri: "https://api.soundcloud.com/users/237200",
    permalink_url: "http://soundcloud.com/anjunabeats",
    avatar_url: "https://i1.sndcdn.com/avatars-000275637550-gjn2gq-large.jpg"*/
}
