package com.roadmapper.oauthtest;

import android.app.Application;
import android.content.Context;

public class AutoCloudApplication extends Application {

    private static AutoCloudApplication currentApp = null;

    private static Context context;

    public static final String BASE_URL = "http://soundcloud.com";

    // you should either define client id and secret as constants or in string resources
    public static String CLIENT_ID;
    public static String CLIENT_ID_WEB;
    public static String OAUTH_TOKEN_WEB = "1-138878-17645122-df148c40088f46";

    @Override
    public void onCreate() {
        super.onCreate();
        currentApp = this;
        context = getApplicationContext();
        CLIENT_ID = context.getResources().getString(R.string.client_id);
        CLIENT_ID_WEB = context.getResources().getString(R.string.soundcloud_web_client_id);
    }

    public static Context getAppContext() {
        return context;
    }
}
