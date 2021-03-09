import {requireNativeComponent} from "react-native";
import React from 'react';

let NativeMapView = requireNativeComponent('MapboxView');

export const MapView = () => {
    return (
        <>
            <NativeMapView
                style={{ position: 'absolute', bottom: 30, left: 0, height: '100%', width: '100%', border: '1px solid black'}}
            />
        </>

    )
};
