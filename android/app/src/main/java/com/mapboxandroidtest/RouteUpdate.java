package com.mapboxandroidtest;

import com.mapbox.api.directions.v5.models.BannerInstructions;

import java.util.List;

public class RouteUpdate {
   public double remainingRouteDistance;
   public double remainingRouteDuration;
   public double remainingStepDistance;
   public double remainingStepDuration;
   public List<BannerInstructions> instructionsBanner;
   public List<BannerInstructions> upcomingInstructionsBanner;
   RouteUpdate(double remainingRouteDistance,
               double remainingRouteDuration,
               double remainingStepDistance,
               double remainingStepDuration,
               List<BannerInstructions> instructionsBanner,
               List<BannerInstructions> upcomingInstructionsBanner) {
       this.remainingRouteDistance = Double.isNaN(remainingRouteDistance) ? 0 : remainingRouteDistance;
       this.remainingRouteDuration = remainingRouteDuration;
       this.remainingStepDistance = Double.isNaN(remainingStepDistance) ? 0 : remainingStepDistance;
       this.remainingStepDuration = remainingStepDuration;
       this.instructionsBanner = instructionsBanner;
       this.upcomingInstructionsBanner = upcomingInstructionsBanner;
   }
}
