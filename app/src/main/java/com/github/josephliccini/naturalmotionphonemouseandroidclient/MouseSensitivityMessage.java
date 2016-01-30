package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 1/30/2016.
 */
public class MouseSensitivityMessage extends MouseMessage {
    private double amount;

    public MouseSensitivityMessage(double amount) {
        super("MouseSensitivity");
        this.amount = amount;
    }
}