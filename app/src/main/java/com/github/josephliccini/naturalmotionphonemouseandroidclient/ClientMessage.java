package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 3/19/2016.
 */
public class ClientMessage {
    private String message;

    public ClientMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
