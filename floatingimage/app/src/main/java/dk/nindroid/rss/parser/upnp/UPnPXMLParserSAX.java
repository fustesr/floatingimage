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

    private ArrayList<URL> images = new ArrayList<URL>();;
    private ArrayList<String> subFolders = new ArrayList<String>();


    private String idB;
    private int selector = 0; // 1 pour container, 2 pour item
    private int selector2 = 0; // 1 pour si "object.item.imageItem.photo" pour aller chercher URL
    private boolean firstRes = true;


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
        }
    }

    //fin du parsing
    public void endElement(String uri, String localName, String qName) throws SAXException{

        Log.e("------>", "Fin de l'élément " + qName);
        if (qName.equals("container")) {
            selector = 0;
            Log.e("cc","selector = 0");
        }
        else if (qName.equals("item")) {
            selector = 0;
            selector2 = 0;
            firstRes = true;
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



        Log.e("cc","NODE ==========" + node);
        Log.e("cc","selector ================" + selector);
        Log.e("cc","STR ==================" + str);
        System.out.println(node.equals("upnp:class"));
        System.out.println(selector == 1);
        System.out.println(str.equals("object.container"));
        System.out.println("ID =  " +idB);
        if (node.equals("upnp:class") && selector == 2 && str.equals("object.item.imageItem.photo")) {
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
                images.add(new URL(str));
            } catch (Exception e) {
                e.printStackTrace();
            }
            firstRes = false;
        }


    }

    public ArrayList<URL> getImages() {
        return images;
    }

    public ArrayList<String> getSubFolders() {
        return subFolders;
    }


}



