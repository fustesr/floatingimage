package dk.nindroid.rss.upnp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.ServiceReference;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import dk.nindroid.rss.GalleryActivity;
import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.parser.upnp.PicturesBatch;
import dk.nindroid.rss.parser.upnp.UPnPParser;

/**
 * Created by Maxime on 14/03/2017.
 */

public class GlobalUpnpService {

    public static AndroidUpnpService upnpService = null;
    public static boolean bound = false;
    private static Vector<RegistryListener> lists = new Vector<RegistryListener>();
    private static HashMap<View,RegistryListener> uiLists = new HashMap<View, RegistryListener>();

    private static ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            upnpService.getControlPoint().search();
            for(RegistryListener l : lists)
                addRegistryListener(l);
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    public static void addRegistryListener(RegistryListener listener){
        if(upnpService!=null) {
            upnpService.getRegistry().addListener(listener);
            for (Device device : upnpService.getRegistry().getDevices())
                if (device instanceof RemoteDevice)
                    listener.remoteDeviceAdded(upnpService.getRegistry(), (RemoteDevice) device);
        }
        else
            lists.add(listener);
    }

    public static void removeRegistryListener(RegistryListener listener){
        upnpService.getRegistry().removeListener(listener);
    }

    public static Registry getRegistry() {
        return upnpService.getRegistry();
    }

    public static RegistryListener pictureFetcherListener(final String udn, final UPnPParser parser){
        return new DefaultRegistryListener() {

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                if(device.getIdentity().getUdn().getIdentifierString().equals(udn)){
                    new PicturesBatch(upnpService, device.findService(new UDAServiceId("ContentDirectory")), parser).run();
                    getRegistry().removeListener(this);
                }
            }
        };
    }

    public static RegistryListener availabilityListener(final String udn, final View v, final Handler handler) {
        return new DefaultRegistryListener() {

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                if(device.getIdentity().getUdn().getIdentifierString().equals(udn))
                    handler.obtainMessage(GalleryActivity.AVAILABLE,v).sendToTarget();
            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                if(device.getIdentity().getUdn().getIdentifierString().equals(udn))
                    handler.obtainMessage(GalleryActivity.UNAVAILABLE,v).sendToTarget();
            }
        };
    }

    public static void addAvailabilityListener(String udn, View v, Handler handler){
        RegistryListener listener = availabilityListener(udn,v,handler);
        uiLists.put(v,listener);
        addRegistryListener(listener);
    }

    public static void removeAvailabilityListener(View v){
        if(uiLists.get(v)!=null) getRegistry().removeListener(uiLists.get(v));
    }


    /***
     * start the upnpservice for the application if not already started
     * here, only one application so should stay bound
     * @param c
     */
    public static void startUpnp(Context c) {
        bound = c.bindService(
                new Intent(c, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    public static void refreshDevices(){
        for(RegistryListener list : lists)
            for (Device device : upnpService.getRegistry().getDevices())
                if (device instanceof RemoteDevice)
                    list.remoteDeviceRemoved(upnpService.getRegistry(), (RemoteDevice) device);
        getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search();
    }
}
