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


    //Nous nous servirons de cette variable plus tard
    private String node = null;

    private ArrayList<UPnPImage> images = new ArrayList<UPnPImage>();;
    private ArrayList<String> subFolders = new ArrayList<String>();
    UPnPImage image;


    private String idB;
    private int selector = 0; // 1 pour container, 2 pour item

    private String upnp_class;
    private String res;
    private String title;
    private int resolution =0;
    private boolean currentRes;


    //début du parsing
    public void startElement(String namespaceURI, String lname, String qname, Attributes attrs) throws SAXException {
        Log.e("","---------------------------------------------");

        node = qname;
        //cette variable contient le nom du nœud qui a créé l'événement
        Log.e("!!!!!!!!!!!", "qname = " + qname);

        if (qname.equals("container")) {
            selector = 1;
            Log.e("cc","selector = 1");
            idB = attrs.getValue("id");
        }
        else if (qname.equals("item")) {
            selector = 2;
            Log.e("cc","selector = 2");
            image = new UPnPImage();
        }
        else if (qname.equals("res")) {
            String reso = attrs.getValue("resolution");
            if(reso!=null) {
                String resos[] = reso.split("x", 2);
                int size = Integer.parseInt(resos[0]) * Integer.parseInt(resos[1]);

                if (size > resolution) {
                    currentRes = true;
                    resolution = size;
                }
            }
            else if(res==null){
                currentRes = true;
            }
        }
    }

    //fin du parsing
    public void endElement(String uri, String localName, String qName) throws SAXException{

        Log.e("------>", "Fin de l'élément " + qName);
        if (qName.equals("container")) {
            selector = 0;
            subFolders.add(idB);
            Log.e("cc","selector = 0");
        }
        else if (qName.equals("res")) {
            currentRes = false;
        }
        else if (qName.equals("item")) {
            Log.i("URL",res);

            if (upnp_class.equals("object.item.imageItem.photo")){
                image.title = title;
                image.thumbURL = res;
                image.sourceURL = res;
                image.pageURL = res;
                image.setID(res);

                selector = 0;
                images.add(image);
            }

        }

    }


    /**
     * permet de récupérer la valeur d'un nœud
     */
    public void characters(char[] data, int start, int end){

        System.out.println("***********************************************");

        //La variable data contient tout notre fichier.
        //Pour récupérer la valeur, nous devons nous servir des limites en paramètre
        //"start" correspond à l'indice où commence la valeur recherchée
        //"end" correspond à la longueur de la chaîne

        String str = new String(data, start, end);

        System.out.println("Donnée du nœud " + node + " : " + str);



/*        Log.e("cc","NODE ==========" + node);
        Log.e("cc","selector ================" + selector);
        Log.e("cc","STR ==================" + str);
        System.out.println(node.equals("upnp:class"));
        System.out.println(selector == 1);
        System.out.println(str.equals("object.container"));
        System.out.println("ID =  " +idB);*/


/*        if (node.equals("upnp:class") && selector == 2 && str.equals("object.item.imageItem.photo")) {
            selector2 = 1;
            Log.e("cc","selector2 = 1");
        }
        if (node.equals("upnp:class") && selector == 1 && str.equals("object.container")) {
            Log.e("cc","Ajout ID dans subFolders");
            subFolders.add(idB);
            selector = 0;
            Log.e("cc","selector = 0");
        }
        else if (node.equals("res") && selector2 == 1 && firstRes == true) {
            try {
                Log.e("cc","Ajout URL dans images");
                image.thumbURL = str;
                image.sourceURL = str;
                image.pageURL = str;
                image.setID(str);

            } catch (Exception e) {
                Log.e("erreur","ERRRRRRRRRRRRRREUUUUUUUUUUUUUUUUUR");
                e.printStackTrace();
            }
            firstRes = false;
        }
        else if (node.equals("upnp:title") && selector == 1) {
            image.title = str;
        }*/

        if(node.equals("upnp:class")) {
            upnp_class = str;
        }
        else if (node.equals("dc:title")) {
            title = str;
        }
        else if (node.equals("res")) {
            if (currentRes) {
                res = str;
            }
        }


    }

/*    public ArrayList<URL> getImages() {
        return images;
    }*/

    public ArrayList<String> getSubFolders() {
        return subFolders;
    }

    public ArrayList<UPnPImage> getUpnpImage() { return images; }

}



