package com.revnoah.txtmsglandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;
import java.util.HashMap;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private String serverUrl = "";
    private String username = "";
    private String password = "";

    android.content.SharedPreferences sharedPref = null;

    public static final String SMS_BUNDLE = "pdus";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public SmsBroadcastReceiver() {

    }

    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();
        if (intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            String smsMessageStr = "";
            String smsSenderAddress = "";
            for (int i = 0; i < sms.length; ++i) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i]);

                smsSenderAddress = smsMessage.getOriginatingAddress();
                smsMessageStr += smsMessage.getMessageBody().toString();
            }
            Toast.makeText(context, "From " + smsSenderAddress + " " + smsMessageStr, Toast.LENGTH_SHORT).show();

            //read connection preferences and set variables
            sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            setUsername(sharedPref.getString("general_username", ""));
            setPassword(sharedPref.getString("general_password", ""));
            setServerUrl(sharedPref.getString("server_address", "http://192.168.1.104/txtmsglanapi/public/api/"));

            SendMessageAsyncTask task = new SendMessageAsyncTask();

            java.util.HashMap<String, String> params = new HashMap<>();
            params.put("task", "messages");
            params.put("path", "messages");
            params.put("type", "received");
            params.put("status", "draft");
            params.put("message", smsMessageStr);
            params.put("server", getServerUrl());
            params.put("username", getUsername());
            params.put("password", getPassword());
            params.put("address", smsSenderAddress);

            task.execute(params);
        }
    }
}