package com.thematic.smarthome;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceConfig {
    private SharedPreferences sharedPreferences;
    private Context context;

    public SharedPreferenceConfig(Context applicationContext) {
        this.context = applicationContext;
        sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.smart_home_sp), Context.MODE_PRIVATE);
    }

    public void SharedPreferenceConfig(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.smart_home_sp), Context.MODE_PRIVATE);
    }

    public void log_in_remember_me_commit(String username, String password, boolean status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getResources().getString(R.string.smart_home_username), username);
        editor.putString(context.getResources().getString(R.string.smart_home_password), password);
        editor.putBoolean(context.getResources().getString(R.string.smart_home_status), status);
        editor.commit();
    }

    public boolean log_in_check() {
        boolean status = false;
        status = sharedPreferences.getBoolean(context.getResources().getString(R.string.smart_home_status), status);
        return status;
    }

    public String mqtt_connect_info_username() {
        String username = "";
        username = sharedPreferences.getString(context.getResources().getString(R.string.smart_home_username), username);
        return username;
    }

    public String mqtt_connect_info_password() {
        String password = "";
        password = sharedPreferences.getString(context.getResources().getString(R.string.smart_home_password), password);
        return password;
    }

    public void sign_out_commit() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getResources().getString(R.string.smart_home_status), false);
        editor.commit();
    }

}
