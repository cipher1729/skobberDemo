package com.routecar.util;

import com.routecar.MapActivity;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.positioner.SKPosition;

/**
 * Created by cipher1729 on 9/27/2015.
 *
 */
/*
    Get current position from some kind of cache
*/

public class MapCenter implements  Runnable{

    SKMapSurfaceView mapView;
    public  MapCenter(SKMapSurfaceView mapView)
    {
        this.mapView= mapView;
    }
    @Override
    public void run() {
        //wait for 3 seconds. Then try to center map to current location. Else center map to a default coordinate.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(MapActivity.currentPosition!=null)
        {
            mapView.centerMapOnCurrentPositionSmooth(17, 500);
        }
        else
        {
            //center Map on a default coordinate
            mapView.centerMapOnPosition(new SKCoordinate(-111.841250200,33.306160500));
        }
    }
}
