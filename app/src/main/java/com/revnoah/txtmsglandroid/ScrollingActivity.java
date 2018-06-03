package com.revnoah.txtmsglandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo.DetailedState; //NOTE: I'm not sure if this is the right import
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;

public class ScrollingActivity extends AppCompatActivity {
    private String serverUrl = "";
    private String username = "";
    private String password = "";

    SharedPreferences sharedPref = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //read connection preferences and set variables
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setUsername(sharedPref.getString("general_username", ""));
        setPassword(sharedPref.getString("general_password", ""));
        setServerUrl(sharedPref.getString("server_address", "http://192.168.1.104/txtmsglanapi/public/api/"));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //Intent myIntent = new Intent(ScrollingActivity.this, SmsActivity.class);
            //myIntent.putExtra("key", value); //Optional parameters
            //ScrollingActivity.this.startActivity(myIntent);

            String wifiName = getWifiName(getBaseContext());

            Snackbar.make(view, "Connected to: " + wifiName, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            SendMessageAsyncTask task = new SendMessageAsyncTask();
            String messageText = "This is a test message";

            HashMap<String, String> params = new HashMap<>();
            params.put("task", "messages");
            params.put("path", "messages");
            params.put("type", "sent");
            params.put("status", "draft");
            params.put("address", "123456");
            params.put("message", messageText);
            params.put("server", getServerUrl());
            params.put("username", getUsername());
            params.put("password", getPassword());

            task.execute(params);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(ScrollingActivity.this, SettingsActivity.class);
            //myIntent.putExtra("key", value); //Optional parameters
            ScrollingActivity.this.startActivity(myIntent);

            return true;
        } else if(id == R.id.action_about) {
            Intent aboutIntent = new Intent(ScrollingActivity.this, AboutActivity.class);
            ScrollingActivity.this.startActivity(aboutIntent);

            return true;
        } else if(id == R.id.action_refresh) {
            SendMessageAsyncTask task = new SendMessageAsyncTask();

            HashMap<String, String> params = new HashMap<>();
            params.put("task", "refresh");
            params.put("path", "poll");

            task.execute(params);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getWifiName(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == DetailedState.CONNECTED || state == DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

}
