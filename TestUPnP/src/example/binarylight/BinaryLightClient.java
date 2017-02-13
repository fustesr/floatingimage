

package example.binarylight;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.*;
import org.fourthline.cling.model.action.*;
import org.fourthline.cling.model.message.*;
import org.fourthline.cling.model.message.header.*;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.*;
import org.fourthline.cling.registry.*;

public class BinaryLightClient implements Runnable {
	
	RegistryListener createRegistryListener(final UpnpService upnpService) {
	    return new DefaultRegistryListener() {
	
	        ServiceId serviceId = new UDAServiceId("ContentDirectory");
	
	        @Override
	        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
	
	            Service contentDirectory;
	            
	            if ((contentDirectory = device.findService(serviceId)) != null) {
	                System.out.println("Service discovered: " + contentDirectory);
		            new PicturesBatch(upnpService, contentDirectory).run();
	            }
	
	        }
	
	        @Override
	        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
	            Service switchPower;
	            if ((switchPower = device.findService(serviceId)) != null) {
	                System.out.println("Service disappeared: " + switchPower);
	            }
	        }
	
	    };
	}
		
    public static void main(String[] args) throws Exception {
        // Start a user thread that runs the UPnP stack
        Thread clientThread = new Thread(new BinaryLightClient());
        clientThread.setDaemon(false);
        clientThread.start();

    }

    public void run() {
        try {

            UpnpService upnpService = new UpnpServiceImpl();

            // Add a listener for device registration events
            upnpService.getRegistry().addListener(
                    createRegistryListener(upnpService)
            );

            // Broadcast a search message for all devices
            upnpService.getControlPoint().search(
                    new STAllHeader()
            );

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            System.exit(1);
        }
    }
}