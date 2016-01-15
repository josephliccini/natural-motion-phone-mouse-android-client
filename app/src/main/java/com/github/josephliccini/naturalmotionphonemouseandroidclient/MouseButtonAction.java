package com.github.josephliccini.naturalmotionphonemouseandroidclient;

/**
 * Created by Joseph on 1/13/2016.
 */
public class MouseButtonAction extends MouseMessage {
    public static final MouseButtonAction LEFT_PRESS = new MouseButtonAction("MouseButtonAction", "Left_Press");
    public static final MouseButtonAction RIGHT_PRESS = new MouseButtonAction("MouseButtonAction", "Right_Press");
    public static final MouseButtonAction LEFT_RELEASE = new MouseButtonAction("MouseButtonAction", "Left_Release");
    public static final MouseButtonAction RIGHT_RELEASE = new MouseButtonAction("MouseButtonAction", "Right_Release");

    private final String mouseActionType;

    private MouseButtonAction(String messageType, String mouseActionType) {
        super(messageType);
        this.mouseActionType = mouseActionType;
    }
}
