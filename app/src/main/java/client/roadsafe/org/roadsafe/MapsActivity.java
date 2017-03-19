package client.roadsafe.org.roadsafe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import client.roadsafe.org.roadsafe.client.RoadSafeServiceClient;
import client.roadsafe.org.roadsafe.models.Incident;
import client.roadsafe.org.roadsafe.models.RiskFactor;
import client.roadsafe.org.roadsafe.utils.IntentConstants;
import client.roadsafe.org.roadsafe.utils.NetworkStateUtil;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener, GoogleMap.OnMapClickListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMapLongClickListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    public static final float DEFAULT_ZOOM_LEVEL = 18.0f;
    private static final String TAG = MapsActivity.class.getName();
    private GoogleMap mMap;
    private LocationManager locationManager;
    private Marker currentMarker;
    HashMap<String, Marker> markerHashMap = new HashMap<>();
    private TextView fatalIncidentCount;
    private double previousFatalRisk = 0.0f, previousInjuryRisk = 0.0f, previousPropertyDamageRisk = 0.0f;
    private TextToSpeech tts;
    private Button cmdRefresh;
    private TextView injuredCount;
    private TextView propertyCount;
    private View recommendationContainer;
    private TextView recommendationText;
    private boolean riskFactorComputeInProgress;
    private boolean incidentQueryInProgress;
    private Switch considerTimeOfDay;
    public long lastNag;
    private boolean use_hour = true;
    private View offlineStatusIndicator;
    private String mode;
    private boolean lockView = true;
    private TextView riskFactorText;
    private TextView current_hour;
    private Switch trackLocationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        cmdRefresh = (Button) findViewById(R.id.cmd_refresh);
        cmdRefresh.setOnClickListener(this);

        offlineStatusIndicator = findViewById(R.id.offline_status_indicator);
        fatalIncidentCount = (TextView) findViewById(R.id.fatal_count);
        injuredCount = (TextView) findViewById(R.id.injured_count);
        propertyCount = (TextView) findViewById(R.id.property_damage);
        recommendationText = (TextView) findViewById(R.id.recommendation_text);
        riskFactorText = (TextView) findViewById(R.id.risk_factor_indicator);
        current_hour = (TextView)findViewById(R.id.hour_indicator);

        considerTimeOfDay = (Switch) findViewById(R.id.time_of_day);
        trackLocationSwitch = (Switch) findViewById(R.id.track_current_location_switch);

        considerTimeOfDay.setOnCheckedChangeListener(this);
        trackLocationSwitch.setOnCheckedChangeListener(this);

        //containers
        recommendationContainer = findViewById(R.id.card_recommendation);
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER, this, this.getMainLooper());
        }

        Intent intent = getIntent();
        this.mode = intent.getStringExtra(IntentConstants.MODE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) return;

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Location location = getLastBestLocation();
        if (location != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM_LEVEL));
            // Add a marker in Sydney and move the camera
            renderAllMarkers(location);
        }

        mMap.setOnMapClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onLocationChanged(final Location location) {
        renderAllMarkers(location);
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if (lockView) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }

        queryRiskFactor(currentLocation);
    }

    private void queryRiskFactor(final LatLng currentLocation) {
        if (NetworkStateUtil.isNetworkAvailable(this)) {
            offlineStatusIndicator.setVisibility(View.GONE);
            AsyncTask<Void, Void, RiskFactor> computeRiskFactor = new AsyncTask<Void, Void, RiskFactor>() {


                public int hourOfDay;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    MapsActivity.this.riskFactorComputeInProgress = true;
                    this.hourOfDay = new DateTime().toDateTime(DateTimeZone.UTC).getHourOfDay();
                }

                @Override
                protected RiskFactor doInBackground(Void... voids) {
                    RoadSafeServiceClient client = new RoadSafeServiceClient();

                    float radius = 0.5f;

                    if (mode.equals(IntentConstants.MODE_PEDESTRIAN)) {
                        radius = 0.2f;
                    }

                    if (MapsActivity.this.use_hour) {
                        DateTime dt = new DateTime();
                        return client.getRiskFactor(currentLocation.longitude, currentLocation.latitude, radius, hourOfDay);
                    } else {
                        return client.getRiskFactor(currentLocation.longitude, currentLocation.latitude, radius, -1);
                    }
                }

                @Override
                protected void onPostExecute(RiskFactor riskFactor) {
                    super.onPostExecute(riskFactor);
                    MapsActivity.this.riskFactorComputeInProgress = false;

                    if (MapsActivity.this.use_hour) {
                        current_hour.setText("Hour " + hourOfDay);
                    } else {
                        current_hour.setText("Not Filtering by hours " + hourOfDay);
                    }

                    if (riskFactor != null) {
                        long currentTime = System.currentTimeMillis();
                        if (riskFactor.getInjuryRiskFactor() >= 1 && riskFactor.getFatalRiskFactor() > previousFatalRisk) {
                            if (currentTime - lastNag > 1000 * 60 * 10) {
                                lastNag = currentTime;
                                speak("Warning! Area has increased risk of a serious accident");
                            }
                            showRecommendation("Warning! Area has increased risk of a serious accident");
                        } else if (riskFactor.getInjuryRiskFactor() >= 1 && riskFactor.getInjuryRiskFactor() > previousInjuryRisk) {
                            if (currentTime - lastNag > 1000 * 60 * 10) {
                                lastNag = currentTime;
                                speak("Warning! Area has increased risk of an injury to you or to a pedestrian");
                            }
                            showRecommendation("Warning! Area has increased risk of an injury to you or to a pedestrian");
                        } else if (mode.equals(IntentConstants.MODE_MOTORIST) && riskFactor.getPropertyRiskFactor() >= 1 && riskFactor.getPropertyRiskFactor() > previousPropertyDamageRisk) {
                            if (currentTime - lastNag > 1000 * 60 * 10) {
                                lastNag = currentTime;
                                speak("Warning! Area has increased risk of damage to your vehicle or public property");
                            }
                            showRecommendation("Warning! Area has increased risk of damage to your vehicle or public property");
                        } else {
                            hideRecommendation();
                        }

                        previousFatalRisk = riskFactor.getFatalRiskFactor();
                        previousInjuryRisk = riskFactor.getInjuryRiskFactor();
                        previousPropertyDamageRisk = riskFactor.getPropertyRiskFactor();

                        riskFactorText.setText("fatal " + riskFactor.getFatalRiskFactor() +
                                ", injury " + riskFactor.getInjuryRiskFactor() + ", damage " + riskFactor.getPropertyRiskFactor());
                        fatalIncidentCount.setText(Integer.toString(riskFactor.getFatalCount()) + " fatal incidents");
                        injuredCount.setText(Integer.toString(riskFactor.getInjuryCount()) + " with injuries");
                        propertyCount.setText(Integer.toString(riskFactor.getPropertyCount()) + " with damage to vehicle");
                    }
                }
            };

            AsyncTask<Void, Void, ArrayList<Incident>> queryIncident = new AsyncTask<Void, Void, ArrayList<Incident>>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    MapsActivity.this.incidentQueryInProgress = true;
                }

                @Override
                protected ArrayList<Incident> doInBackground(Void... voids) {
                    RoadSafeServiceClient client = new RoadSafeServiceClient();
                    float radius = 1f;

                    if (mode.equals(IntentConstants.MODE_PEDESTRIAN)) {
                        radius = 0.5f;
                    }
                    if (MapsActivity.this.use_hour) {
                        DateTime dt = new DateTime();
                        return client.getIncidentReports(currentLocation.longitude, currentLocation.latitude, radius, dt.toDateTime(DateTimeZone.UTC).getHourOfDay());
                    } else {
                        return client.getIncidentReports(currentLocation.longitude, currentLocation.latitude, radius, -1);

                    }
                }

                @Override
                protected void onPostExecute(ArrayList<Incident> incidents) {
                    super.onPostExecute(incidents);
                    MapsActivity.this.incidentQueryInProgress = false;
                    HashSet<String> visibleIncidents = new HashSet<String>();
                    for (int i = 0; i < incidents.size(); i++) {
                        Incident incident = incidents.get(i);
                        LatLng incidentLocation = new LatLng(incident.getLatitude(), incident.getLongitude());

                        BitmapDescriptor bitmapDescriptor = null;

                        if (incident.getSeverity().equals("Fatal")) {
                            bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_skull_black_18dp);
                        } else if (incident.getSeverity().equals("Injury")) {
                            bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital_building_black_18dp);
                        } else {
                            bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_car_black_18dp);
                        }

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(incidentLocation)
                                .title(incident.getSeverity()).icon(bitmapDescriptor);
                        if (markerHashMap.get(incident.getExternelUUID()) == null) {
                            markerHashMap.put(incident.getExternelUUID(), mMap.addMarker(markerOptions));
                        }

                        visibleIncidents.add(incident.getExternelUUID());
                    }

                    //Remove other markers
                    ArrayList<String> keysToRemove = new ArrayList<>();
                    for (String key : markerHashMap.keySet()) {
                        if (!visibleIncidents.contains(key)) {
                            keysToRemove.add(key);
                            markerHashMap.get(key).remove();
                        }
                    }

                    for (String key : keysToRemove) {
                        markerHashMap.remove(key);
                    }
                }
            };
            if (!MapsActivity.this.incidentQueryInProgress) {
                queryIncident.execute();
            }
            if (!MapsActivity.this.riskFactorComputeInProgress) {
                computeRiskFactor.execute();
            }
        } else {
            offlineStatusIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void speak(final String message) {

         this.tts = new TextToSpeech(MapsActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        tts.setLanguage(Locale.US);

    }

    private void renderAllMarkers(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    private void showRecommendation(String message) {
        recommendationContainer.setVisibility(View.VISIBLE);
        recommendationText.setText(message);
    }

    private void hideRecommendation() {
        recommendationContainer.setVisibility(View.GONE);
    }

    /**
     * @return the last know best location
     */
    private Location getLastBestLocation() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
            return null;
        }


        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }

    @Override
    public void onClick(View view) {
        if (view == cmdRefresh) {
            speak("This is a test");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == considerTimeOfDay) {
            Log.d(TAG, "use location onl filter" + b);
            this.use_hour = b;
        } else
            if (compoundButton == trackLocationSwitch) {
                if (!b) {
                    locationManager.removeUpdates(this);
                } else {
                    int permissionCheck = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        trackLocationSwitch.setChecked(false);
                        return;
                    }

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 5000, 10, this);
                }
            }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        this.lockView = false;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        this.lockView = true;
        return false;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        markerHashMap.clear();

        trackLocationSwitch.setChecked(false);
        locationManager.removeUpdates(this);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng);
        mMap.addMarker(markerOptions);
        queryRiskFactor(latLng);
    }
}
