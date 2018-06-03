package com.revnoah.txtmsglandroid;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

class SendMessageAsyncTask extends AsyncTask<HashMap<String, String>, Void, JSONObject> {
    private String ACTIVITY_NAME = "SendMessageAsyncTask";

    public AsyncResponse delegate = null;
    SharedPreferences sharedPref = null;

    private String accessToken = null;
    private String serverUrl = "http://192.168.1.104/txtmsglanapi/public/api/";
    private String username = "";
    private String password = "";

    //getters and setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

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
    protected JSONObject doInBackground(HashMap<String, String>... params) {
        JSONObject jsonResponse = null;
        boolean firstRun = true;

        do {
            firstRun = !(refreshAccessToken(params[0]));
        } while (firstRun);

        //send message using params passed into task
        sendMessage(params[0]);

        return jsonResponse;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        //delegate.processFinish(result);
        super.onPostExecute(result);
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
    }

    private boolean refreshAccessToken(HashMap<String, String> params) {
        boolean tokenRefreshed = false;

        try {
            //attempt to log in user
            JSONObject jsonResponse = loginUser(params.get("username"), params.get("password"));

            //attempt to register user if access token is unset
            if (jsonResponse.getString("access_token") == null) {
                jsonResponse = registerUser(params.get("username"), params.get("username"), params.get("password"));
            }

            //handle access token, if we have one
            if (jsonResponse.getString("access_token") != null) {
                Log.d(ACTIVITY_NAME, "Token: " + jsonResponse.getString("access_token"));
                String accessToken = jsonResponse.getString("access_token");
                setAccessToken(accessToken);
                tokenRefreshed = true;
            } else {
                Log.e(ACTIVITY_NAME, "Token unset");
            }
        } catch (Exception e) {
            Log.e(ACTIVITY_NAME, e.getMessage());
        }

        return tokenRefreshed;
    }

    private HttpURLConnection getConnection(String route) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(serverUrl + route);
            conn = (HttpURLConnection) url.openConnection();

            //set jwt access token obtained through login or registration
            String bearerAuth = "bearer " + getAccessToken();
            conn.setRequestProperty("Authorization", bearerAuth);

            //set connect settings
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);

            //additional connection settings
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return conn;
    }

    private HttpURLConnection getConnection(String route, String method) {
        HttpURLConnection conn = null;

        try {
            //get base connection
            conn = getConnection(route);

            //verify that the method is valid
            String[] valueArray = {"POST", "GET", "HEAD", "PUT", "PATCH", "DELETE"};
            if (Arrays.asList(valueArray).contains(method)) {
                conn.setRequestMethod(method);
            }
        } catch (ProtocolException e) {
            e.getStackTrace();
        } catch (Exception e) {
            e.getStackTrace();
        }

        return conn;
    }

    private int sendQuery(HttpURLConnection conn, String query) {
        int responseCode = 500;

        try {
            //handle output of data
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            //get three digit http response code
            responseCode = conn.getResponseCode();

            return responseCode;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseCode;
    }

    private JSONObject getResponseJSON(HttpURLConnection conn) {
        JSONObject jsonResponse = null;

        try {
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + '\n');
            }
            String jsonString = stringBuilder.toString();
            jsonResponse = new JSONObject(jsonString);
        } catch(java.io.IOException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }

    public JSONObject sendMessage(HashMap<String, String> params)
    {
        JSONObject jsonResponse = null;

        //if access token is invalid, reauthenticate
        if(getAccessToken() == null) {
            refreshAccessToken(params);
        }

        try {
            //setup connection for route
            HttpURLConnection conn = getConnection("messages");

            //set values
            String messageType = params.get("type") != null ? params.get("type") : "sent";
            String messageStatus = params.get("status") != null ? params.get("status") : "draft";
            String messageText = params.get("message") != null ? params.get("message") : "test message";
            String senderAddress = params.get("address") != null ? params.get("address") : null;

            //content params
            Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("message_status", messageStatus)
                .appendQueryParameter("message_type", messageType)
                .appendQueryParameter("message_text", messageText)
                .appendQueryParameter("sender_address", senderAddress);
            String query = builder.build().getEncodedQuery();

            //send query and get response code
            int responseCode = sendQuery(conn, query);

            //2xx successful response code
            if(Math.floor(responseCode / 100) == 2) {
                jsonResponse = getResponseJSON(conn);
            } else {
                //clear access token to trigger next request
                setAccessToken(null);
            }

            conn.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }

    //register new user and return status
    public JSONObject registerUser(String userName, String userEmail, String userPassword)
    {
        JSONObject jsonResponse = null;

        try {
            HttpURLConnection conn = getConnection("register");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("name", userName)
                    .appendQueryParameter("email", userEmail)
                    .appendQueryParameter("password", userPassword);

            String query = builder.build().getEncodedQuery();

            //send query and get response code
            int responseCode = sendQuery(conn, query);

            //2xx successful response code
            if(Math.floor(responseCode / 100) == 2) {
                jsonResponse = getResponseJSON(conn);
                String token = jsonResponse.getString("access_token");
                setAccessToken(token);
            }

            conn.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }

    //login user and return status
    public JSONObject loginUser(String userEmail, String userPassword)
    {
        JSONObject jsonResponse = null;

        try {
            HttpURLConnection conn = getConnection("login");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("email", userEmail)
                    .appendQueryParameter("password", userPassword);
            String query = builder.build().getEncodedQuery();

            //send query and get response code
            int responseCode = sendQuery(conn, query);

            //2xx successful response code
            if (Math.floor(responseCode / 100) == 2) {
                jsonResponse = getResponseJSON(conn);
                String token = jsonResponse.getString("access_token");
                setAccessToken(token);
            }

            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }
}