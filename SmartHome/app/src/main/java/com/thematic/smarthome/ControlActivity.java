package com.thematic.smarthome;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class ControlActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    final static String CHANNEL_ID = "A";
    MqttAndroidClient client;
    MqttConnectOptions options;

    boolean control_seekbar_msg = true;

    Switch lock_switch, fan_switch, tv_switch;
    SeekBar light_seekbar;
    FloatingActionButton fab;

    private int nextDrawableId = R.drawable.ic_close;
    private int rotation = 180;

    String MqttHost = "tcp://iot.cht.com.tw:1883";

    //由於首用帳號密碼，所以沒有使用 apikey
    String apikey = "DKAWB90KPA5AMY2CBZ";

    String sub_topic_top = "/v1/device/";

    int Sub_qos[] = {1, 1, 1, 1};

    String Sub_topic = "/v1/device/20408404691/sensor/+/rawdata";

    private SharedPreferenceConfig sharedPreferenceConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        init();

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.e("msg_arrived", message.toString());
                String two_status_sensor[] = {"door", "fan", "TV"};

                String n = get_json_name(message.toString());
                int v = Integer.valueOf(get_json_value(message.toString()));

                for (int i = 0; i < two_status_sensor.length; i++) {
                    if (n.equals(two_status_sensor[i])) {
                        boolean switch_status = two_status(v);
                        switch (i) {
                            case 0:
                                lock_switch.setChecked(switch_status);
                                if (switch_status) {
                                    notify1();
                                }
                                break;
                            case 1:
                                fan_switch.setChecked(switch_status);
                                break;
                            case 2:
                                tv_switch.setChecked(switch_status);
                                break;
                        }
                    }
                }
                control_seekbar_msg = false;
                if (n.equals("light")) {
                    light_seekbar.setProgress(v);
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    //基本物件導向 MQTT連線
    private void init() {
        light_seekbar = findViewById(R.id.control_light_seekbar);
        light_seekbar.setOnSeekBarChangeListener(this);

        lock_switch = findViewById(R.id.control_lock_switch);
        fan_switch = findViewById(R.id.control_fan_switch);
        tv_switch = findViewById(R.id.control_tv_switch);

        //fab = findViewById(R.id.floatingActionButton);

        lock_switch.setOnCheckedChangeListener(this);
        fan_switch.setOnCheckedChangeListener(this);
        tv_switch.setOnCheckedChangeListener(this);

        //fab.setOnClickListener(this);

        mqtt_connect_to();
    }

    //登出按鈕
    public void signout(View view) {
        sharedPreferenceConfig.sign_out_commit();
        Intent sign_out_intent = new Intent(this, MainActivity.class);
        startActivity(sign_out_intent);
        finish();
    }

    //Switch 開關
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
        String getsensor = "";
        switch (compoundButton.getId()) {
            case R.id.control_lock_switch:
                Log.e("door", "on");
                getsensor = "door";
                check_info(isCheck, getsensor);
                break;
            case R.id.control_fan_switch:
                getsensor = "fan";
                check_info(isCheck, getsensor);
                break;
            case R.id.control_tv_switch:
                getsensor = "TV";
                check_info(isCheck, getsensor);
                break;
        }
    }

    //取得Switch 基本資訊 (sensor, 開關)
    private void check_info(boolean isCheck, String sensor) {
        if (isCheck) {
            pub(sensor, 1);
        } else {
            pub(sensor, 0);
        }
    }

    //發布 (利用回傳字串取得 發布訊息格式)
    private void pub(String sensor1, int stutas) {
        String pass_word = sharedPreferenceConfig.mqtt_connect_info_password();
        String Pub_topic = "/v1/device/" + pass_word + "/rawdata";
        String json_msg = set_msg_json(sensor1, stutas);
        try {
            Log.e("mqtt_pub", Pub_topic);
            client.publish(Pub_topic, json_msg.getBytes(), 1, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //下面三個都是滑桿，主要是第一個
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        //只要拖動就一直觸發
        if (control_seekbar_msg) {
            String sensor = "light";
            pub(sensor, i);

            Toast.makeText(this, String.valueOf(i), Toast.LENGTH_SHORT).show();
        }
        control_seekbar_msg = true;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //開始拖動觸發，僅一次
        control_seekbar_msg = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //拖動結束觸發，僅一次
        control_seekbar_msg = false;
    }

    //MQTT連線 ，會在 init 被呼叫
    private void mqtt_connect_to() {
        sharedPreferenceConfig = new SharedPreferenceConfig(getApplicationContext());

        String Mqtt_username = sharedPreferenceConfig.mqtt_connect_info_username();

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MqttHost, clientId);

        options = new MqttConnectOptions();
        options.setUserName(Mqtt_username);
        options.setPassword(Mqtt_username.toCharArray());

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("Mqtt_connect", "onSuccess");
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("Mqtt_connect", "onFailure");

                }
            });
        } catch (MqttException e) {
            Log.e("MQTT_CON", e.toString());
        } catch (NullPointerException n) {
            n.printStackTrace();
        }
    }

    //訂閱  MQTT連現成功時呼叫
    private void sub() {
        String Mqtt_password_device_id = sharedPreferenceConfig.mqtt_connect_info_password();

        String Sub_light_topic = sub_topic_top + Mqtt_password_device_id + "/sensor/light/rawdata";
        String Sub_door_topic = sub_topic_top + Mqtt_password_device_id + "/sensor/door/rawdata";
        String Sub_fan_topic = sub_topic_top + Mqtt_password_device_id + "/sensor/fan/rawdata";
        String Sub_TV_topic = sub_topic_top + Mqtt_password_device_id + "/sensor/TV/rawdata";

        String[] Sub = new String[]{Sub_light_topic, Sub_door_topic, Sub_fan_topic, Sub_TV_topic};

        try {
            //IMqttToken subToken = client.subscribe(Sub_topic, qos);
            IMqttToken subToken = client.subscribe(Sub, Sub_qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("mqtt_sub", "success");
                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //設定發布的訊息
    private String set_msg_json(String sensor, int stutas) {
        String a = "[{\"id\":\"" + sensor + "\",\"value\":[\"" + stutas + "\"]}]";
        Log.e("set_msg_json", a);
        return a;
    }

    //取得接收MQTT訊息時，拆解JSON並取得 sensor，在 onCreate 裡的 MessageArrived 被呼叫
    private String get_json_name(String json_msg) {
        String name = "null";
        try {
            //建立一個JSONObject並帶入JSON格式文字，getString(String key)取出欄位的數值
            JSONObject jsonObject = new JSONObject(json_msg);
            name = jsonObject.getString("id");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return name;
    }

    //取得接收MQTT訊息時，拆解JSON並取得 sensor 狀態，在 onCreate 裡的 MessageArrived 被呼叫
    private String get_json_value(String json_msg) {
        String value = "null";
        try {
            //建立一個JSONObject並帶入JSON格式文字，getString(String key)取出欄位的數值
            JSONObject jsonObject = new JSONObject(json_msg);
            String name = jsonObject.getString("id");

            JSONArray array = jsonObject.getJSONArray("value");
            for (int i = 0; i < array.length(); i++) {
                value = array.getString(i);
                Log.e("json_msg_arrived", name + value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    //判斷拆解出來的訊息 sensor 狀態  開或關
    private boolean two_status(int v) {
        boolean switch_status = false;
        if (v == 1) {
            switch_status = true;
        } else {
            switch_status = false;
        }
        return switch_status;
    }

    //語音按鈕
    @Override
    public void onClick(View view) {
        fab.animate()
                .rotationBy(rotation)        // rest 180 covered by "shrink" animation
                .setDuration(100)
                .scaleX(1.1f)           //Scaling to 110%
                .scaleY(1.1f)           //Scaling to 110%
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {

                        //Chaning the icon by the end of animation
                        //fab.setImageResource(nextDrawableId);
                        fab.setImageResource(R.drawable.ic_mic);
                        fab.animate()
                                .rotationBy(rotation)   //Complete the rest of the rotation
                                .setDuration(100)
                                .scaleX(1)              //Scaling back to what it was
                                .scaleY(1)
                                .start();

                        //Setting other drawable ID (your ids could be different)
                        nextDrawableId = (nextDrawableId == R.drawable.ic_mic) ? R.drawable.ic_close : R.drawable.ic_mic;
                        //Negating the existing value to reverse direction
                        rotation = -rotation;

                    }
                })
                .start();

        speechTotext_control();
    }

    //開啟語音          //如果不要語音 ，把 init  第 123 & 129 註解 、xml檔 10 到 19 行註解  ，註解方式 <!--      -->
    private void speechTotext_control() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "說些什麼吧");
        try {
            startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    //語音回傳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (data != null) {
                    ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String getspeechText = arrayList.get(0);

                    speech_control_sensor(getspeechText);


                } else {
                    Toast.makeText(this, "語音無法辨識", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //判斷回傳語音，沒有直接改變UI，利用MessageArrived改變UI
    private void speech_control_sensor(String control) {
        String a1[] = {"開燈", "關燈",
                "開門", "打開門", "關門", "鎖門",
                "打開風扇", "風扇打開", "關閉風扇", "風扇關閉",
                "打開電視", "電視打開", "關閉電視", "電視關閉"};
        for (int i = 0; i < a1.length; i++) {
            if (control.equals(a1[i])) {
                if (i == 0) {
                    pub("light", 100);
                } else if (i == 1) {
                    pub("light", 0);
                } else if (i == 2 || i == 3) {
                    pub("door", 1);
                } else if (i == 4 || i == 5) {
                    pub("door", 0);
                } else if (i == 6 || i == 7) {
                    pub("fan", 1);
                } else if (i == 8 || i == 9) {
                    pub("fan", 0);
                } else if (i == 10 || i == 11) {
                    pub("TV", 1);
                } else if (i == 12 || i == 13) {
                    pub("TV", 0);
                } else {
                    Toast.makeText(this, "語音無法辨識", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void notify1() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String notify_message = "門被開啟於 " + timer();
        String notify_title = getResources().getString(R.string.app_name);

        int importance = NotificationManager.IMPORTANCE_LOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "channel name", importance);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lock)
                .setContentTitle(notify_title)
                .setContentText(notify_message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(0, builder.build());
    }

    private String timer() {
        // 方法一
        Calendar mCal = Calendar.getInstance();
        CharSequence s = DateFormat.format("MM月dd日 kk時mm分ss秒", mCal.getTime());    // kk:24小時制, hh:12小時制

// 方法二
        String dateformat = "yyyyMMdd";
        SimpleDateFormat df = new SimpleDateFormat(dateformat);
        String today = df.format(mCal.getTime());

// 指定日期
        mCal.set(Calendar.YEAR, 2013);
        mCal.set(Calendar.MONTH, 11);                  // 1 月是 0, 所以 12 月是 11, 容易讓人搞混, 建議採用 Calendar 常數
        mCal.set(Calendar.MONTH, Calendar.DECEMBER);  // 跟前一行指令功能一樣，不過以 Calendar 常數 表示
        mCal.set(Calendar.DATE, 12);
        today = df.format(mCal.getTime());

        Log.e("TIMER", s.toString());
        return s.toString();
    }
}
