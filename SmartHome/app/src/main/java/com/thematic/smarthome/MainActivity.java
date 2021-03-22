package com.thematic.smarthome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MainActivity extends AppCompatActivity {

    private SharedPreferenceConfig sharedPreferenceConfig;

    TextInputLayout name_text_layout, passw_text_layout;
    TextInputEditText name_text_input, passw_text_input;

    ImageView img;

    CheckBox remember_me_check_box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    //基本物件導向
    private void init() {
        img = findViewById(R.id.background_img);

        remember_me_check_box = findViewById(R.id.check_remember_me);

        name_text_layout = findViewById(R.id.username_layout);
        passw_text_layout = findViewById(R.id.password_layout);

        name_text_input = findViewById(R.id.username_editinput);
        passw_text_input = findViewById(R.id.password_editinput);

        img.setImageAlpha(80);

        //取得是否 有"記住我"，若有，則到控制頁面
        sharedPreferenceConfig = new SharedPreferenceConfig(getApplicationContext());
        if (sharedPreferenceConfig.log_in_check()) {
            Intent login = new Intent(this, ControlActivity.class);
            startActivity(login);
            finish();
        }
        name_text_input.setText(sharedPreferenceConfig.mqtt_connect_info_username());
        passw_text_input.setText(sharedPreferenceConfig.mqtt_connect_info_password());
    }

    //檢查帳號密碼輸入，有則回傳true
    private boolean get_user_input() {

        if (name_text_input.getText().toString().trim().isEmpty()) {
            name_text_layout.setError("帳 號 未 輸 入");
        } else {
            name_text_layout.setError(null);
            if (!passw_text_input.getText().toString().trim().isEmpty()) {
                passw_text_layout.setError(null);
                return true;
            }
        }
        if (passw_text_input.getText().toString().trim().isEmpty()) {
            passw_text_layout.setError("密 碼 未 輸 入");
        } else {
            passw_text_layout.setError(null);
        }
        return false;
    }

    //按鈕事件，判斷是否有"記住我"
    public void login_btn(View view) {

        String name = name_text_input.getText().toString();
        String passw = passw_text_input.getText().toString();

        if (get_user_input()) {
            if (remember_me_check_box.isChecked()) {
                sharedPreferenceConfig.log_in_remember_me_commit(name, passw, true);
            } else {
                sharedPreferenceConfig.log_in_remember_me_commit(name, passw, false);
            }
            Intent login_intent = new Intent(this, ControlActivity.class);
            startActivity(login_intent);
            finish();
        }
    }
}
