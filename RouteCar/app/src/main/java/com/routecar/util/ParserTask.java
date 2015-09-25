package com.routecar.util;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.SimpleAdapter;

import com.routecar.MapActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by chandanj on 9/24/2015.
 */
public class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{
    JSONObject jObject;
    @Override
    protected List<HashMap<String, String>> doInBackground(String... jsonData) {
        List<HashMap<String, String>> places = null;

        PlaceJSONParser placeJsonParser = new PlaceJSONParser();

        try{
            jObject = new JSONObject(jsonData[0]);

            // Getting the parsed data as a List construct
            places = placeJsonParser.parse(jObject);

        }catch(Exception e){
            Log.d("Exception", e.toString());
        }
        return places;
    }

    @Override
    protected void onPostExecute(List<HashMap<String, String>> result) {



        MapActivity.list = result;
        // Creating a SimpleAdapter for the AutoCompleteTextView
        /*
        SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);

        // Setting the adapter
        atvPlaces.setAdapter(adapter);
        */
    }
}
