package com.tourassistant.coderoids.starttrip;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.tourassistant.coderoids.R;
import com.tourassistant.coderoids.adapters.UserReviewsAdapter;
import com.tourassistant.coderoids.helpers.AppHelper;
import com.tourassistant.coderoids.helpers.DirectionsJSONParser;
import com.tourassistant.coderoids.helpers.LocationHelper;
import com.tourassistant.coderoids.helpers.PermissionHelper;
import com.tourassistant.coderoids.helpers.RuntimePermissionsActivity;
import com.tourassistant.coderoids.interfaces.LoginHelperInterface;
import com.tourassistant.coderoids.models.ReviewModel;
import com.tourassistant.coderoids.profilefriends.FriendsProfileActivity;
import com.tourassistant.coderoids.services.LocationService;
import com.tourassistant.coderoids.services.LocationThread;
import com.visuality.f32.temperature.Temperature;
import com.visuality.f32.temperature.TemperatureUnit;
import com.visuality.f32.weather.data.entity.Forecast;
import com.visuality.f32.weather.data.entity.RainInformation;
import com.visuality.f32.weather.data.entity.Weather;
import com.visuality.f32.weather.manager.WeatherManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StartTrip extends RuntimePermissionsActivity implements LoginHelperInterface {
    GoogleMap map;
    ArrayList<LatLng> markerPoints;
    TextView tvDistanceDuration ,cityCurrent ,updatedCurrentField ,currentTemperatureField,detailsField;
    TextView cityDest ,updatedDestField ,destTemperatureField,detailsFieldDest;
    LatLng currentLocation;
    LatLng destination;
    MarkerOptions marker, marker2;
    Button startNavigation ,foreCastTrip ,reviewTrip;
    ImageView currentWeatherIcon;
    ImageView destWeatherIcon;
    LocationHelper locationManager = null;
    Double latitude = 0.0;
    Double longitude = 0.0;
    SharedPreferences.Editor editorLogin;
    SharedPreferences prefLogin;
    ExtendedFloatingActionButton hazardFabBut;
    RecyclerView rvReviews;
    LinearLayoutManager llReviews;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_trip);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        locationManager = LocationHelper.getInstance();
        if(!locationManager.getLatitude().matches("") && !locationManager.getLongitude().matches("")) {
            latitude = Double.parseDouble(locationManager.getLatitude());
            longitude = Double.parseDouble(locationManager.getLongitude());
        }
        tvDistanceDuration = findViewById(R.id.tv_distance_time);

        cityCurrent =  findViewById(R.id.city_field);
        foreCastTrip =  findViewById(R.id.forecast_trip);
        reviewTrip =  findViewById(R.id.review_trip);
        updatedCurrentField = findViewById(R.id.updated_field);
        currentWeatherIcon =  findViewById(R.id.weather_icon);
        currentTemperatureField =  findViewById(R.id.current_temperature_field);
        detailsField =  findViewById(R.id.details_field);
        rvReviews = findViewById(R.id.user_reviews);
        llReviews = new LinearLayoutManager(this);
        llReviews.setOrientation(LinearLayoutManager.HORIZONTAL);
        cityDest =  findViewById(R.id.city_field_dest);
        updatedDestField = findViewById(R.id.updated_field_dest);
        destWeatherIcon =  findViewById(R.id.weather_icon_dest);
        destTemperatureField =  findViewById(R.id.current_temperature_field_dest);
        detailsFieldDest =findViewById(R.id.details_field_dest);
        hazardFabBut = findViewById(R.id.hazard_fab);

        startNavigation = findViewById(R.id.start_nav);
        // Initializing
        markerPoints = new ArrayList<LatLng>();
        prefLogin = getSharedPreferences("logindata", Context.MODE_PRIVATE);
        editorLogin = prefLogin.edit();
        if(AppHelper.tripEntityList.getFirebaseUserId().matches(AppHelper.currentProfileInstance.getUserId())) {
            editorLogin.putString("isTripInProgress","1").apply();
            foreCastTrip.setVisibility(View.VISIBLE);
        } else {
            foreCastTrip.setVisibility(View.GONE);
        }
        // Getting reference to SupportMapFragment of the activity_main
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (map == null) {
            // Getting Map for the SupportMapFragment
            fm.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mGoogleMap) {
                    map = mGoogleMap;
                    populateMap();
                    startWeatherForecasting();
                }
            });
        }

        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigating();
            }
        });

        foreCastTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            startActivity(new Intent(StartTrip.this,ForecastTripActivity.class));
            }
        });

        hazardFabBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartTrip.this,ReportHazard.class));

            }
        });

        reviewTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });
        updateFirebaseDb();
        fetchReviews();
    }

    private void fetchReviews() {
        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
        rootRef.collection("PublicTrips").document(AppHelper.tripEntityList.getFirebaseId()).collection("reviews").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isComplete()) {
                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                    if (documentSnapshots != null) {
                        documentSnapshots.size();
                        UserReviewsAdapter newsListingAdapter = new UserReviewsAdapter(StartTrip.this, documentSnapshots);
                        rvReviews.setAdapter(newsListingAdapter);
                        rvReviews.setLayoutManager(llReviews);
                    }
                }
            }
        });
    }

    private void updateFirebaseDb() {
        if(AppHelper.tripEntityList.getFirebaseUserId().matches(AppHelper.currentProfileInstance.getUserId())) {
            String tripId = AppHelper.tripEntityList.getFirebaseId();
            FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
            DocumentReference uidRefPublic = rootRef.collection("PublicTrips").document(tripId);
            uidRefPublic.update("tripLocationTracking", "1");
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        startLocationService();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startLocationService() {
        boolean isLocationServiceRunning = isServiceRunning(LocationService.class);
        if (isLocationServiceRunning ) {
            stopLocationService();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                try {
                    LocationThread locationThread = new LocationThread(StartTrip.this,"LocationService");
                    locationThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 3000);
        LocationService.shouldContinueRunnable = true;
    }

    public void stopLocationService() {
        boolean isLocationServiceRunning = isServiceRunning(LocationService.class);
        if (isLocationServiceRunning) {
            stopService(new Intent(this, LocationService.class));
            if(LocationThread.t != null){
                LocationThread.t.interrupt();
                LocationThread.t = null;
            }
        }
    }

    private void startWeatherForecasting() {
        if(destination != null) {
            new WeatherManager(getResources().getString(R.string.weather_forecast_id)).getCurrentWeatherByCoordinates(
                    destination.latitude, // latitude
                    destination.longitude, // longitude
                    new WeatherManager.CurrentWeatherHandler() {
                        @Override
                        public void onReceivedCurrentWeather(WeatherManager manager, Weather weather) {
                            // RainInformation rainInformation = weather1.getRain();
                            cityDest.setText(weather.getNavigation().getLocationName());
                            Temperature temperature = weather.getTemperature().getCurrent();
                            Double celcius = temperature.getValue(TemperatureUnit.CELCIUS);
                            destTemperatureField.setText(String.format("%.1f", celcius) + (char) 0x00B0 + "C");
                            detailsFieldDest.setText("Humidity \n" + weather.getAtmosphere().getHumidityPercentage() + "");
                            manageWeaherIcon(weather, currentWeatherIcon);
                        }

                        @Override
                        public void onFailedToReceiveCurrentWeather(WeatherManager manager) {
                            // Handle error
                        }
                    }
            );
        }

        new WeatherManager(getResources().getString(R.string.weather_forecast_id)).getCurrentWeatherByCoordinates(
               latitude, // latitude
                longitude, // longitude
                new WeatherManager.CurrentWeatherHandler() {
                    @Override
                    public void onReceivedCurrentWeather(WeatherManager manager, Weather weather) {
                        // RainInformation rainInformation = weather1.getRain();
                        cityCurrent.setText(weather.getNavigation().getLocationName());
                        Temperature temperature = weather.getTemperature().getCurrent();
                        Double celcius = temperature.getValue(TemperatureUnit.CELCIUS);
                        currentTemperatureField.setText(String.format("%.1f", celcius) + (char) 0x00B0+"C");
                        detailsField.setText("Humidity \n"+weather.getAtmosphere().getHumidityPercentage()+"");
                        manageWeaherIcon(weather,destWeatherIcon);
                    }

                    @Override
                    public void onFailedToReceiveCurrentWeather(WeatherManager manager) {
                        // Handle error
                    }
                }
        );
    }

    private void manageWeaherIcon(Weather weather, ImageView destWeatherIcon) {
        if(weather.getCloudiness().getPercentage() > 1 && weather.getCloudiness().getPercentage() < 25){
            destWeatherIcon.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.ic_sunny));
        } else if( weather.getCloudiness().getPercentage() >25 && weather.getCloudiness().getPercentage() <50){
            destWeatherIcon.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.ic_sunny_clouds));
        } else if(weather.getCloudiness().getPercentage() >50 && weather.getCloudiness().getPercentage() <70){
            destWeatherIcon.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.ic_cloudy_day));
        } else if(weather.getCloudiness().getPercentage() > 70){
            destWeatherIcon.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.ic_cloudy));
        }
    }

    private void populateMap() {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        currentLocation = new LatLng(latitude, longitude);
        if(AppHelper.tripRoomPlace != null && AppHelper.tripRoomPlace.size()>0 && AppHelper.tripRoomPlace.get(0) != null)
            destination = AppHelper.tripRoomPlace.get(0).getLatLng();

        marker = new MarkerOptions().position(currentLocation).title("Address");
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        if(destination != null) {
            marker2 = new MarkerOptions().position(destination).title("Address");
            marker2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marker.getPosition());
        builder.include(marker2.getPosition());
        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen


        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 8));
        // addMarker(currentLocation,dest);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(15).build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,width,height,padding));
        addMarker();
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

            }
        });
    }

    private void startNavigating() {
        if(destination != null) {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination.latitude + "," + destination.longitude);
            Log.v("My Point", "" + gmmIntentUri.toString());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null)
                startActivity(mapIntent);
        }
    }

    private void addMarker() {
        // Checks, whether start and end locations are captured
        map.addMarker(marker);
        markerPoints.add(currentLocation);
        if(destination != null) {
            map.addMarker(marker2);
            markerPoints.add(destination);

        // Checks, whether start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);
            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);
            DownloadTask downloadTask = new DownloadTask();
            // Start downloading json data from Google Directions
            downloadTask.execute(url);
        }
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=" + getResources().getString(R.string.google_maps__browser_key);

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;
        // Output format
        String output = "json";
        // Building the url to the web service


        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
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
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onPrivacyPolicy(String state) {

    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {
        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if (result.size() < 1) {
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {    // Get distance from the list
                        distance = (String) point.get("distance");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = (String) point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }

            tvDistanceDuration.setText("Distance:" + distance + ", Duration:" + duration);

            // Drawing polyline in the Google Map for the i-th route
            map.addPolyline(lineOptions);
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.view_submit_review, null);
        dialogBuilder.setView(dialogView);
        RatingBar appCompatRatingBar = dialogView.findViewById(R.id.rating_bar);
        TextInputEditText tvReviewText = dialogView.findViewById(R.id.review_et);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        AlertDialog alertDialog = dialogBuilder.create();
        appCompatRatingBar.setRating((float) 3.0);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float rating = appCompatRatingBar.getRating();
                String message = tvReviewText.getText().toString();
                ReviewModel reviewModel = new ReviewModel();
                reviewModel.setReviewerId(AppHelper.currentProfileInstance.getUserId());
                reviewModel.setRatingCount(rating+"");
                reviewModel.setReviewMessage(message);
                FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
                rootRef.collection("PublicTrips").document(AppHelper.tripEntityList.getFirebaseId())
                        .collection("reviews").document().set(reviewModel);
                Toast.makeText(StartTrip.this, "Submitted", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
