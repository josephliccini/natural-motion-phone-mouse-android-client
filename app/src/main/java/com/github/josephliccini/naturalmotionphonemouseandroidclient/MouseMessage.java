package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 1/14/2016.
 */
public abstract class MouseMessage {
    private final String messageType;

    protected MouseMessage(String messageType) {
        this.messageType = messageType;
    }
}
