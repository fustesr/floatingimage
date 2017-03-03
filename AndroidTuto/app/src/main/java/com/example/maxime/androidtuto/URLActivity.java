package com.example.maxime.androidtuto;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDAServiceType;

import java.net.URI;
import java.net.URL;

/**
 * Created by Maxime on 27/02/2017.
 */

public class URLActivity extends ListActivity {

    private ArrayAdapter<String> listAdapter;

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

    class ContentCallback extends ActionCallback {

        protected ContentCallback(ActionInvocation actionInvocation) {
            super(actionInvocation);
        }

        @Override
        public void success(ActionInvocation invocation) {
            Log.d("Trace","Successfully called action!");
            Log.d("Reply",invocation.getOutput()[0].toString());

            listAdapter.add(invocation.getOutput()[0].toString());
            /*XMLParser parser = new XMLParser(invocation.getOutput()[0].toString());
            List<URL> urls = parser.getImages();

            for(URL url : urls){
                try {
                    images.add(resizeImg(new ImageIcon(ImageIO.read(url))));
                    System.out.println(url);
                } catch (IOException e) {e.printStackTrace();}
            }
            folders.addAll(parser.getSubFolders());
            waitingForReply.remove(invocation);
            if(!parser.getSubFolders().isEmpty() || waitingForReply.isEmpty())
                synchronized(lock){ lock.notify(); }*/
        }

        @Override
        public void failure(ActionInvocation invocation,
                            UpnpResponse operation,
                            String defaultMsg) {
            /*System.err.println(defaultMsg);
            waitingForReply.remove(invocation);
            synchronized(lock){ lock.notify(); }*/
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
                new FixedAndroidLogHandler()
        );
        // Now you can enable logging as needed for various categories of Cling:
        // Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);
        Service s = RemoteDirectories.get(getIntent().getIntExtra("Service",0));
        AndroidUpnpService upnpService = RemoteDirectories.getUpnp();
        /*ServiceType st = new UDAServiceType((String) getIntent().getStringExtra("type"));
        ServiceId si = new UDAServiceId("ContentDirectory");
        URI descriptor = (URI) getIntent().getSerializableExtra("descriptor");
        URI control = (URI) getIntent().getSerializableExtra("control");
        URI event = (URI) getIntent().getSerializableExtra("event");
        try {
            s = new RemoteService(st,si,descriptor,control,event);
        } catch (ValidationException e) {
            Log.d("ERROR","SHITSHITSHITSERVICE");
            s = null;
            e.printStackTrace();
        }
        Log.d("End", s.getAction("Browse").toString());*/
        //listAdapter.add("hello");
        ActionInvocation setTargetInvocation = new BrowseActionInvocation(s, "0");
        upnpService.getControlPoint().execute(new ContentCallback(setTargetInvocation));

    }
}
