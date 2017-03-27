package dk.nindroid.rss.upnp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

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
import java.util.List;
import java.util.Vector;

import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.parser.upnp.PicturesBatch;
import dk.nindroid.rss.parser.upnp.UPnPParser;

/**
 * Created by Maxime on 14/03/2017.
 */

public class globalUpnpService {

    public static AndroidUpnpService upnpService = null;
    public static boolean bound = false;
    private static Vector<RegistryListener> lists = new Vector();

    private static ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("Trace", "Device connected");
            upnpService = (AndroidUpnpService) service;
            Log.e("UpnpServ",""+upnpService);
            // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search();
            for(RegistryListener l : lists){
                Log.e("Listener","hey");
                addRegistryListener(l);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    public static void addRegistryListener(RegistryListener listener){
        Log.d("Listener","ddd");
        Log.e("Listener",listener.getClass().toString());
        if(upnpService!=null) {
            // Get ready for future device advertisements
            upnpService.getRegistry().addListener(listener);
            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                Log.e("Device",device.toString() + (device instanceof RemoteDevice));
                if (device instanceof RemoteDevice)
                    listener.remoteDeviceAdded(upnpService.getRegistry(), (RemoteDevice) device);
            }
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

    public static RegistryListener listenForService(final String udn, final UPnPParser parser){
        return new DefaultRegistryListener() {
            ServiceId serviceId = new UDAServiceId("ContentDirectory");
            private boolean used = false;

            @Override
            public void localDeviceAdded(Registry registry, LocalDevice device) {
                Log.e("Event","local device "+device);
            }

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                if(used==true)
                    return;
                used=true;
                Log.e("Event","remote device "+device);
                ServiceReference serviceReference = new ServiceReference(UDN.valueOf(udn),new UDAServiceId("ContentDirectory"));
                Service contentDirectory;
                if ((contentDirectory=upnpService.getRegistry().getService(serviceReference)) != null) {
                    Log.e("Service test","Test succeeded, good service");
                    new PicturesBatch(upnpService, contentDirectory, parser).run();
                    globalUpnpService.getRegistry().removeListener(this);
                }
                else {
                    Log.e("Service test","Test failed, not good service");
                }
            }
        };
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
        Log.e("Bound",""+upnpService);
    }
}
