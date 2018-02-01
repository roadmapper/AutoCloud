package com.roadmapper.oauthtest;

import android.app.Application;
import android.content.Context;

public class AutoCloudApplication extends Application {

    private static AutoCloudApplication currentApp = null;

    public static final String BASE_URL = "http://soundcloud.com";

    // you should either define client id and secret as constants or in string resources
    public static String CLIENT_ID;
    public static String CLIENT_ID_WEB;
    public static String OAUTH_TOKEN_WEB;

    @Override
    public void onCreate() {
        super.onCreate();
        currentApp = this;
        CLIENT_ID = getAppContext().getResources().getString(R.string.client_id);
        CLIENT_ID_WEB = getAppContext().getResources().getString(R.string.soundcloud_web_client_id);
        OAUTH_TOKEN_WEB = BuildConfig.token;
        SharedPrefManager.getInstance().writeSharedPrefString(R.string.oauth_token, AutoCloudApplication.OAUTH_TOKEN_WEB);
        SharedPrefManager.getInstance().writeSharedPrefString(R.string.oauth_scope, "*");

    }

    public static Context getAppContext() {
        return currentApp;
    }
}
