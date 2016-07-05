package com.roadmapper.oauthtest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * The Retrofit interface to interact with the SoundCloud HTTP API (only for the OAuth authentication).
 */
public interface LoginService {
    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<AccessToken> getAccessToken(@FieldMap() Map<String, String> params);//@Field("code") String code, @Field("grant_type") String grantType);
}
