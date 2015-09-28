package com.routecar.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.skobbler.ngx.SKCoordinate;

import java.io.IOException;
import java.util.List;

/**
 * Created by cipher1729 on 9/27/2015.
 */
public class Helper {

    public static SKCoordinate convertStringToCoord(String location, Context context)
    {   Geocoder geocoder;
        geocoder = new Geocoder(context);
        float latitude=0.0f,longitude=0.0f;
        try {
            List<Address> addresses = geocoder.getFromLocationName(location, 3);
            if(addresses.size()!=0)
            {
                latitude= (float)addresses.get(0).getLatitude();
                longitude= (float)addresses.get(0).getLongitude();
            }
            else
            {
                latitude=0.0f;
                longitude=0.0f;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SKCoordinate(longitude, latitude);
    }
}
