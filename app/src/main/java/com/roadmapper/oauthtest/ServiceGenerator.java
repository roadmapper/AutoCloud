package com.roadmapper.oauthtest;

import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The management interface for Retrofit for making HTTP calls.
 */
public class ServiceGenerator {

    public static final String API_BASE_URL = "http://api.soundcloud.com";

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null);
    }

    public static <S> S createService(Class<S> serviceClass, final AccessToken token) {
        if (token != null) {
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();
                    if (original.url().queryParameter("oauth_token") == null) {
                        HttpUrl url = original.url().newBuilder().addQueryParameter("oauth_token", token.accessToken).build();
                        Log.d("ServiceGenerator", url.toString());
                        Request request = original.newBuilder().url(url).build();
                        return chain.proceed(request);}
                    else
                        return chain.proceed(original);
                }
            });
        }

        /*if (clientId != null) {
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    HttpUrl url = original.url().newBuilder().addQueryParameter("client_id", clientId).build();
                    Log.d("ServiceGenerator", url.toString());
                    Request request = original.newBuilder().url(url).build();
                    return chain.proceed(request);
                }
            });
        }

        if (clientSecret != null) {
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    HttpUrl url = original.url().newBuilder().addQueryParameter("client_secret", clientSecret).build();
                    Log.d("ServiceGenerator", url.toString());
                    Request request = original.newBuilder().url(url).build();
                    return chain.proceed(request);
                }
            });
        }*/

        httpClient.followRedirects(false);
        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }
}