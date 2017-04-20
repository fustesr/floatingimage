package dk.nindroid.rss;

import android.util.Log;
import dk.nindroid.rss.data.CircularList;
import dk.nindroid.rss.data.ImageReference;

public class TextureBank {
	CircularList<ImageReference>			images;		
	ImageCache 								ic;
	BitmapDownloader						bitmapDownloader;
	MainActivity							mActivity;
	boolean 								stopThreads = false;
	boolean 								running = false;
	
	public TextureBank(MainActivity activity){
		this.mActivity = activity;
	}
	
	public void initCache(int cacheSize, int activeImages){
		if(this.images == null){
			this.images = new CircularList<ImageReference>(cacheSize);
		}
	}
	
	public void setFeeders(BitmapDownloader bitmapDownloader, ImageCache ic){
		this.bitmapDownloader = bitmapDownloader;
		this.ic = ic;
	}
	
	public void addBitmap(ImageReference ir, boolean doCache, boolean next){
		if(ir != null && ir.getBitmap() != null){
			if(doCache){
				ic.saveImage(ir);
			}
			synchronized (images) {
				if(next){
					images.addNext(ir);
				}else{
					images.addPrev(ir);
				}
			}
		}
	}
		
	public boolean doDownload(String url){
		return !ic.exists(url, mActivity.getSettings().highResThumbs ? 256 : 128);
	}
	
	public void addFromCache(ImageReference ir, boolean next){
		ic.addImage(ir, next, mActivity.getSettings().highResThumbs ? 256 : 128);
	}
		
	public ImageReference getTexture(ImageReference previousImage, boolean next){
		ImageReference ir = get(next, previousImage);
		if(ir != null && ir.getBitmap() != null){ // What? The bitmap should NEVER be null!
			// Remove previous image, if any
			if(previousImage != null){
				//mActiveBitmaps.remove(previousImage.getID());
				if(previousImage.isInvalidated()){
					ic.updateMeta(previousImage);
					previousImage.validate();
				}
			}
			//mActiveBitmaps.put(ir.getID(), ir);
			return ir;
		}
		return null;
	}
	private ImageReference get(boolean next, ImageReference last){
		ImageReference ir = null;
		synchronized (images) {	
			ir = next ? images.next(last) : images.prev(last);
			if(ir != null){
				if(ir.getBitmap() == null) {
					Log.v("Floating Image", "Bad data returned!");
					ir = null;
				}
			}
			images.notifyAll();
		}
		return ir;
	}
	public void reset(){
		this.images.clear();
		//this.mActiveBitmaps.clear();
	}
	
	public void stop(){
		if(running){
			running = false;
			stopThreads = true;
			synchronized(images){
				images.notifyAll();
			}
		}
	}
	public void startExternal(){
		new Thread(bitmapDownloader).start();
		//new Thread(ic).start();
	}
	
	public void start(){
		if(!running){
			running = true;
			stopThreads = false;
			startExternal();
		}
	}
}
