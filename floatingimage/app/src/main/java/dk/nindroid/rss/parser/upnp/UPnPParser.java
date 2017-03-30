package dk.nindroid.rss.parser.upnp;

import android.content.Context;
import android.util.Log;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import dk.nindroid.rss.data.FeedReference;
import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.parser.FeedParser;
import dk.nindroid.rss.settings.Settings;
import dk.nindroid.rss.upnp.globalUpnpService;

/**
 * Created by Maxime on 07/03/2017.
 */

public class UPnPParser implements FeedParser {
    public List<ImageReference> imgs;
    public Semaphore done = new Semaphore(0);

    @Override
    public List<ImageReference> parseFeed(FeedReference feed, Context context) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
        boolean timeOut = true;
        imgs = new ArrayList<ImageReference>();
        globalUpnpService.startUpnp(context.getApplicationContext());
        globalUpnpService.addRegistryListener(globalUpnpService.listenForService(feed.getFeedLocation(),this));
        try {
            timeOut = !done.tryAcquire(1,7, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(timeOut){
            Log.e("Parser", "Failed to parse "+feed.getName());
            return imgs;
        }
        Log.e("Parser","Images obtained");
        for(ImageReference img : imgs)
            Log.w("Image",img.getOriginalImageUrl());
        return imgs;
    }

    @Override
    public void init(Settings settings) {}
}