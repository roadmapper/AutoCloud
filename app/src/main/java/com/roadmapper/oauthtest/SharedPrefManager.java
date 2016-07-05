package com.roadmapper.oauthtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;

public class SharedPrefManager {

    private static SharedPrefManager instance = null;
    private static Context context = AutoCloudApplication.getAppContext();
    private static SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_prefs), Context.MODE_PRIVATE);

    private SharedPrefManager() {}

    public static SharedPrefManager getInstance() {
        if (instance == null) {
            instance = new SharedPrefManager();
        }
        return instance;
    }

    public void writeSharedPrefString(@StringRes int key, String val) {
        sharedPref.edit().putString(context.getString(key), val).apply();
    }

    public void writeSharedPrefInt(@StringRes int key, int val) {
        sharedPref.edit().putInt(context.getString(key), val).apply();
    }

    public String readSharedPrefString(@StringRes int key) {
        return sharedPref.getString(context.getString(key), "");
    }

    public int readSharedPrefInt(@StringRes int key) {
        return sharedPref.getInt(context.getString(key), -1);
    }
}
