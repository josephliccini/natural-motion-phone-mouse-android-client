package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

/**
 * Created by Joseph on 1/31/2016.
 */
public class PointUtils {

    public static MatOfPoint2f convertToPoint(MatOfKeyPoint mat) {
        KeyPoint[] arr = mat.toArray();
        Point[] pointArr = new Point[arr.length];

        for(int i = 0; i < arr.length; ++i) {
            pointArr[i] = arr[i].pt;
        }

        return new MatOfPoint2f(pointArr);
    }

    public static MatOfKeyPoint convertToKeyPoint(MatOfPoint2f mat) {
        Point[] arr = mat.toArray();
        KeyPoint[] pointArr = new KeyPoint[arr.length];

        for(int i = 0; i < arr.length; ++i) {
            Point p = arr[i];
            pointArr[i] = new KeyPoint((float)p.x, (float)p.y, 1.0f);
        }

        return new MatOfKeyPoint(pointArr);
    }


}
