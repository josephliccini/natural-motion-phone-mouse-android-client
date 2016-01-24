package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 1/22/2016.
 */
public class MouseWheelDelta extends MouseMessage {
    private String mouseWheelActionType;
    private double amount;

    public MouseWheelDelta(String mouseWheelActionType, double amount) {
        super("MouseWheelDelta");
        this.mouseWheelActionType = mouseWheelActionType;
        this.amount = amount;
    }
}
