package qvdev.apps.test.locator;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;


public class CustomLocationOverlay extends MyLocationOverlay
{
    private MapView mapView;
    private Context context;

    private MainActivity activity;

    public CustomLocationOverlay(Context context, MapView mapView)
    {
        super(context, mapView);

        this.mapView = mapView;
        this.context = context;

        this.activity = (MainActivity) context;
    }


    public synchronized void onLocationChanged(Location location)
    {
        Log.d("MYLocation", "lat::" + location.getLatitude() + "lng::" + location.getProvider());

        activity.determineCoins();

        super.onLocationChanged(location);
    }
}
