package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import org.opencv.core.Point;

import java.util.Comparator;

/**
 * Created by Joseph on 1/22/2016.
 */
public class ShortestYDistanceComparator implements Comparator<Point> {
    @Override
    public int compare(Point lhs, Point rhs) {
        return (int) (rhs.y - lhs.y);
    }
}
