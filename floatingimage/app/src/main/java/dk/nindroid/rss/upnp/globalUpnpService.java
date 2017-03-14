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
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Vector;

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
                addRegistryListener(l);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    public static void addRegistryListener(RegistryListener listener){
        if(upnpService!=null) {
            // Get ready for future device advertisements
            upnpService.getRegistry().addListener(listener);
            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices())
                if (device instanceof RemoteDevice)
                    listener.remoteDeviceAdded(upnpService.getRegistry(), (RemoteDevice) device);
        }
        else
            lists.add(listener);
    }

    public static Registry getRegistry() {
        return upnpService.getRegistry();
    }

    /***
     * start the upnpservice for the application if not already started
     * here, only one application so should stay bound
     * @param a
     */
    public static void startUpnp(Activity a) {
        bound = a.getApplicationContext().bindService(
                new Intent(a, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
        Log.e("Bound",""+upnpService);
    }
}
