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

    private boolean isCentered = false;

    public CustomLocationOverlay(Context context, MapView mapView)
    {
        super(context, mapView);

        this.mapView = mapView;
        this.context = context;

        this.activity = (MainActivity) context;
    }


    public synchronized void onLocationChanged(Location location)
    {
        super.onLocationChanged(location);

        Log.d("MYLocation", "lat::" + location.getLatitude() + "lng::" + location.getProvider());
        if (!isCentered)
        {
            runOnFirstFix(new Runnable()
            {
                public void run()
                {
                    isCentered = true;
                    mapView.getController().animateTo(getMyLocation());
//                    mapView.getController().setCenter(getMyLocation());
                }
            });
        }
        activity.determineCoins();
    }
}
