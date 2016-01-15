/**
 * Created by Joseph on 1/13/2016.
 */
package com.github.josephliccini.naturalmotionphonemouseandroidclient;

public class DeltaPair extends MouseMessage {
    private double displacementX;
    private double displacementY;

    public DeltaPair(double displacementX, double displacementY) {
        super("MouseTranslation");
        this.displacementX = displacementX;
        this.displacementY = displacementY;
    }

    public double getDisplacementX() {
        return displacementX;
    }

    public double getDisplacementY() {
        return displacementY;
    }

    public void setDisplacementX(double displacementX) {
        this.displacementX = displacementX;
    }

    public void setDisplacementY(double displacementY) {
        this.displacementY = displacementY;
    }
}
