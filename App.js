import { StatusBar } from 'expo-status-bar';
import React from 'react';
import { StyleSheet, Text, View, ActivityIndicator } from 'react-native';
import {MapView} from "./MapView";

export default function App() {
  return (
    <View style={styles.container}>

      <MapView
          style={{ position: 'absolute', bottom: 30, left: 0, height: '100%', width: '100%', border: '1px solid black'}}
      />
        <ActivityIndicator size="large" color="#00ff00" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    height: '100%',
    width: '100%'
  },
});
