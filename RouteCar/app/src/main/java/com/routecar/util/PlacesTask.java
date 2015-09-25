package com.routecar.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by chandanj on 9/24/2015.
 */
public class PlacesTask extends AsyncTask<String, Void, String>{

ParserTask parserTask;
    @Override
    protected String doInBackground(String... place) {
        // For storing data from web service
        String data = "";

        // Obtain browser key from https://code.google.com/apis/console
        String key = "key=AIzaSyBLoCM-QA5elaGyRH23hDqK96FY-AMbFLA";

        String input="";

        try {
            input = "input=" + URLEncoder.encode(place[0], "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        // place type to be searched
        String types = "types=geocode";

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = input+"&"+types+"&"+sensor+"&"+key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

        try{
            // Fetching the data from we service
            data = downloadUrl(url);
        }catch(Exception e){
            Log.d("Background Task", e.toString());
        }
        return data;
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // Creating ParserTask
        parserTask = new ParserTask();

        // Starting Parsing the JSON string returned by Web Service
        parserTask.execute(result);
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
