package qvdev.apps.test.locator;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import java.util.ArrayList;

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem>
{

    private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();

    public MyItemizedOverlay(Drawable marker) {
        super(boundCenterBottom(marker));

        populate();
    }

    public void addItem(GeoPoint p, String title, String snippet){
        limitList();
        OverlayItem newItem = new OverlayItem(p, title, snippet);
        overlayItemList.add(newItem);
        populate();
    }

    public void removeItem(OverlayItem coin){
        overlayItemList.remove(coin);
        populate();
    }

    private void limitList()
    {
        if(overlayItemList.size() > MainActivity.MAX_COINS_IN_LIST)
        {
            overlayItemList.remove(0);
        }
        else if(overlayItemList.size() < MainActivity.MAX_COINS_IN_LIST)
        {

        }
    }

    @Override
    protected OverlayItem createItem(int i) {
        return overlayItemList.get(i);
    }

    @Override
    public int size() {
        return overlayItemList.size();
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);
    }

}
