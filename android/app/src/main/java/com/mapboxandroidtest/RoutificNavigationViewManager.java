package com.mapboxandroidtest;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;


public class RoutificNavigationViewManager extends SimpleViewManager<RoutificNavigationView> {

    public static final String REACT_CLASS = "MapboxView";
    ReactApplicationContext mCallerContext;
    RoutificNavigationView mapboxView;

    public RoutificNavigationViewManager(ReactApplicationContext reactContext) {
        mCallerContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RoutificNavigationView createViewInstance(ThemedReactContext context) {
        this.mapboxView = new RoutificNavigationView(context);
        return this.mapboxView;
    }

}