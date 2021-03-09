package com.mapboxandroidtest;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class RoutificNavigationView extends LinearLayout {
    public View linearLayout;
    public RoutificMapboxFragment routificMapboxFragment;
    public Context navigationViewContext;
    public boolean navigationEnded = true;


    public RoutificNavigationView(@NonNull Context context) {
        super(context);
        navigationViewContext = context;
        Fragment frag = ((MainActivity) ((ThemedReactContext) getContext()).getCurrentActivity()).getSupportFragmentManager().findFragmentById(R.id.routific_map_fragment);
        if (frag == null) {
            this.linearLayout = inflate(context, R.layout.routific_navigation_view, this);
        } else {
            this.linearLayout = findViewById(R.id.routific_navigation_view);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        setup();
    }

    public void setup() {
        if (routificMapboxFragment != null && navigationEnded) {
            getCurrentFragmentManager().beginTransaction().remove(routificMapboxFragment).commit();
        }
        navigationEnded = false;
        Fragment frag = ((MainActivity)((ThemedReactContext) getContext()).getCurrentActivity()).getSupportFragmentManager().findFragmentById(R.id.routific_map_fragment);
        if (frag == null) {
            return;
        }
        routificMapboxFragment = (RoutificMapboxFragment)frag;
        routificMapboxFragment.setContext((ThemedReactContext)getContext());
        routificMapboxFragment.setParent(this);
        routificMapboxFragment.startNavigation();
    }

    public FragmentManager getCurrentFragmentManager() {
        return ((MainActivity)((ThemedReactContext) getContext()).getCurrentActivity()).getSupportFragmentManager();
    }

    public RoutificNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RoutificNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
