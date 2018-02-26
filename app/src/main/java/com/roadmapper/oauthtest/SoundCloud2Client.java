package com.roadmapper.oauthtest;

import com.roadmapper.oauthtest.entities.TrackUrn;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
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
    @PUT("/users/{userId}/track_likes/{trackId}?app_version=1519311309")
    Call<ResponseBody> likeTrack(@Path("userId") String userId, @Path("trackId") String trackId, @Query("client_id") String clientId, @Body String body);

    @DELETE("/users/{userId}/track_likes/{trackId}?app_version=1519311309")
    Call<ResponseBody> unlikeTrack(@Path("userId") String userId, @Path("trackId") String trackId, @Query("client_id") String clientId);

    //{"track_urn":"soundcloud:tracks:389443545"}
    @POST("/me/play-history?app_version=1519311309")
    Call<ResponseBody> updatePlayHistory(@Query("client_id") String clientId, @Body TrackUrn track);
}
