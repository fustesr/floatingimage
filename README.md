# Floating Image
Floating Image is an Android application that streams images from the web as well as from the device and displays them in a continuous stream of floating images across the display.
Floating Image supports images coming from your device, and from the following web sources:
* Flickr
* Picasa
* Facebook
* 500px
* Photobucket
* RSS

## What we added
We added the possibility to add a UPnP feed to your image stream. The application is able to detect nearby available UPnP servers that can share images (thanks to the ContentDirectory service), and you can select one to fetch the images it contains.

The background color for each UPnP feed indicates if the server is currently available from your device (green means available, red means unavailable). Should the server turn on or off while on the same network than the device, the color updates in real-time, otherwise (for example: if one loses connection to the server) you will have to refresh the feed list manually using the _refresh_ button.
