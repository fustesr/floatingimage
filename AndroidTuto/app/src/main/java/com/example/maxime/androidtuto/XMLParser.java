package com.example.maxime.androidtuto;

import android.os.Build;
import android.support.annotation.RequiresApi;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class XMLParser {

	private ArrayList<URL> images;
	private ArrayList<String> subFolders;

	public XMLParser(String s) {
		images = new ArrayList<URL>();
		subFolders = new ArrayList<String>();
	    try {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream(s.getBytes("UTF-8")));
		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("item");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				if(eElement.getElementsByTagName("upnp:class").item(0).getTextContent().equals("object.item.imageItem.photo"))
					images.add(new URL(eElement.getElementsByTagName("res").item(0).getTextContent()));
			}
		}
		nList = doc.getElementsByTagName("container");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				if(eElement.getElementsByTagName("upnp:class").item(0).getTextContent().equals("object.container"))
					subFolders.add(eElement.getAttribute("id"));
			}
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	public ArrayList<URL> getImages() { return images; }

	public ArrayList<String> getSubFolders() {
		return subFolders;
	}
}