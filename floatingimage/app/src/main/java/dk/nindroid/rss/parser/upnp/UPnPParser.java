package dk.nindroid.rss.parser.upnp;

import android.content.Context;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

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
    @Override
    public List<ImageReference> parseFeed(FeedReference feed, Context context) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
        return null;
    }

    @Override
    public void init(Settings settings) {}
}
