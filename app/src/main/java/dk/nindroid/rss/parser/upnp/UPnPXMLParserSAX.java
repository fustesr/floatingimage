package dk.nindroid.rss.parser.upnp;


import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UPnPXMLParserSAX extends DefaultHandler {
    private String node = null;

    private ArrayList<UPnPImage> images = new ArrayList<UPnPImage>();;
    private ArrayList<String> subFolders = new ArrayList<String>();
    UPnPImage image;

    private String idB;

    private String upnp_class;
    private String res;
    private String title;
    private int resolution =0;
    private boolean currentRes;

    public void startElement(String namespaceURI, String lname, String qname, Attributes attrs) throws SAXException {
        node = qname;
        if(qname.equals("container"))
            idB = attrs.getValue("id");
        else if(qname.equals("item")) {
            image = new UPnPImage();
            res = null;
            resolution = 0;
        }
        else if(qname.equals("res")) {
            String reso = attrs.getValue("resolution");
            if(reso!=null) {
                String resos[] = reso.split("x", 2);
                int size = Integer.parseInt(resos[0]) * Integer.parseInt(resos[1]);
                if(size > resolution) {
                    currentRes = true;
                    resolution = size;
                }
            }
            else if(res==null)
                currentRes = true;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException{
        if(qName.equals("container"))
            subFolders.add(idB);
        else if(qName.equals("res"))
            currentRes = false;
        else if(qName.equals("item") && upnp_class.equals("object.item.imageItem.photo")){
            image.title = title;
            image.thumbURL = res;
            image.sourceURL = res;
            image.pageURL = res;
            image.setID(res);
            images.add(image);
        }
    }

    public void characters(char[] data, int start, int end){
        String str = new String(data, start, end);
        if(node.equals("upnp:class"))
            upnp_class = str;
        else if(node.equals("dc:title"))
            title = str;
        else if(node.equals("res") && currentRes)
            res = str;
    }

    public ArrayList<String> getSubFolders() {
        return subFolders;
    }

    public ArrayList<UPnPImage> getUpnpImage() { return images; }

}



