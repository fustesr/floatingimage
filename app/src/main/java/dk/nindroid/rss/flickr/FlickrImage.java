package dk.nindroid.rss.flickr;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.parser.flickr.data.ImageSizes;

public class FlickrImage extends ImageReference{
	private final static String imageType = "flickrInternal";
	String farmID;
	String serverID;
	String imgID;
	String secret;
	String title;
	String owner;
	FlickrUserInfo userInfo;
	boolean unseen;
	boolean personal;
	ImageSizes sizes;
	
	public String getID(){
		return imgID;
	}
	
	public void getExtended(){
		userInfo = PersonInfo.getInfo(owner);
	}
	public String getFarmID() {
		return farmID;
	}
	public void setFarmID(String farmID) {
		this.farmID = farmID;
	}
	public String getServerID() {
		return serverID;
	}
	public void setServerID(String serverID) {
		this.serverID = serverID;
	}
	public String getImageID() {
		return imgID;
	}
	public void setImgID(String imgID) {
		this.imgID = imgID;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public void setTitle(String title){
		this.title = title;
	}
	public String getTitle(){
		return title;
	}
	public void setOwner(String owner){
		this.owner = owner;
	}
	public String getOwner(){
		return owner;
	}
	public String getAuthor(){
		if(userInfo != null){
			return userInfo.getUsername();
		}
		return null;
	}
	public FlickrUserInfo getUserInfo(){
		return userInfo;
	}
	public void setPersonInfo(FlickrUserInfo userInfo){
		this.userInfo = userInfo;
	}
	public FlickrImage(String farmID, String serverID, String imgID,
			String secret, String title, String owner, boolean isNew, boolean isPersonal) {
		super();
		this.farmID = farmID;
		this.serverID = serverID;
		this.imgID = imgID;
		this.secret = secret;
		this.title = title;
		this.owner = owner;
		this.unseen = isNew;
		this.personal = isPersonal;
	}
	public FlickrImage(){
		this.unseen = false;
	}
	@Override
	public String get128ImageUrl() {
		return "http://farm" + farmID + ".static.flickr.com/" + serverID + "/" + imgID + "_" + secret + "_t.jpg";
	}
	@Override
	public String get256ImageUrl() {
		return "http://farm" + farmID + ".static.flickr.com/" + serverID + "/" + imgID + "_" + secret + "_m.jpg"; // 240, but 500 is waaaay too large!
	}
	@Override
	public String getBigImageUrl() {
		if(sizes == null){
			sizes = FlickrFeeder.getImageSizes(imgID);
		}
		if(sizes == null){
			return null;
		}
		if(sizes.getMediumUrl() != null) {
			return sizes.getMediumUrl();
		}
		if(sizes.getSmallUrl() != null) {
			return sizes.getSmallUrl();
		}
		return null;
	}
	@Override
	public String getOriginalImageUrl() {
		if(sizes == null){
			sizes = FlickrFeeder.getImageSizes(imgID);
		}
		if(sizes == null){
			return null;
		}
		if(sizes.getOriginalUrl() != null) {
			return sizes.getOriginalUrl();
		}
		if(sizes.getMediumUrl() != null) {
			return sizes.getMediumUrl();
		}
		if(sizes.getSmallUrl() != null) {
			return sizes.getSmallUrl();
		}
		return null;
	}
	
	@Override
	public String getImagePageUrl() {
		return "http://m.flickr.com/photos/" + owner + "/" + imgID;
	}
	
	@Override
	public Intent follow(){
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setData(Uri.parse(getImagePageUrl()));
		return intent;
	}
	@Override
	public String getInfo() {
		StringBuilder sb = new StringBuilder();
		String nl = "\n";
		sb.append(imageType);
		sb.append(nl);
		sb.append(mWidth);
		sb.append(nl);
		sb.append(mHeight);
		sb.append(nl);
		sb.append(farmID);
		sb.append(nl);
		sb.append(serverID);
		sb.append(nl);
		sb.append(imgID);
		sb.append(nl);
		sb.append(secret);
		sb.append(nl);
		sb.append(URLEncoder.encode(title));
		sb.append(nl);
		sb.append(URLEncoder.encode(owner));
		// Person info
		if(userInfo != null){
			sb.append(nl);
			sb.append(userInfo.getUsername());
			sb.append(nl);
			sb.append(userInfo.getRealName());
			sb.append(nl);
			sb.append(userInfo.getUrl());
		}else{
			sb.append(nl);
			sb.append(nl);
			sb.append(nl);
			sb.append(nl);
			sb.append(nl);
			sb.append(nl);
		}
		return sb.toString(); 
	}
	@Override
	public void parseInfo(String[] tokens, Bitmap bmp) throws IOException {
		mWidth = Float.parseFloat(tokens[2]);
		mHeight = Float.parseFloat(tokens[3]);
		farmID = tokens[4];
		serverID = tokens[5]; 
		imgID = tokens[6];
		secret = tokens[7];
		title = URLDecoder.decode(tokens[8]);
		owner = URLDecoder.decode(tokens[9]);
		userInfo = new FlickrUserInfo();
		if(tokens.length > 10){
			userInfo.setUsername(tokens[10]);
			userInfo.setRealName(tokens[11]);
			userInfo.setUrl(tokens[12]);
		}
		this.mBitmap = bmp;
	}
	public boolean isNew(){
		return unseen;
	}
	public void setOld(){
		unseen = false;
	}
	public boolean isPersonal(){
		return personal;
	}
}
