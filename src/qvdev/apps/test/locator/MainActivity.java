package qvdev.apps.test.locator;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.maps.*;
import com.swarmconnect.Swarm;
import com.swarmconnect.SwarmAchievement;
import com.swarmconnect.SwarmActiveUser;
import com.swarmconnect.SwarmLeaderboard;
import com.swarmconnect.delegates.SwarmLoginListener;

import java.util.Map;
import java.util.Random;

public class MainActivity extends MapActivity
{
    /*
    Maps keys
     */
    //0RXhTqtcEwuHXarBIsYWM7MLQWFJcW5gfq5waFw debug
    //0srr_2pUysGmijiNMQAbapUzKLKOB9WqNyysS9Q release


    private static final int SWARM_APP_ID = 164;
    private static final String SWARM_APP_KEY = "3e302b4710dfaaa758ac1d2811a42d07";

    private static final int LEADERBOARD_ID = 3005;

    private static final int ACHIEVEMENT_ID_1 = 4747;
    private static final int ACHIEVEMENT_ID_2 = 4749;

    private static final int ACHIEVEMENT_LIMIT_1 = 10;
    private static final int ACHIEVEMENT_LIMIT_2 = 50;

    private static SwarmLeaderboard leaderboard;
    private static Map<Integer, SwarmAchievement> achievements;

    private static final String COLLECTED_COINS = "coins_collected";

    public static final int MAX_COINS_IN_LIST = 10;
    public static final int MAX_METERS_IN_SIGHT = 100;
    public static final int MIN_METERS_COINS_CATCH = 10;

    private int score = 0;

    private TextView scoreView;
    private TextView userNameView;
    private TextView timeRemaining;

    private Button startPauseButton;

    private MapView mapView;
    private CustomLocationOverlay myLocationOverlay;
    private MapController myMapController;

    private Drawable marker;
    private MyItemizedOverlay myItemizedOverlay;

    /*
    Timer for remaining time
     */
    private final static int LVL_TIME_MIN = 5;
    private final static int LVL_TIME = (LVL_TIME_MIN * 60) * 1000;
    private boolean pause = true;
    private boolean stopped = false;
    private CountDownTimerPausable myTimer;

    private boolean freeMode = false;

    /*
    Sounds
     */
    private MediaPlayer mediaPlayer;

    /*
    Animation
     */
    ImageView coinScore;
    Animation rotationAnimation;

    /*
    Location
     */

    LocationManager locationManager;
    LocationProvider provider;
    private static final int UPDATE_LATLNG = 0;
    private static final int LOCATION_INTERVAL = 100; //miliseconds
    private static final int LOCATION_METERS = 1; //meters

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        startPauseButton = (Button) findViewById(R.id.start_pause);

        userNameView = (TextView) findViewById(R.id.user_name);
        timeRemaining = (TextView) findViewById(R.id.time_remaining);


        coinScore = (ImageView) findViewById(R.id.coin);
        rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.coin_jump);

        coinScore.startAnimation(rotationAnimation);

        mapView = (MapView) findViewById(R.id.mapview);
        scoreView = (TextView) findViewById(R.id.coins);

        mapView.setClickable(false);


        myMapController = mapView.getController();
        myMapController.setZoom(20); //Fixed Zoom Level

        myLocationOverlay = new CustomLocationOverlay(this, mapView);
        myLocationOverlay.enableCompass();

        // add this overlay to the MapView and refresh it
        mapView.getOverlays().add(myLocationOverlay);

        marker = getResources().getDrawable(R.drawable.retro_coin);
        int markerWidth = marker.getIntrinsicWidth();
        int markerHeight = marker.getIntrinsicHeight();
        marker.setBounds(0, markerHeight, markerWidth, 0);

        myItemizedOverlay = new MyItemizedOverlay(marker);
        mapView.getOverlays().add(myItemizedOverlay);

        mapView.postInvalidate();

        setLocationMan();

        Swarm.setActive(this);
        Swarm.init(this, SWARM_APP_ID, SWARM_APP_KEY, mySwarmLoginListener);
    }

    private void setLocationMan()
    {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        // Retrieve a list of location providers that have fine accuracy, no monetary cost, etc
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setCostAllowed(false);

        String providerName = locationManager.getBestProvider(criteria, true);

        // If no suitable provider is found, null is returned.
        if (providerName != null)
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL,          // 10-second interval.
                    LOCATION_METERS,             // 10 meters.
                    listener);
        }
    }

    private void startTimer()
    {
        myTimer = new CountDownTimerPausable(LVL_TIME, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                timeRemaining.setText("" + millisRemaining / 1000);
            }

            @Override
            public void onFinish()
            {

                timeRemaining.setText("" + 0);
                stopped = true;
                stopTimer();
                startPauseButton.setText("Start");
                Swarm.showLeaderboards();

            }
        }.start();
    }

    public void freeMode(View v)
    {
        if (!freeMode && myTimer != null)
        {
            freeMode = true;
            pauseTimer();
        } else if(myTimer != null)
        {
            freeMode = false;
            unPauseTimer();
        }
    }

    private void pauseTimer()
    {
        if (myTimer != null && !myTimer.isPaused())
            myTimer.pause();
    }

    private void unPauseTimer()
    {
        if (myTimer != null && myTimer.isPaused())
            myTimer.start();
    }

    private void stopTimer()
    {
        myTimer = null;
        score = 0;
        addScore(null);
    }


    public void showDash(View v)
    {
        if (!pause && !stopped)
        {
            playEffect(R.raw.smb_pause);
            Swarm.showDashboard();
            pause = true;
            pauseTimer();
        } else if (!pause && stopped)
        {
            stopped = false;
            startTimer();
            startPauseButton.setText("Pause");
        }
    }

    public void addScore(View v)
    {
        score++;

        setCoins(score);
        coinScore.startAnimation(rotationAnimation);

        playEffect(R.raw.smb_coin);
    }

    private void playEffect(int id)
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.reset();
        }
        mediaPlayer = MediaPlayer.create(this, id);
        mediaPlayer.start();
    }

    private void setCoins(int coins)
    {
        String formatted = String.format("%02d", coins);

        scoreView.setText("x " + formatted);
    }

    public void submitScore(View v)
    {
        if (MainActivity.leaderboard != null)
        {
            // Then submit the score
            MainActivity.leaderboard.submitScore(score);

            if (score >= ACHIEVEMENT_LIMIT_1 && score < ACHIEVEMENT_LIMIT_2)
            {

                // Make sure that we have our achievements map.
                if (MainActivity.achievements != null)
                {

                    // Grab the achievement from our map.
                    SwarmAchievement achievement = MainActivity.achievements.get(ACHIEVEMENT_ID_1);

                    // No need to unlock more than once...
                    if (achievement != null && achievement.unlocked == false)
                    {
                        achievement.unlock();
                    }
                }
            } else if (score >= ACHIEVEMENT_LIMIT_2)
            {

                // Make sure that we have our achievements map.
                if (MainActivity.achievements != null)
                {

                    // Grab the achievement from our map.
                    SwarmAchievement achievement = MainActivity.achievements.get(ACHIEVEMENT_ID_2);

                    // No need to unlock more than once...
                    if (achievement != null && achievement.unlocked == false)
                    {
                        achievement.unlock();
                    }
                }
            }

            if (Swarm.isLoggedIn())
            {
                Swarm.user.saveCloudData(COLLECTED_COINS, "" + score);
            }
        }
    }


    SwarmLeaderboard.GotLeaderboardCB callbackLeader = new SwarmLeaderboard.GotLeaderboardCB()
    {
        public void gotLeaderboard(SwarmLeaderboard leaderboard)
        {

            if (leaderboard != null)
            {

                // Save the leaderboard for later use
                MainActivity.leaderboard = leaderboard;
            }
        }
    };

    SwarmAchievement.GotAchievementsMapCB callbackAchievement = new SwarmAchievement.GotAchievementsMapCB()
    {

        public void gotMap(Map<Integer, SwarmAchievement> achievements)
        {

            // Store the map of achievements somewhere to be used later.
            MainActivity.achievements = achievements;
        }
    };

    private SwarmLoginListener mySwarmLoginListener = new SwarmLoginListener()
    {

        // This method is called when the login process has started
        // (when a login dialog is displayed to the user).
        public void loginStarted()
        {
        }

        // This method is called if the user cancels the login process.
        public void loginCanceled()
        {
        }

        // This method is called when the user has successfully logged in.
        public void userLoggedIn(SwarmActiveUser user)
        {
            SwarmLeaderboard.getLeaderboardById(LEADERBOARD_ID, callbackLeader);
            SwarmAchievement.getAchievementsMap(callbackAchievement);

            // Make sure a user is logged in...
            user.getCloudData(COLLECTED_COINS, callback);

            //Set the name of the user
            userNameView.setText(user.username);

            //Start the timer and game
            pause = false;
            stopped = false;
            startTimer();
        }

        // This method is called when the user logs out.
        public void userLoggedOut()
        {
        }

    };

    SwarmActiveUser.GotCloudDataCB callback = new SwarmActiveUser.GotCloudDataCB()
    {
        public void gotData(String data)
        {

            // Did our request fail (network offline, and uncached)?
            if (data == null)
            {

                // Handle failure case.
                return;
            }

            // Has this key never been set?  Default it to a value...
            if (data.length() == 0)
            {
                // In this case, we're storing levelProgress, default them to level 1.
                data = "0";
            }

            // Parse the level data for later use
            score = Integer.parseInt(data);
            setCoins(score);
        }
    };


    private final LocationListener listener = new LocationListener()
    {

        @Override
        public void onLocationChanged(Location location)
        {
            // A new location update is received.  Do something useful with it.  In this case,
            // we're sending the update to a handler which then updates the UI with the new
            // location.
//            Message.obtain(mHandler,
//                    UPDATE_LATLNG,
//                    location.getLatitude() + ", " +
//                            location.getLongitude()).sendToTarget();

            Log.d("Location", "lat::" + location.getLatitude() + "lng::" + location.getLongitude());


            if (myLocationOverlay.getMyLocation() != null)
            {
                //Log.d("Location", "lat::" + myLocationOverlay.getMyLocation().getLatitudeE6() + "lng::" + myLocationOverlay.getMyLocation().getLongitudeE6());

                //CenterLocation(myLocationOverlay.getMyLocation());


            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle)
        {

        }

        @Override
        public void onProviderEnabled(String s)
        {

        }

        @Override
        public void onProviderDisabled(String s)
        {

        }

    };

    public static Location getLocation(Location location, int radius)
    {
        Random random = new Random();

        // Convert radius from meters to degrees
        double radiusInDegrees = radius / 100;

        double x0 = location.getLongitude() * 1E6;
        double y0 = location.getLatitude() * 1E6;
        double u = random.nextInt(1001) / 1000;
        double v = random.nextInt(1001) / 1000;
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        // Adjust the x-coordinate for the shrinking of the east-west distances
        double new_x = x / Math.cos(y0);

        // Set the adjusted location
        Location newLocation = new Location("Loc in radius");
        newLocation.setLongitude(new_x + x0);
        newLocation.setLatitude(y + y0);

        Log.d("NewLocation", "lat::" + newLocation.getLatitude() + "lng::" + newLocation.getLongitude());

        return newLocation;
    }

    private void CenterLocation(GeoPoint centerGeoPoint)
    {
        myMapController.setCenter(centerGeoPoint);

    }

    private void collectCoin()
    {
        double lat1 = ((double) myLocationOverlay.getMyLocation().getLatitudeE6()) / 1e6;
        double lng1 = ((double) myLocationOverlay.getMyLocation().getLongitudeE6()) / 1e6;

        for (int i = 0; i < myItemizedOverlay.size(); i++)
        {
            OverlayItem coin = myItemizedOverlay.getItem(i);

            double lat2 = ((double) coin.getPoint().getLatitudeE6()) / 1e6;
            double lng2 = ((double) coin.getPoint().getLongitudeE6()) / 1e6;

            float[] dist = new float[1];
            Location.distanceBetween(lat1, lng1, lat2, lng2, dist);

            float distance = dist[0];

            Log.d("distance", "distance in m::" + distance);


            if (distance < MIN_METERS_COINS_CATCH)
            {
                myItemizedOverlay.removeItem(coin);
                addScore(null);
            } else if (distance > MAX_METERS_IN_SIGHT)
            {
                myItemizedOverlay.removeItem(coin);
            }
        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        // This verification should be done during onStart() because the system calls
        // this method when the user returns to the activity, which ensures the desired
        // location provider is enabled each time the activity resumes from the stopped state.
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled)
        {
            // Build an alert dialog here that requests that the user enable
            // the location services, then when the user clicks the "OK" button,
            // call enableLocationSettings()
        }
    }

    protected void onStop()
    {
        super.onStop();
        locationManager.removeUpdates(listener);
        submitScore(null);
    }

    private void enableLocationSettings()
    {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation)
    {
        if (currentBestLocation == null)
        {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder)
        {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate)
        {
            return true;
        } else if (isNewer && !isLessAccurate)
        {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
        {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null)
        {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    protected boolean isRouteDisplayed()
    {
        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Make sure a user is logged in...
        if (Swarm.isLoggedIn())
        {
            Swarm.user.getCloudData(COLLECTED_COINS, callback);
            userNameView.setText(Swarm.user.username);
            if (myTimer != null && myTimer.isPaused)
            {
                unPauseTimer();
                pause = false;
            }

        }
        Swarm.setActive(this);
        myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Swarm.setInactive(this);
        myLocationOverlay.disableMyLocation();
    }


    public void determineCoins()
    {
        if (!stopped)
        {
            if (myLocationOverlay.getMyLocation() != null)
            {
                if (myItemizedOverlay.size() < MAX_COINS_IN_LIST)
                {
                    // Determine a random location from the bounds set previously
                    GeoPoint mapCenter = mapView.getMapCenter();
                    GeoPoint southWest = mapView.getProjection().fromPixels(0, mapView.getHeight());
                    GeoPoint northEast = mapView.getProjection().fromPixels(mapView.getWidth(), 0);

                    int lngSpan = northEast.getLongitudeE6() - southWest.getLongitudeE6();
                    int latSpan = northEast.getLatitudeE6() - southWest.getLatitudeE6();

                    for (int i = myItemizedOverlay.size(); i < MAX_COINS_IN_LIST; i++)
                    {
                        int lat = (int) (southWest.getLatitudeE6() + latSpan * Math.random());
                        int lng = (int) (southWest.getLongitudeE6() + lngSpan * Math.random());

                        GeoPoint myGeoPoint = new GeoPoint(
                                lat,
                                lng);

                        myItemizedOverlay.addItem(myGeoPoint, "myPoint1", "myPoint");
                    }

                }

                CenterLocation(myLocationOverlay.getMyLocation());

                collectCoin();
            }
        }
    }
}
