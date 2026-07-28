package com.boi.monitor.network;

import com.google.gson.annotations.SerializedName;

public class AnonymousAuthRequest {

    @SerializedName("userId")
    private String userId;

    public AnonymousAuthRequest() {}

    public AnonymousAuthRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
