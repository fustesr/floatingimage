package com.example.maxime.androidtuto;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Vector;

/**
 * Created by Maxime on 27/02/2017.
 */

public class URLActivity extends ListActivity {

    private ArrayAdapter<URL> listAdapter;
    Vector<URL> images;
    Vector<String> folders;
    Vector<ActionInvocation> waitingForReply;
    AndroidUpnpService upnpService;
    Service contentDirectory;
    final Object lock = new Object();
    boolean waiting = false;

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
            Log.e("REPLY","REPLY");
            final XMLParser parser = new XMLParser(invocation.getOutput()[0].toString());
            /*List<URL> urls = parser.getImages();
            for(URL url : urls) {
                //images.add(url);
                listAdapter.add(url);
            }*/
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.addAll(parser.getImages());
                }
            });
            folders.addAll(parser.getSubFolders());
            waitingForReply.remove(invocation);
            if(waiting)
                synchronized(lock){ lock.notify(); }
        }

        @Override
        public void failure(ActionInvocation invocation,
                            UpnpResponse operation,
                            String defaultMsg) {
            System.err.println(defaultMsg);
            waitingForReply.remove(invocation);
            Log.e("FAIL","FAIL");
            synchronized(lock){ lock.notify(); }
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
        contentDirectory = RemoteDirectories.get(getIntent().getIntExtra("Service",0));
        upnpService = RemoteDirectories.getUpnp();

        new Thread(new Runnable() {
            @Override
            public void run() {
                folders = new Vector<String>();
                images = new Vector<URL>();
                folders.add("0");
                waitingForReply = new Vector<ActionInvocation>();
                ActionInvocation setTargetInvocation;
                while(!folders.isEmpty() || !waitingForReply.isEmpty()){
                    if(!folders.isEmpty()){
                        Log.e("LAUNCH","LAUNCH");
                        setTargetInvocation = new BrowseActionInvocation(contentDirectory, folders.remove(0));
                        waitingForReply.add(setTargetInvocation);
                        upnpService.getControlPoint().execute(new ContentCallback(setTargetInvocation));
                    }
                    else
                        synchronized(lock){
                            if(!folders.isEmpty() || !waitingForReply.isEmpty())
                                try {
                                    waiting = true;
                                    lock.wait();
                                    waiting = false;
                                } catch (InterruptedException e) { e.printStackTrace();}
                        }
                }
            }
        }).start();


    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        URL url = (URL)l.getItemAtPosition(position);
        LayoutInflater factory = LayoutInflater.from(URLActivity.this);
        final View view = factory.inflate(R.layout.image_dialog, null);
        //dialog.setView(view);
        /*dialog.setNeutralButton(
            getString(R.string.OK),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }
        );*/
        //ImageView img = (ImageView)findViewById(R.id.dialog_imageview);
        ImageView img = new ImageView(this);
        new DL(img).execute(url);
        dialog.setView(img);
        dialog.show();
        /*TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        textView.setTextSize(12);*/
        super.onListItemClick(l, v, position, id);
    }

    class DL extends AsyncTask<URL, Void, Bitmap> {
        private ImageView img;

        public DL(ImageView img){
            this.img = img;
        }


        protected Bitmap doInBackground(URL... urls) {
            try {
                URL url = urls[0];
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                Log.e("Bitmap","returned");
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Exception",e.getMessage());
                return null;
            }
        }

        protected void onPostExecute(Bitmap bmp) {
            img.setImageBitmap(bmp);
        }
    }
}
