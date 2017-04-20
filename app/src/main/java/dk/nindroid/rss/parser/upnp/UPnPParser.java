package dk.nindroid.rss.parser.upnp;

import android.content.Context;
import android.util.Log;

import org.fourthline.cling.registry.RegistryListener;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import dk.nindroid.rss.data.FeedReference;
import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.parser.FeedParser;
import dk.nindroid.rss.settings.Settings;
import dk.nindroid.rss.upnp.GlobalUpnpService;

/**
 * Created by Maxime on 07/03/2017.
 */

public class UPnPParser implements FeedParser {
    public List<ImageReference> imgs;
    public Semaphore done = new Semaphore(0);

    @Override
    public List<ImageReference> parseFeed(FeedReference feed, Context context) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
        boolean timeOut = true;
        RegistryListener listener;
        imgs = new ArrayList<ImageReference>();
        GlobalUpnpService.startUpnp(context.getApplicationContext());
        GlobalUpnpService.addRegistryListener(listener = GlobalUpnpService.pictureFetcherListener(feed.getFeedLocation(),this));
        try {
            timeOut = !done.tryAcquire(1,15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(timeOut){
            GlobalUpnpService.getRegistry().removeListener(listener);
            return imgs;
        }
        return imgs;
    }

    @Override
    public void init(Settings settings) {}
}
