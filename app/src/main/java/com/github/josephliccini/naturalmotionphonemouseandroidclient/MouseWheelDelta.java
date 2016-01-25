package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 1/22/2016.
 */
public class MouseWheelDelta extends MouseMessage {
    private String mouseWheelActionType;
    private double amount;

    public static final MouseWheelDelta MOUSE_WHEEL_UP = new MouseWheelDelta("MouseWheelUp", 1);
    public static final MouseWheelDelta MOUSE_WHEEL_DOWN = new MouseWheelDelta("MouseWheelDown", 1);

    private MouseWheelDelta(String mouseWheelActionType, double amount) {
        super("MouseWheelDelta");
        this.mouseWheelActionType = mouseWheelActionType;
        this.amount = amount;
    }
}
