package com.boi.monitor.network;

public class DeleteRequest {
    private Integer days;
    private String date;

    public DeleteRequest(int days) {
        this.days = days;
    }

    public DeleteRequest(String date) {
        this.date = date;
    }
}

