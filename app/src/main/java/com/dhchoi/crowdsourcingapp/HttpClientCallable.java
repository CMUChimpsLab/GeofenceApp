package com.dhchoi.crowdsourcingapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class HttpClientCallable implements Callable<String> {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String CHARSET = "UTF-8";

    private static final String TAG = "HttpClientCallable";

    private String mUrl;
    private String mMethod;
    private String mData;

    public HttpClientCallable(String url, String method, Map<String, String> data) {
        mUrl = url;
        mMethod = method;
        mData = createParams(data);
    }

    @Override
    public String call() {
        HttpURLConnection urlConnection = null;
        String response = null;

        try {
            byte[] data = mData != null ? mData.getBytes(CHARSET) : null;

            Log.d(TAG, "Initiating with url: " + mUrl + ", method: " + mMethod + ", data: " + mData);

            if (mMethod.equals(POST)) {
                urlConnection = (HttpURLConnection) new URL(mUrl).openConnection();
                urlConnection.setRequestMethod(POST);
                urlConnection.setRequestProperty("Accept-Charset", CHARSET);
                if (data != null) {
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
                    urlConnection.getOutputStream().write(data);
                }
            } else {
                urlConnection = (HttpURLConnection) new URL(data != null ? mUrl + "?" + new String(data) : mUrl).openConnection();
                urlConnection.setRequestMethod(GET);
                urlConnection.setRequestProperty("Accept-Charset", CHARSET);
                urlConnection.setRequestProperty("Content-Type", "text/plain");
            }

            if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                response = "";
                String line;
                while ((line = in.readLine()) != null) {
                    response += line;
                }
            } else {
                // Do response handling for bad response codes
                Log.e(TAG, "Bad http response code: " + urlConnection.getResponseCode());
            }

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            // Handle problems
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        Log.d(TAG, "Returning http response: " + response);

        return response;
    }

    private static String createParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        String paramsAsString = "";
        try {
            for (String key : params.keySet()) {
                if (!paramsAsString.isEmpty()) {
                    paramsAsString += "&";
                }
                paramsAsString += key + "=" + URLEncoder.encode(params.get(key), CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
        }

        return paramsAsString;
    }

    public static class Executor {

        private static ExecutorService pool = Executors.newSingleThreadExecutor();

        public static String execute(HttpClientCallable httpClientCallable) {
            try {
                Future<String> result = pool.submit(httpClientCallable);
                return result.get();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            } catch (ExecutionException e) {
                Log.e(TAG, e.getMessage());
            }

            return null;
        }
    }
}
