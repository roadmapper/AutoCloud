package com.roadmapper.oauthtest;

import com.roadmapper.oauthtest.entities.AffiliatedActivities;
import com.roadmapper.oauthtest.entities.Track;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The Retrofit interface to interact with the SoundCloud HTTP API.
 */
public interface SoundCloudClient {
    @GET("/users/{username}/favorites.json")
    Call<List<Track>> getFavoriteTracks(@Path("username") String username);

    @GET("/me/activities/tracks/affiliated")
    Call<AffiliatedActivities> getStreamTracks(@Query("limit") long limit);

    @GET("/me/favorites")
    Call<List<Track>> getMyFavorites(@Query("limit") long limit);

    @GET("/tracks/{trackId}/stream")
    Call<ResponseBody> getStreamInfo(@Path("trackId") long trackId);
}