package dk.nindroid.rss.upnp;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;

import dk.nindroid.rss.GalleryActivity;

/**
 * Created by Maxime on 31/03/2017.
 */

public class UPnPHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        switch(msg.what){
            case GalleryActivity.AVAILABLE :
                ((ViewGroup) msg.obj).setBackgroundColor(Color.GREEN);
                break;
            case GalleryActivity.UNAVAILABLE :
                ((ViewGroup) msg.obj).setBackgroundColor(Color.RED);
                break;
        }
    }
}
