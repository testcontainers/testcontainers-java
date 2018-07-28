package com.example.linkedcontainer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A crude, partially implemented Redmine client.
 */
public class RedmineClient {

    private String url;
    private OkHttpClient client;

    public RedmineClient(String url) {
        this.url = url;
        client = new OkHttpClient();
    }

    public int getIssueCount() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/issues.json")
                .build();

        Response response = client.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());
        return jsonObject.getInt("total_count");
    }
}
