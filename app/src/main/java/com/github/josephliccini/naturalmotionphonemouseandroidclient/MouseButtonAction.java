package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import com.google.gson.Gson;

/**
 * Created by Joseph on 1/13/2016.
 */
public class MouseButtonAction extends MouseMessage {
    public static final MouseButtonAction LEFT_PRESS = new MouseButtonAction("MouseButtonAction", "Left_Press");
    public static final MouseButtonAction RIGHT_PRESS = new MouseButtonAction("MouseButtonAction", "Right_Press");
    public static final MouseButtonAction LEFT_RELEASE = new MouseButtonAction("MouseButtonAction", "Left_Release");
    public static final MouseButtonAction RIGHT_RELEASE = new MouseButtonAction("MouseButtonAction", "Right_Release");
    public static final MouseButtonAction MIDDLE_PRESS = new MouseButtonAction("MouseButtonAction", "Middle_Press");
    public static final MouseButtonAction XBUTTON1_PRESS = new MouseButtonAction("MouseButtonAction", "XButton1_Press");
    public static final MouseButtonAction XBUTTON1_RELEASE = new MouseButtonAction("MouseButtonAction", "XButton1_Release");
    public static final MouseButtonAction XBUTTON2_PRESS = new MouseButtonAction("MouseButtonAction", "XButton2_Press");
    public static final MouseButtonAction XBUTTON2_RELEASE = new MouseButtonAction("MouseButtonAction", "XButton2_Release");

    private final String mouseActionType;

    private MouseButtonAction(String messageType, String mouseActionType) {
        super(messageType);
        this.mouseActionType = mouseActionType;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
