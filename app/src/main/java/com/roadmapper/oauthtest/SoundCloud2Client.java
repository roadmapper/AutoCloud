package com.roadmapper.oauthtest;

import com.roadmapper.oauthtest.entities.AffiliatedActivities;
import com.roadmapper.oauthtest.entities.Track;
import com.roadmapper.oauthtest.entities.TrackUrn;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The Retrofit interface to interact with the unpublished SoundCloud HTTP API.
 */
public interface SoundCloud2Client {
    //https://api-v2.soundcloud.com/users/17645122/track_likes/3040119?client_id=D5FkbNvLCuNKECehP5wrooWTuVhzpVtf&app_version=1519311309
    @PUT("/users/{userId}/track_likes/{trackId}?app_version=1592566117")
    Call<ResponseBody> likeTrack(@Path("userId") String userId, @Path("trackId") String trackId, @Query("client_id") String clientId, @Body String body);

    @DELETE("/users/{userId}/track_likes/{trackId}?app_version=1592566117")
    Call<ResponseBody> unlikeTrack(@Path("userId") String userId, @Path("trackId") String trackId, @Query("client_id") String clientId);

    //{"track_urn":"soundcloud:tracks:389443545"}
    @POST("/me/play-history?app_version=1592566117")
    Call<ResponseBody> updatePlayHistory(@Query("client_id") String clientId, @Body TrackUrn track);

    @GET("/users/17645122/track_likes?app_version=1592566117")
    Call<AffiliatedActivities> getMyFavorites(@Query("client_id") String clientId, @Query("limit") long limit);

    @GET("/stream?app_version=1592566117")
    Call<AffiliatedActivities> getStreamTracks(@Query("client_id") String clientId, @Query("limit") long limit);
// https://api-v2.soundcloud.com/stream?sc_a_id=09ab7c468a36a953fa5a2eaf5cf9847504628d5d&device_locale=en&variant_ids=&user_urn=soundcloud%3Ausers%3A17645122&promoted_playlist=true&client_id=3JLYybc5BG7YPqpXxjNj8OQMnRMGYbIm&limit=10&offset=0&linked_partitioning=1&app_version=1592566117&app_locale=en

    //https://api-v2.soundcloud.com/users/17645122/track_likes?client_id=3JLYybc5BG7YPqpXxjNj8OQMnRMGYbIm&limit=24&offset=0&linked_partitioning=1&app_version=1592566117&app_locale=en
}
