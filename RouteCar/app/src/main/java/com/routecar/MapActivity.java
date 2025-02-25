/*
Need to set simMode to false somewhere
Test the skout mode
Figure out way to include current location
 */
package com.routecar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.routecar.UI.CustomAutoCompleteTextView;
import com.routecar.application.DemoApplication;
import com.routecar.util.DemoUtils;
import com.routecar.util.Helper;
import com.routecar.util.PlacesTask;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.routing.SKRouteInfo;
import com.skobbler.ngx.routing.SKRouteJsonAnswer;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.routing.SKViaPoint;
import com.skobbler.ngx.sdktools.navigationui.SKToolsAdvicePlayer;
import com.skobbler.ngx.util.SKLogging;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity implements SKMapSurfaceListener, SKCurrentPositionListener, SKRouteListener, SKNavigationListener{

//app local variables go here
    private SKMapSurfaceView mapView;
    private SKMapViewHolder mapViewGroup;
    private SKCurrentPositionProvider currentPositionProvider;
    private SKPosition currentPosition;
    DemoApplication app;
    AutoCompleteTextView fromTextView, toTextView;
    PlacesTask placesTask;
    public static List<HashMap<String,String>> list;
    Button navigateBtn, positionMeButton, simulateBtn;
    public float fromLat, fromLong, toLat, toLong;
    private Integer cachedRouteId;
    private boolean shouldCacheTheNextRoute, navigationInProgress=false, simMode=false;
    private static final String TAG = "MapActivity";
    List<CustomAutoCompleteTextView> waypointList;
    Context context;

    //IDs for textViews
    int textViewCount=0;

    //UI
    ViewGroup linearLayoutView;
    LayoutInflater inflater;
    View textView;


    private enum MapAdvices {
        TEXT_TO_SPEECH, AUDIO_FILES
    }
    private TextToSpeech textToSpeechEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SKMaps.getInstance().initializeSKMaps(this, null);
        DemoUtils.initializeLibrary(MapActivity.this);
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.layout1,null);
        setContentView(view);

        //add from and to Views
        linearLayoutView = (ViewGroup)findViewById(R.id.rootLinearLayout);
        textView = inflater.inflate(R.layout.pathview, null);
        textView.setId(textViewCount);
        linearLayoutView.addView(textView);
        fromTextView = (CustomAutoCompleteTextView)linearLayoutView.findViewById(textViewCount).findViewById(R.id.waypointId);
        fromTextView.setHint("From");
        fromTextView.setThreshold(4);
        View plusSignView = linearLayoutView.findViewById(textViewCount).findViewById(R.id.plusSignId);
        plusSignView.setVisibility(View.INVISIBLE);
        //increment ID for toTextView
        textViewCount++;
        textView= inflater.inflate(R.layout.pathview, null);
        textView.setId(textViewCount);
        linearLayoutView.addView(textView);
        toTextView = (CustomAutoCompleteTextView)linearLayoutView.findViewById(textViewCount).findViewById(R.id.waypointId);
        toTextView.setHint("To");
        toTextView.setThreshold(4);
        setOnClickListenerForPlus(linearLayoutView.findViewById(textViewCount).findViewById(R.id.plusSignId));


        app = (DemoApplication) getApplication();
        mapViewGroup = (SKMapViewHolder) findViewById(R.id.view_group_map);
        mapViewGroup.setMapSurfaceListener(MapActivity.this);

        //button
        navigateBtn = (Button)findViewById(R.id.navigateBtn);
        positionMeButton= (Button)findViewById(R.id.position_me_button);
        simulateBtn = (Button)findViewById(R.id.simulateBtn);

        //inits
        waypointList = new ArrayList<>();

        //current Position
        currentPositionProvider = new SKCurrentPositionProvider(this);
        currentPositionProvider.setCurrentPositionListener(this);
        currentPositionProvider.requestLocationUpdates(DemoUtils.hasGpsModule(this), DemoUtils.hasNetworkModule(this), false);


        context = MapActivity.this;


        addGUIListeners();
    }

    private void addGUIListeners()
    {
        /*fromTextView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                toTextView.setVisibility(View.GONE);
            }
        });*/
        fromTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
                String[] from = new String[]{"description"};
                int[] to = new int[]{android.R.id.text1};
                if (list != null) {
                    SimpleAdapter adapter = new SimpleAdapter(MapActivity.this, list, android.R.layout.simple_list_item_1, from, to);
                    fromTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //remove the from textView
               linearLayoutView.findViewById(textViewCount).setVisibility(View.GONE);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        fromTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                linearLayoutView.findViewById(textViewCount).setVisibility(View.VISIBLE);
            }
        });

        toTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
                String[] from = new String[]{"description"};
                int[] to = new int[]{android.R.id.text1};
                if (list != null) {
                    SimpleAdapter adapter = new SimpleAdapter(MapActivity.this, list, android.R.layout.simple_list_item_1, from, to);
                    toTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //remove the To TextView
                linearLayoutView.findViewById(0).setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("", "");
            }
        });


       toTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                linearLayoutView.findViewById(0).setVisibility(View.VISIBLE);
            }
        });

        navigateBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (navigationInProgress) {
                    // stop navigation if ongoing
                    stopNavigation();
                   // navigateBtn.setVisibility(View.VISIBLE);
                   // simulateBtn.setVisibility(View.VISIBLE);
                    showAllUI();
                } else {

                        //get To coordinates from editText
                        SKCoordinate skCoordinate= Helper.convertStringToCoord(toTextView.getText().toString(), context);
                        toLat= (float)skCoordinate.getLatitude();
                        toLong= (float)skCoordinate.getLongitude();

                        //got From coordinates from editText
                        skCoordinate= Helper.convertStringToCoord(fromTextView.getText().toString(), context);
                        fromLat= (float)skCoordinate.getLatitude();
                        fromLong= (float)skCoordinate.getLongitude();

                   // navigateBtn.setVisibility(View.GONE);
                   // simulateBtn.setVisibility(View.GONE);
                    hideAllUI();

                    SKRouteManager.getInstance().clearCurrentRoute();
                    launchRouteCalculation(new SKCoordinate(fromLong, fromLat), new SKCoordinate(toLong, toLat));
                    new AlertDialog.Builder(MapActivity.this)
                            .setMessage("Choose the advice type")
                            .setCancelable(false)
                            .setPositiveButton("Scout audio", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    navigateBtn.setText(getResources().getString(R.string.stop_navigation));
                                    setAdvicesAndStartNavigation(MapAdvices.AUDIO_FILES);
                                }
                            })
                            .setNegativeButton("Text to speech", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (textToSpeechEngine == null) {
                                        Toast.makeText(MapActivity.this, "Initializing TTS engine",
                                                Toast.LENGTH_LONG).show();
                                        textToSpeechEngine = new TextToSpeech(MapActivity.this,
                                                new TextToSpeech.OnInitListener() {
                                                    @Override
                                                    public void onInit(int status) {
                                                        if (status == TextToSpeech.SUCCESS) {
                                                            int result = textToSpeechEngine.setLanguage(Locale.ENGLISH);
                                                            if (result == TextToSpeech.LANG_MISSING_DATA || result ==
                                                                    TextToSpeech.LANG_NOT_SUPPORTED) {
                                                                Toast.makeText(MapActivity.this,
                                                                        "This Language is not supported",
                                                                        Toast.LENGTH_LONG).show();
                                                            }
                                                        } else {
                                                            Toast.makeText(MapActivity.this, getString(R.string.text_to_speech_engine_not_initialized),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                        navigateBtn.setText(getResources().getString(R.string
                                                                .stop_navigation));
                                                        setAdvicesAndStartNavigation(MapAdvices.TEXT_TO_SPEECH);
                                                    }
                                                });
                                    } else {
                                        navigateBtn.setText(getResources().getString(R.string.stop_navigation));
                                        setAdvicesAndStartNavigation(MapAdvices.TEXT_TO_SPEECH);
                                    }

                                }
                            })
                            .show();

                }
            }
        });


        simulateBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                simMode= true;
                if (navigationInProgress) {
                    // stop navigation if ongoing
                    stopNavigation();
                    //navigateBtn.setVisibility(View.VISIBLE);
                   // simulateBtn.setVisibility(View.VISIBLE);
                    positionMeButton.setVisibility(View.VISIBLE);
                    linearLayoutView.findViewById(0).setVisibility(View.VISIBLE);
                    linearLayoutView.findViewById(textViewCount).setVisibility(View.VISIBLE);
                }
                else {
                    //navigateBtn.setVisibility(View.GONE);
                   // simulateBtn.setVisibility(View.GONE);
                    positionMeButton.setVisibility(View.GONE);
                    linearLayoutView.findViewById(textViewCount).setVisibility(View.GONE);
                    linearLayoutView.findViewById(0).setVisibility(View.GONE);
                    SKRouteManager.getInstance().clearCurrentRoute();


                    launchRouteCalculation(new SKCoordinate(fromLong, fromLat), new SKCoordinate(-111.651302000,35.198283600));

                    new AlertDialog.Builder(MapActivity.this)
                            .setMessage("Choose the advice type")
                            .setCancelable(false)
                            .setPositiveButton("Scout audio", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    simulateBtn.setText("Stop simulation");
                                    setAdvicesAndStartNavigation(MapAdvices.AUDIO_FILES);
                                }
                            })
                            .setNegativeButton("Text to speech", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (textToSpeechEngine == null) {
                                        Toast.makeText(MapActivity.this, "Initializing TTS engine",
                                                Toast.LENGTH_LONG).show();
                                        textToSpeechEngine = new TextToSpeech(MapActivity.this,
                                                new TextToSpeech.OnInitListener() {
                                                    @Override
                                                    public void onInit(int status) {
                                                        if (status == TextToSpeech.SUCCESS) {
                                                            int result = textToSpeechEngine.setLanguage(Locale.ENGLISH);
                                                            if (result == TextToSpeech.LANG_MISSING_DATA || result ==
                                                                    TextToSpeech.LANG_NOT_SUPPORTED) {
                                                                Toast.makeText(MapActivity.this,
                                                                        "This Language is not supported",
                                                                        Toast.LENGTH_LONG).show();
                                                            }
                                                        } else {
                                                            Toast.makeText(MapActivity.this, getString(R.string.text_to_speech_engine_not_initialized),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                        simulateBtn.setText("Stop simulation");
                                                        setAdvicesAndStartNavigation(MapAdvices.TEXT_TO_SPEECH);
                                                    }
                                                });
                                    } else {
                                        simulateBtn.setText("Stop simulation");
                                        setAdvicesAndStartNavigation(MapAdvices.TEXT_TO_SPEECH);
                                    }

                                }
                            })
                            .show();

                }
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

    private void hideAllUI()
    {
        for(int i=0;i<=textViewCount;i++)
        {
            linearLayoutView.findViewById(i).setVisibility(View.GONE);
        }
        positionMeButton.setVisibility(View.GONE);
    }

    private void showAllUI()
    {
        for(int i=0;i<=textViewCount;i++)
        {
            linearLayoutView.findViewById(i).setVisibility(View.VISIBLE);
        }
        positionMeButton.setVisibility(View.VISIBLE);
    }
    private void setOnClickListenerForPlus(View v)
    {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView = inflater.inflate(R.layout.pathview,null);

                //increment count
                textViewCount++;
                textView.setId(textViewCount);
                linearLayoutView.addView(textView);
                //add the listener to this textView
                CustomAutoCompleteTextView customAutoCompleteTextView = (CustomAutoCompleteTextView)linearLayoutView.findViewById(textViewCount).findViewById(R.id.waypointId);
                customAutoCompleteTextView.setHint("Waypoint");
                customAutoCompleteTextView.setThreshold(4);
                setTextViewChangedListener(customAutoCompleteTextView);
                setOnClickListenerForPlus(linearLayoutView.findViewById(textViewCount).findViewById(R.id.plusSignId));

                //remove previous textView's plus sign
                view.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void setTextViewChangedListener(final CustomAutoCompleteTextView customAutoCompleteTextView)
    {
        customAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
                String[] from = new String[]{"description"};
                int[] to = new int[]{android.R.id.text1};
                if (list != null) {
                    SimpleAdapter adapter = new SimpleAdapter(MapActivity.this, list, android.R.layout.simple_list_item_1, from, to);
                    customAutoCompleteTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //linearLayoutView.findViewById(0).setVisibility(View.GONE);

                //make other textviews invisible
                for(int i=0;i<=textViewCount;i++)
                {
                    if (!(linearLayoutView.findViewById(i).findViewById(R.id.waypointId).hasFocus()))
                    {
                        linearLayoutView.findViewById(i).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        customAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //linearLayoutView.findViewById(0).setVisibility(View.VISIBLE);
                //Need to do
                for(int i=0;i<=textViewCount;i++)
                {
                        linearLayoutView.findViewById(i).setVisibility(View.VISIBLE);
                }
            }
        });

    }
    @Override
    public void onDestinationReached() {
        Toast.makeText(MapActivity.this, "Destination reached", Toast.LENGTH_SHORT).show();
        // clear the map when reaching destination
        clearMap();
    }

    @Override
    public void onSignalNewAdviceWithInstruction(String instruction) {
        SKLogging.writeLog(TAG, " onSignalNewAdviceWithInstruction " + instruction, Log.DEBUG);
        textToSpeechEngine.speak(instruction, TextToSpeech.QUEUE_ADD, null);
    }

    @Override
    public void onSignalNewAdviceWithAudioFiles(String[] audioFiles, boolean b) {
        // a new navigation advice was received
        SKLogging.writeLog(TAG, " onSignalNewAdviceWithAudioFiles " + Arrays.asList(audioFiles), Log.DEBUG);
        SKToolsAdvicePlayer.getInstance().playAdvice(audioFiles, SKToolsAdvicePlayer.PRIORITY_NAVIGATION);
    }

    @Override
    public void onSpeedExceededWithAudioFiles(String[] strings, boolean b) {

    }

    @Override
    public void onSpeedExceededWithInstruction(String s, boolean b) {

    }

    @Override
    public void onUpdateNavigationState(SKNavigationState skNavigationState) {

    }

    @Override
    public void onReRoutingStarted() {

    }

    @Override
    public void onFreeDriveUpdated(String s, String s1, SKNavigationState.SKStreetType skStreetType, double v, double v1) {

    }

    @Override
    public void onViaPointReached(int i) {

    }

    @Override
    public void onVisualAdviceChanged(boolean b, boolean b1, SKNavigationState skNavigationState) {

    }

    @Override
    public void onTunnelEvent(boolean b) {

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

        if (!navigationInProgress) {
            mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE);
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
        if (textToSpeechEngine != null) {
            textToSpeechEngine.stop();
            textToSpeechEngine.shutdown();
        }
    }

    @Override
    public void onCurrentPositionUpdate(SKPosition skPosition) {
        currentPosition = skPosition;
         if (mapView != null) {
            mapView.reportNewGPSPosition(this.currentPosition);
        }
    }

    @Override
    public void onRouteCalculationFailed(SKRoutingErrorCode skRoutingErrorCode) {
        shouldCacheTheNextRoute = false;
        Toast.makeText(MapActivity.this, getResources().getString(R.string.route_calculation_failed),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAllRoutesCompleted() {
        if (shouldCacheTheNextRoute) {
            shouldCacheTheNextRoute = false;
            SKRouteManager.getInstance().saveRouteToCache(cachedRouteId);
        }
        SKRouteManager.getInstance().zoomToRoute(1, 1, 8, 8, 8, 8);
    }

    @Override
    public void onServerLikeRouteCalculationCompleted(SKRouteJsonAnswer skRouteJsonAnswer) {

    }

    @Override
    public void onOnlineRouteComputationHanging(int i) {

    }

    @Override
    public void onRouteCalculationCompleted(SKRouteInfo routeInfo) {
        // select the current route (on which navigation will run)
        List<SKCoordinate> points = new ArrayList<>();
        points = SKRouteManager.getInstance().getCoordinatesForRoute(routeInfo.getRouteID());
        SKRouteManager.getInstance().setCurrentRouteByUniqueId(routeInfo.getRouteID());
        SKRouteManager.getInstance().zoomToRoute(1, 1, 8, 8, 8, 8);
    }

    //HELPER
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

    /**
     * Launches a single route calculation
     */
    private void launchRouteCalculation(SKCoordinate startPoint, SKCoordinate destinationPoint) {
        clearRouteFromCache();
        // get a route object and populate it with the desired properties
        SKRouteSettings route = new SKRouteSettings();


        // set start and destination points
        route.setStartCoordinate(new SKCoordinate(currentPosition.getLongitude(),currentPosition.getLatitude()));
        route.setDestinationCoordinate(destinationPoint);

        //handle waypoints
        List<SKViaPoint> viaList = new ArrayList<>();
        int viaPointNumber=0;

        for(int i=2;i<=textViewCount;i++)
        {
            AutoCompleteTextView autoCompleteTextView= (AutoCompleteTextView)linearLayoutView.findViewById(i).findViewById(R.id.waypointId);
            String locationString = autoCompleteTextView.getText().toString();
            SKCoordinate viaLocation= Helper.convertStringToCoord(locationString,context);
            viaList.add(new SKViaPoint(viaPointNumber,viaLocation));
            SKAnnotation annotation = new SKAnnotation(0);
            annotation.setUniqueID(viaPointNumber++);
            annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_MARKER);
            annotation.setLocation(viaLocation);
            annotation.setMininumZoomLevel(5);
            mapView.addAnnotation(annotation,SKAnimationSettings.ANIMATION_NONE);

        }
        route.setViaPoints(viaList);


        // set the number of routes to be calculated
        route.setNoOfRoutes(1);
        // set the route mode
        route.setRouteMode(SKRouteSettings.SKRouteMode.CAR_FASTEST);
        // set whether the route should be shown on the map after it's computed
        route.setRouteExposed(true);
        // set the route listener to be notified of route calculation
        // events
        SKRouteManager.getInstance().setRouteListener(this);
        // pass the route to the calculation routine
        SKRouteManager.getInstance().calculateRoute(route);
    }

    public void clearRouteFromCache() {
        SKRouteManager.getInstance().clearAllRoutesFromCache();
        cachedRouteId = null;
    }

    /**
     * Setting the audio advices
     */
    private void setAdvicesAndStartNavigation(MapAdvices currentMapAdvices) {
        final SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
        advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
        advisorSettings.setAdvisorConfigPath(app.getMapResourcesDirPath() + "/Advisor");
        advisorSettings.setResourcePath(app.getMapResourcesDirPath() + "/Advisor/Languages");
        advisorSettings.setAdvisorVoice("en");
        switch (currentMapAdvices) {
            case AUDIO_FILES:
                advisorSettings.setAdvisorType(SKAdvisorSettings.SKAdvisorType.AUDIO_FILES);
                break;
            case TEXT_TO_SPEECH:
                advisorSettings.setAdvisorType(SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
                break;
        }
        SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
        launchNavigation();

    }

    /**
     * Launches a navigation on the current route
     */
    private void launchNavigation() {

        // get navigation settings object
        SKNavigationSettings navigationSettings = new SKNavigationSettings();
        // set the desired navigation settings
        if(simMode== false) navigationSettings.setNavigationType(SKNavigationSettings.SKNavigationType.REAL);
        else navigationSettings.setNavigationType(SKNavigationSettings.SKNavigationType.SIMULATION);
        navigationSettings.setPositionerVerticalAlignment(-0.25f);
        navigationSettings.setShowRealGPSPositions(false);
        // get the navigation manager object
        SKNavigationManager navigationManager = SKNavigationManager.getInstance();
        navigationManager.setMapView(mapView);
        applySettingsOnMapView();

        // set listener for navigation events
        navigationManager.setNavigationListener(this);

        // start navigating using the settings
        navigationManager.startNavigation(navigationSettings);

        navigationInProgress = true;
    }


    private void stopNavigation() {
        navigationInProgress = false;
        if (textToSpeechEngine != null && !textToSpeechEngine.isSpeaking()) {
            textToSpeechEngine.stop();
        }

        SKNavigationManager.getInstance().stopNavigation();
        if(simMode==false )navigateBtn.setText("Navigate");
        else simulateBtn.setText("Simulate");
    }

    /**
     * Clears the map
     */
    private void clearMap() {

        //navigateBtn.setVisibility(View.VISIBLE);
        //simulateBtn.setVisibility(View.VISIBLE);
        SKRouteManager.getInstance().clearCurrentRoute();
         mapView.deleteAllAnnotationsAndCustomPOIs();
         if (navigationInProgress) {
                    // stop navigation if ongoing
         stopNavigation();
         }

        positionMeButton.setVisibility(View.VISIBLE);

    }




}
