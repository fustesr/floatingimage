package com.example.maxime.androidtuto;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDAServiceType;

import java.net.URI;

/**
 * Created by Maxime on 27/02/2017.
 */

public class URLActivity extends ListActivity {

    private ArrayAdapter<Service> listAdapter;

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
        RemoteService s;
        ServiceType st = new UDAServiceType((String) getIntent().getStringExtra("type"));
        ServiceId si = new UDAServiceId("ContentDirectory");
        URI descriptor = (URI) getIntent().getSerializableExtra("descriptor");
        URI control = (URI) getIntent().getSerializableExtra("control");
        URI event = (URI) getIntent().getSerializableExtra("event");
        try {
            s = new RemoteService(st,si,descriptor,control,event);
        } catch (ValidationException e) {
            s = null;
            e.printStackTrace();
        }

        listAdapter.add(s);
    }
}
