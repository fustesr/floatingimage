package example.binarylight;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;

public class PicturesBatch implements Runnable{

	Vector<ImageIcon> images;
	Vector<String> folders;
	Vector<ActionInvocation> waitingForReply;
	UpnpService upnpService;
	Service contentDirectory;
	final Object lock = new Object();
	
	
	public PicturesBatch(UpnpService upnpService, Service contentDirectory) {
		this.upnpService = upnpService;
		this.contentDirectory = contentDirectory;
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
			System.out.println("Successfully called action!");
			System.out.println(invocation.getOutput()[0]);
			XMLParser parser = new XMLParser(invocation.getOutput()[0].toString());
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

	public ImageIcon resizeImg(ImageIcon i){
		return new ImageIcon(i.getImage().getScaledInstance(-1, 600,  java.awt.Image.SCALE_SMOOTH)); 
	}

	@Override
	public void run() {
		folders = new Vector<String>();
		images = new Vector<ImageIcon>();
		folders.add("0");
		waitingForReply = new Vector<ActionInvocation>();
		ActionInvocation setTargetInvocation;
		
		while(!folders.isEmpty() || !waitingForReply.isEmpty()){
			if(!folders.isEmpty()){
				setTargetInvocation = new BrowseActionInvocation(contentDirectory, folders.remove(0));
				waitingForReply.add(setTargetInvocation);
				upnpService.getControlPoint().execute(new ContentCallback(setTargetInvocation));
			}
			else
				synchronized(lock){
					try {
						lock.wait();
					} catch (InterruptedException e) { e.printStackTrace();}
				}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
		        JPanel pane = new JPanel();
		        for(ImageIcon im : images)
		        	pane.add(new JLabel(im));
		        JFrame display = new JFrame("Batch"){{
		        	add(new JScrollPane(pane));
			        setVisible(true); 
			        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			        pack();
			        setSize(800, 600);
		        }};
			}
		});
		
	}
}
