package dk.nindroid.rss.parser.upnp;

import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.DOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import dk.nindroid.rss.data.ImageReference;

public class PicturesBatch implements Runnable{

	Vector<UPnPImage> images;
	Vector<String> folders;
	Vector<ActionInvocation> waitingForReply;
	AndroidUpnpService upnpService;
	Service contentDirectory;
	UPnPParser parser;
	final Object lock = new Object();


	public PicturesBatch(AndroidUpnpService upnpService, Service contentDirectory, UPnPParser parser) {
		this.upnpService = upnpService;
		this.contentDirectory = contentDirectory;
		this.parser = parser;
		images = new Vector<UPnPImage>();
	}

	class BrowseActionInvocation extends ActionInvocation {

		BrowseActionInvocation(Service service, String id) {
			super(service.getAction("Browse"));
			try {
				// Throws InvalidValueException if the value is of wrong type
				setInput("ObjectID", id);
				setInput("BrowseFlag", "BrowseDirectChildren");
				setInput("Filter", "*");
				setInput("StartingIndex", "0");
				setInput("RequestedCount", "0");
				setInput("SortCriteria", "");


			} catch (InvalidValueException ex) {
				System.err.println(ex.getMessage());
				System.exit(1);
			}
		}
	}

	class ContentCallback extends ActionCallback{

		protected ContentCallback(ActionInvocation actionInvocation) {
			super(actionInvocation);
		}

		@Override
		public void success(ActionInvocation invocation) {
/*			UPnPXMLParser parser = new UPnPXMLParser(invocation.getOutput()[0].toString());
			List<URL> urls = parser.getImages();
			UPnPImage image;*/

			List<UPnPImage> arrayListImage = null;
			UPnPXMLParserSAX parserSAX = null;
			UPnPImage image;


			try {

				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser parser = factory.newSAXParser();

				parserSAX = new UPnPXMLParserSAX();

				InputSource source = new InputSource(new StringReader(invocation.getOutput()[0].toString()));
				parser.parse(source, parserSAX );

				arrayListImage  = parserSAX.getUpnpImage();


			} catch (Exception e) {
				e.printStackTrace();
			}

/*			for (URL url : urls) {
				Log.e("cc", url.toString());
			}
			List<String> listID = parserSAX.getSubFolders();
			for (String id : listID) {
				Log.e("ccID", id);
			}*/

/*			for(URL url : urls){
				images.add(image = new UPnPImage());
				image.title = url.toString();
				image.thumbURL = url.toString();
				image.sourceURL = url.toString();
				image.pageURL = url.toString();
				image.setID(url.toString());
			}*/
			images.addAll(arrayListImage);

			folders.addAll(parserSAX.getSubFolders());
			waitingForReply.remove(invocation);
			if(!parserSAX.getSubFolders().isEmpty() || waitingForReply.isEmpty())
				synchronized(lock){ lock.notify(); }
		}

		@Override
		public void failure(ActionInvocation invocation,
							UpnpResponse operation,
							String defaultMsg) {
			System.err.println(defaultMsg);
			waitingForReply.remove(invocation);
			synchronized(lock){ lock.notify(); }
		}

	}

	@Override
	public void run() {
		folders = new Vector<String>();
		images = new Vector<UPnPImage>();
		folders.add("0");
		waitingForReply = new Vector<ActionInvocation>();
		ActionInvocation setTargetInvocation;

		Log.e("PB","Beginning requests");

		while(!folders.isEmpty() || !waitingForReply.isEmpty()) {
			if (!folders.isEmpty()) {
				setTargetInvocation = new BrowseActionInvocation(contentDirectory, folders.remove(0));
				waitingForReply.add(setTargetInvocation);
				upnpService.getControlPoint().execute(new ContentCallback(setTargetInvocation));
			} else
				synchronized (lock) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
		parser.imgs.addAll(images);
		parser.done.release();
		Log.e("PB","Ending requests");
	}
}
