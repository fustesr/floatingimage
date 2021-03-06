package dk.nindroid.rss.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

import dk.nindroid.rss.R;
import dk.nindroid.rss.settings.SourceSelector.SourceFragment;
import dk.nindroid.rss.upnp.GlobalUpnpService;

public class UPnPBrowser extends SourceFragment {

	public UPnPBrowser() {
		super(4);
	}
	
	boolean mDualPane;
	ArrayAdapter<DeviceDisplay> listAdapter;
	ServiceId serviceId = new UDAServiceId("ContentDirectory");
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		View sourceFrame = getActivity().findViewById(R.id.source);
        mDualPane = sourceFrame != null && sourceFrame.getVisibility() == View.VISIBLE;
	}

	BrowseRegistryListener currentListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		listAdapter = new ArrayAdapter(this.getActivity(), android.R.layout.simple_list_item_1);
		setListAdapter(listAdapter);
		GlobalUpnpService.startUpnp(this.getActivity().getApplicationContext());
		GlobalUpnpService.addRegistryListener(new BrowseRegistryListener());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		GlobalUpnpService.removeRegistryListener(currentListener);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		DeviceDisplay deviceDisplay = (DeviceDisplay)l.getItemAtPosition(position);
		RemoteDevice d = (RemoteDevice) deviceDisplay.device;
		returnUdn(d.getIdentity().getUdn().getIdentifierString(),deviceDisplay.toString());
	}

	@Override
	public boolean back() {
		return false;
	}
	
	void returnUdn(String udn, String title){
		Intent intent = new Intent();
		Bundle b = new Bundle();
		b.putString("PATH", udn);
		b.putString("NAME", title);
		b.putInt("TYPE", Settings.TYPE_UPNP);
		intent.putExtras(b);
		this.getActivity().setResult(Activity.RESULT_OK, intent);		
		this.getActivity().finish();
	}

	protected class BrowseRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */
		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(
							getActivity(),
							"Discovery failed of '" + device.getDisplayString() + "': "
									+ (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
							Toast.LENGTH_LONG
					).show();
				}
			});
			deviceRemoved(device);
		}
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			deviceRemoved(device);
		}

		public void deviceAdded(final Device device) {
			if ((device.findService(serviceId)) != null && getActivity()!=null) {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						DeviceDisplay d = new DeviceDisplay(device);
						int position = listAdapter.getPosition(d);
						if (position >= 0) {
							// Device already in the list, re-set new value at same position
							listAdapter.remove(d);
							listAdapter.insert(d, position);
						}
						else
							listAdapter.add(d);
					}
				});
			}
		}

		public void deviceRemoved(final Device device) {
			if(getActivity()!=null)
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					listAdapter.remove(new DeviceDisplay(device));
				}
			});
		}
	}

	protected class DeviceDisplay {

		Device device;

		public DeviceDisplay(Device device) {
			this.device = device;
		}

		public Device getDevice() {
			return device;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DeviceDisplay that = (DeviceDisplay) o;
			return device.equals(that.device);
		}

		@Override
		public int hashCode() {
			return device.hashCode();
		}

		@Override
		public String toString() {
			String name =
					getDevice().getDetails() != null && getDevice().getDetails().getFriendlyName() != null
							? getDevice().getDetails().getFriendlyName()
							: getDevice().getDisplayString();
			// Display a little star while the device is being loaded (see performance optimization earlier)
			return device.isFullyHydrated() ? name : name + " *";
		}
	}

}
