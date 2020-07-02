package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The set of activities from a SoundCloud user stream.
 */
public class AffiliatedActivities {

    @SerializedName("future_href")
    public String futureHref;
    @SerializedName("collection")
    public List<Activity> collection;
    @SerializedName("next_href")
    public String nextHref;
    @SerializedName("query_urn")
    public String queryUrn;
}
