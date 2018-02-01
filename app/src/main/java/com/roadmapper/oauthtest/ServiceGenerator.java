package com.roadmapper.oauthtest;

import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The management interface for Retrofit for making HTTP calls.
 */
public class ServiceGenerator {

    public static final String API_BASE_URL = "https://api.soundcloud.com";
    public static final String API2_BASE_URL = "https://api-v2.soundcloud.com";

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    private static Retrofit.Builder builder2 =
            new Retrofit.Builder()
                    .baseUrl(API2_BASE_URL)
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
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            httpClient.addInterceptor(logging);

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
        Retrofit retrofit;
        if (serviceClass.getName().equals(SoundCloud2Client.class.getName())) {
            retrofit = builder2.client(client).build();
        } else {
            retrofit = builder.client(client).build();
        }
        return retrofit.create(serviceClass);
    }
}