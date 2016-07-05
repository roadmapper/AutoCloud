package com.roadmapper.oauthtest.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by vinaydandekar on 2/21/16.
 */
public class AffiliatedActivities {

    @SerializedName("future_href")
    public String futureHref;
    @SerializedName("collection")
    public List<Activity> collection;
    @SerializedName("next_href")
    public String nextHref;

}
