package com.routecar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.routecar.UI.CustomAutoCompleteTextView;
import com.routecar.application.DemoApplication;
import com.routecar.util.DemoUtils;
import com.routecar.util.PlacesTask;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;

import java.nio.charset.MalformedInputException;
import java.util.HashMap;
import java.util.List;

public class MapActivity extends Activity implements SKMapSurfaceListener, SKCurrentPositionListener{
//app local variables go here
//
    private SKMapSurfaceView mapView;
    private SKMapViewHolder mapViewGroup;
    private SKCurrentPositionProvider currentPositionProvider;
    private SKPosition currentPosition;
    DemoApplication app;
    AutoCompleteTextView fromTextView, toTextView;
    PlacesTask placesTask;
    public static List<HashMap<String,String>> list;
    Button navigateBtn, positionMeButton;
    public float fromLat, fromLong, toLat, toLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SKMaps.getInstance().initializeSKMaps(this, null);
        DemoUtils.initializeLibrary(MapActivity.this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.layout1,null);
        setContentView(view);
        app = (DemoApplication) getApplication();
        mapViewGroup = (SKMapViewHolder) findViewById(R.id.view_group_map);
        mapViewGroup.setMapSurfaceListener(MapActivity.this);

        //button
        navigateBtn = (Button)findViewById(R.id.navigateBtn);
        positionMeButton= (Button)findViewById(R.id.position_me_button);

        //textviews
        fromTextView = (CustomAutoCompleteTextView)findViewById(R.id.fromText);
        toTextView = (CustomAutoCompleteTextView)findViewById(R.id.toText);
        fromTextView.setThreshold(4);
        toTextView.setThreshold(4);

        //current Position
        currentPositionProvider = new SKCurrentPositionProvider(this);
        currentPositionProvider.setCurrentPositionListener(this);
        currentPositionProvider.requestLocationUpdates(DemoUtils.hasGpsModule(this), DemoUtils.hasNetworkModule(this), false);

        addGUIListeners();
    }

    private void addGUIListeners()
    {
        fromTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
                String[] from = new String[] {"description"};
                int[] to = new int[] { android.R.id.text1 };
                if(list!=null) {
                    SimpleAdapter adapter = new SimpleAdapter(MapActivity.this, list, android.R.layout.simple_list_item_1, from, to);
                    fromTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        toTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
                String[] from = new String[] {"description"};
                int[] to = new int[] { android.R.id.text1 };
                if(list!=null) {
                    SimpleAdapter adapter = new SimpleAdapter(MapActivity.this, list, android.R.layout.simple_list_item_1, from, to);
                    toTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        navigateBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

       positionMeButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if (mapView != null && currentPosition != null) {
                    mapView.centerMapOnCurrentPositionSmooth(17, 500);
                } else {
                    Toast.makeText(MapActivity.this, getResources().getString(R.string.no_position_available), Toast.LENGTH_SHORT)
                            .show();
                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActionPan() {

    }

    @Override
    public void onActionZoom() {

    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder mapHolder) {
        View chessBackground = findViewById(R.id.chess_board_background);
        chessBackground.setVisibility(View.GONE);
        mapView = mapHolder.getMapSurfaceView();
        applySettingsOnMapView();

        if (currentPosition != null) {
            mapView.reportNewGPSPosition(currentPosition);
        }
    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onDoubleTap(SKScreenPoint point) {
        //mapView.zoomInAt(point);
    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {
       // mapPopup.setVisibility(View.GONE);
    }

    @Override
    public void onRotateMap() {

    }

    @Override
    public void onLongPress(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onInternetConnectionNeeded() {

    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {

    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {

    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {

    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI) {

    }

    @Override
    public void onCompassSelected() {

    }

    @Override
    public void onCurrentPositionSelected() {

    }

    @Override
    public void onObjectSelected(int i) {

    }

    @Override
    public void onInternationalisationCalled(int i) {

    }

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapViewGroup.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapViewGroup.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentPositionProvider.stopLocationUpdates();
        SKMaps.getInstance().destroySKMaps();
    }

    /**
     * Customize the map view
     */
    private void applySettingsOnMapView() {
        mapView.getMapSettings().setMapRotationEnabled(true);
        mapView.getMapSettings().setMapZoomingEnabled(true);
        mapView.getMapSettings().setMapPanningEnabled(true);
        mapView.getMapSettings().setZoomWithAnchorEnabled(true);
        mapView.getMapSettings().setInertiaRotatingEnabled(true);
        mapView.getMapSettings().setInertiaZoomingEnabled(true);
        mapView.getMapSettings().setInertiaPanningEnabled(true);
    }

    @Override
    public void onCurrentPositionUpdate(SKPosition skPosition) {
        currentPosition = skPosition;
        if (mapView != null) {
            mapView.reportNewGPSPosition(this.currentPosition);
        }
    }
}
