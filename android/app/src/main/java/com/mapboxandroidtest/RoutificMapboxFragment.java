package com.mapboxandroidtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.facebook.react.uimanager.ThemedReactContext;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteStepProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.arrival.ArrivalObserver;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.history.ReplayEventBase;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper;
import com.mapbox.navigation.core.reroute.RerouteController;
import com.mapbox.navigation.core.reroute.RerouteState;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.TripSessionState;
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.camera.DynamicCamera;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.map.NavigationMapboxMap;
import com.mapbox.navigation.ui.puck.DefaultMapboxPuckDrawableSupplier;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Cache;

public class RoutificMapboxFragment extends Fragment implements
        OnMapReadyCallback,
        RouteProgressObserver,
        RoutesRequestCallback,
        LocationObserver
{

    private static final int LOCATION_PERMISSIONS_REQUEST = 3232;
    private static final String ROUTIFIC_MAPBOX_FRAGMENT_TAG = "ROUTIFIC_MAPBOX_FRAG";
    public MapView mapView;
    public MapboxMap mapboxMap;
    private MapboxNavigation mapboxNavigation;
    private NavigationMapboxMap navigationMapboxMap;
    public ThemedReactContext context;
    public Context activityContext;
    public RoutificNavigationView parent;
    private MapboxReplayer mapboxReplayer = new MapboxReplayer();
    private DirectionsRoute currentRoute;
    private List<? extends DirectionsRoute> routes = Collections.emptyList();
    private boolean mapReady = false;
    private boolean simulate = false;
    private boolean mapInitializing = false;
    private FusedLocationProviderClient locationProvider;
    private LocationEngine locationEngine;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "OnCreateView");
        return inflater.inflate(R.layout.routific_map_view, container, true);
    }

    public void setContext(ThemedReactContext context) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "setContext");
        this.context = context;
    }

    public void setParent(RoutificNavigationView view) {
        this.parent = view;
    }

    public void overview(int[] args) {
        if (navigationMapboxMap != null) {
            Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "overview");
            navigationMapboxMap.showRouteOverview(args);
        }
    }

    public void resume() {
        if (navigationMapboxMap != null) {
            Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "resume");
            navigationMapboxMap.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onAttach");
        super.onAttach(context);
        this.activityContext = context;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onViewCreated");
        initializeMapView();
        locationProvider = LocationServices.getFusedLocationProviderClient(this.activityContext);

    }

    private void initializeMapView() {
        View fragmentView = getView();
        if (fragmentView != null) {
            if (!mapInitializing && !mapReady) {
                mapInitializing = true;
                mapView = fragmentView.findViewById(R.id.routific_navigatio_map_view);
                mapView.getMapAsync(this);
            }
        }
    }

    @Override
    public void onDetach() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onDetach");
        super.onDetach();

    }

    @Override
    public void onResume() {
        initializeMapView();
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onResume");
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onPause");
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onDestroy");
        navigationMapboxMap = null;
        if (ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mapboxMap.getLocationComponent().setLocationComponentEnabled(false);
        }
        mapboxMap = null;
        if (mapboxReplayer != null) {
            mapboxReplayer.unregisterObservers();
            mapboxReplayer.finish();
        }
        if (mapboxNavigation != null) mapboxNavigation.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        mapReady = false;
        mapInitializing = false;
        super.onDestroy();
    }

    @Override
    public void onStart() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onStart");
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onStop");
        mapView.onStop();
        super.onStop();
    }

//    @Nullable
//    private LocationEngine initializeLocationEngineFrom() {
//        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "initializeLocationEngineFrom");
//        ReplayLocationEngine replayLocationEngine = new ReplayLocationEngine(mapboxReplayer);
//        final Point lastLocation = getOriginOfRoute(currentRoute);
//        ReplayEventBase replayEventOrigin = ReplayRouteMapper.mapToUpdateLocation(0.0, lastLocation);
//        mapboxReplayer.pushEvents(Collections.singletonList(replayEventOrigin));
//        mapboxReplayer.play();
//        return replayLocationEngine;
//    }

    private Point getOriginOfRoute(@NonNull final DirectionsRoute directionsRoute) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "getOriginOfRoute");
        return PolylineUtils.decode(directionsRoute.geometry(), 6).get(0);
    }

    public void startNavigation() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "startNavigation");
        if (mapView != null) {
            if (mapReady) {
                setupMap();
            } else {
                initializeMapView();
            }
        } else {
            mapView = this.getView().findViewById(R.id.routific_navigatio_map_view);
            mapView.getMapAsync(this);
        }
    }


    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this.activityContext);

        LocationEngineRequest request = new LocationEngineRequest.Builder(10000L)
                .setFastestInterval(5000L)
                .setDisplacement(1)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build();

//        locationEngine.requestLocationUpdates(request, this, Looper.getMainLooper());
    }

    private void initializeMapboxComponents() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "initializeMapboxComponents");
        if (!mapReady) {
            initializeMapView();
            return;
        }
        try {
            if (navigationMapboxMap == null) {
                navigationMapboxMap = new NavigationMapboxMap(this.mapView, mapboxMap, this, true);
                navigationMapboxMap.setPuckDrawableSupplier(new DefaultMapboxPuckDrawableSupplier());
                navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
                navigationMapboxMap.retrieveMap().getLocationComponent().setRenderMode(RenderMode.GPS);
                navigationMapboxMap.retrieveMap().getLocationComponent().setCameraMode(CameraMode.TRACKING_GPS);
                navigationMapboxMap.setCamera(new DynamicCamera(navigationMapboxMap.retrieveMap()));
                navigationMapboxMap.retrieveMap().getLocationComponent().tiltWhileTracking(45, 1);
                navigationMapboxMap.retrieveMap().getLocationComponent().zoomWhileTracking(16, 1);
            }

            LocationEngineRequest request = new LocationEngineRequest.Builder(10000L)
                    .setFastestInterval(5000L)
                    .setDisplacement(1)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build();
            if (mapboxNavigation == null) {
                initLocationEngine();
                NavigationOptions navigationOptions = MapboxNavigation
                        .defaultNavigationOptionsBuilder(this.context, this.context.getString(R.string.mapbox_access_token))
                        .locationEngineRequest(request)
                        .locationEngine(locationEngine)
                        .build();
                mapboxNavigation = new MapboxNavigation(navigationOptions);
                if (!simulate) {
                    registerNavigationListeners();
                    navigationMapboxMap.addProgressChangeListener(mapboxNavigation);
                }

            }

        } catch (Exception e) {
            Log.i("error", e.toString());
        }
    }

    private void getInitialLocation() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "getInitialLocation");
        if (ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            this.requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_PERMISSIONS_REQUEST);
            return;
        }
    }

    public void setupMap() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "setupMap");
        initializeMapboxComponents();
        fetchRoute();
    }

    public void registerNavigationListeners() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "registerNavigationListeners");
//        mapboxNavigation.registerLocationObserver(this);
    }

    public void fetchRoute() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "fetchRoute");
        LatLng origin = new LatLng(49.257670, -122.779730);
        LatLng destination = new LatLng(49.284680, -123.111890);
        mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                        .accessToken(this.context.getString(R.string.mapbox_access_token))
                        .baseUrl(RouteUrl.BASE_URL)
                        .user(RouteUrl.PROFILE_DEFAULT_USER)
                        .requestUuid("Test" + (Math.random() * (1000 - 1 + 1) + 1))
                        .coordinates(Arrays.asList(
                                Point.fromLngLat(origin.getLongitude(), origin.getLatitude()),
                                Point.fromLngLat(destination.getLongitude(), destination.getLatitude()))
                        )
                        .steps(true)
                        .annotations("congestion,distance")
                        .continueStraight(false)
                        .geometries("polyline6")
                        .voiceUnits("metric")
                        .alternatives(true)
                        .voiceInstructions(true)
                        .bannerInstructions(true)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                this
        );
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onMapReady");
        if (this.context == null) {
            return;
        }

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/routific/ckh3qgpkh017i19lvnm8cpvlz"), style -> {
            mapReady = true;
            mapInitializing = false;
            startNavigation();
        });
        this.mapboxMap = mapboxMap;

//        this.mapboxMap.setStyle(new Style.Builder(), style -> {
//            mapReady = true;
//            mapInitializing = false;
//            startNavigation();
//        });

    }


    @Override
    public void onRoutesReady(@NotNull List<? extends DirectionsRoute> list) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRoutesReady");
        routes = list;
        startLiveNavigation();
        Log.i("error", "");

    }

    public void startLiveNavigation() {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "startLiveNavigation");
        if (routes.size() == 0) {
            return;
        }
        if  (navigationMapboxMap == null) {
            initializeMapView();
        }
        if (ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.activityContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_PERMISSIONS_REQUEST);
        }
        mapboxNavigation.setRoutes(routes);
        currentRoute = routes.get(0);

//        if (simulate) {
//            NavigationOptions navigationOptions = MapboxNavigation
//                    .defaultNavigationOptionsBuilder(this.context, this.context.getString(R.string.mapbox_access_token))
//                    .locationEngine(initializeLocationEngineFrom())
//                    .build();
//            mapboxNavigation = new MapboxNavigation(navigationOptions);
//            navigationMapboxMap.addProgressChangeListener(mapboxNavigation);
//            registerNavigationListeners();
//        }

        if (mapboxNavigation != null && navigationMapboxMap != null && parent != null) {
            mapboxNavigation.startTripSession();
            resume();
            navigationMapboxMap.startCamera(currentRoute);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSIONS_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getInitialLocation();
        }
    }

    @Override
    public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRoutesRequestCanceled");
    }

    @Override
    public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRoutesRequestFailure");

    }

    @Override
    public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRouteProgressChanged");
        RouteLegProgress currentLeg = routeProgress.getCurrentLegProgress();
        if (currentLeg != null) {
            RouteStepProgress currentStepProgress = currentLeg.getCurrentStepProgress();
            if (currentStepProgress != null) {
                LegStep currentStep = currentStepProgress.getStep();
                if (currentStep != null) {
                    List<BannerInstructions> bannerInstructions = currentStep.bannerInstructions();
                    if (bannerInstructions != null) {
                        List<BannerInstructions> upcomingInstructionsBanner = currentLeg.getUpcomingStep() == null ? Collections.emptyList() : currentLeg.getUpcomingStep().bannerInstructions();
                        RouteUpdate routeUpdate = new RouteUpdate(
                                currentLeg.getDistanceRemaining(),
                                currentLeg.getDurationRemaining(),
                                currentStepProgress.getDistanceRemaining(),
                                currentStepProgress.getDurationRemaining(),
                                bannerInstructions,
                                upcomingInstructionsBanner
                        );
                        Gson gson = new Gson();
                        String json = gson.toJson(routeUpdate);
                        Log.i("RouteInstructions", json);
                    }
                }
            }
        }
    }

    @Override
    public void onEnhancedLocationChanged(@NotNull Location location, @NotNull List<? extends Location> list) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onEnhancedLocationChanged");
    }

    @Override
    public void onRawLocationChanged(@NotNull Location location) {
        Log.i(ROUTIFIC_MAPBOX_FRAGMENT_TAG, "onRawLocationChanged");
    }
}
