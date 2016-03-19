package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joseph on 1/31/2016.
 */
public class UserActivityObservable {
    private final List<UserActivityObserver> observerList = new ArrayList<UserActivityObserver>();

    public final void notifyObservers() {
        for (UserActivityObserver observer: observerList) {
            observer.onUserActivity();
        }
    }

    public final void registerObserver(UserActivityObserver o) {
        observerList.add(o);
    }

    public final void deleteObserver(UserActivityObserver o) {
        int index = observerList.indexOf(o);
        observerList.remove(index);
    }
}
