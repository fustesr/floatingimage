package com.example.maxime.androidtuto;


import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.meta.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Maxime on 03/03/2017.
 */

public class RemoteDirectories {
    private static ConcurrentHashMap<Integer,Service> dir = new ConcurrentHashMap<Integer, Service>();
    private static AndroidUpnpService upnpService;

    public static void setUpnp(AndroidUpnpService upnpService){ RemoteDirectories.upnpService = upnpService; }

    public static AndroidUpnpService getUpnp(){ return upnpService; }

    public static void add(Service s){ dir.put(s.hashCode(),s); }

    public static Service get(int i){ return dir.get(i); }
}
