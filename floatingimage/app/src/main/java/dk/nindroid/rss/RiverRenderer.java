package dk.nindroid.rss;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;
import android.view.Surface;
import dk.nindroid.rss.data.ImageReference;
import dk.nindroid.rss.gfx.Vec2f;
import dk.nindroid.rss.renderers.FeedProgress;
import dk.nindroid.rss.renderers.OSD;
import dk.nindroid.rss.renderers.Renderer;
import dk.nindroid.rss.renderers.floating.ShadowPainter;

public class RiverRenderer implements GLSurfaceView.Renderer, dk.nindroid.rss.helpers.GLWallpaperService.Renderer {
	public Display			mDisplay;
	
	private boolean 		mTranslucentBackground = false;
	private boolean			mMoveEventHandled = false;
	private long			mUpTime;
	private Vec2f 			mClickedPos = new Vec2f();
	private boolean 		mClicked = false;
	private boolean			mDoubleClicked = false;
	private boolean 		mShowOSD = false;
	private boolean 		mHideOSD = false;
	private long 			mOffset;
	private float			mFadeOffset = 0;
	private static final float mSensitivityX = 70.0f;
	private final boolean 	mLimitFramerate;
	
	private Renderer  		mRenderer;
	private OSD				mOSD;
	private FeedProgress	mFeedProgress;
	MainActivity 			mActivity;
	private long			mLastFrameTime = 0;
	private long			mLastFPSTime = 0;
	private int				mFrames = 0;
	private boolean			mPause = false;
	
	private int				mFeedsLoaded;
	private int				mFeedsTotal;
	private boolean			mReinit = true;
	
	private long			mStartTime;
	
	private long			mForwardPressed = -1;
	private long			mRewindPressed = -1;
	private long			mForwardReleased = -1;
	private long			mRewindReleased = -1;
	private long			mForwardPressedTime = -1;
	private long			mRewindPressedTime = -1;
		
	public RiverRenderer(MainActivity activity, boolean useTranslucentBackground, boolean limitFramerate){
		this.mActivity = activity;
		this.mLimitFramerate = limitFramerate;
		mDisplay = new Display(activity.getSettings());
		mTranslucentBackground = useTranslucentBackground;
		mOSD = new OSD(activity);
		mFeedProgress = new FeedProgress(activity.context());
		mStartTime = System.currentTimeMillis();
		mLastFrameTime = mStartTime;
		mOffset = -(long)(mActivity.getSettings().floatingTraversal * 0.1f);
	}
	
	public void setRenderer(Renderer renderer){
		this.mRenderer = renderer;
	}
	
	public Renderer getRenderer(){
		return this.mRenderer;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		if(mReinit){
			Log.v("Floating Image", "initting");
			mReinit = false;
			ShadowPainter.initTexture(gl);
	      	mOSD.init(gl, mDisplay);
	      	mRenderer.init(gl, System.currentTimeMillis() + mOffset, mOSD);
			FeedProgress.init();
			Log.v("Floating Image", "initting done!");
		}
		
		
		
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_MODULATE);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL10.GL_PROJECTION);
        //gl.glLoadIdentity();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        //GLU.gluLookAt(gl, mCamDir.getX(), mCamDir.getY(), mCamDir.getZ(), mCamPos.getX(), mCamPos.getY(), mCamPos.getZ(), 0.0f, 1.0f, 0.0f);
        GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);
       
        long realTime = System.currentTimeMillis();
        
        long timeDiff = realTime - mLastFrameTime;
        
        if(mPause){
        	this.mOffset -= timeDiff;
        } else if(timeDiff > 200){ // We left the app, and have returned, or experienced lag.
        	this.mOffset -= timeDiff - 80; 
        }
        
        if(mLimitFramerate){
        	int targetFrametime = mActivity.getSettings().lowFps ? 80 : 40;
	        if(timeDiff < targetFrametime){
	        	try {
					Thread.sleep(targetFrametime - timeDiff);
				} catch (InterruptedException e) {
					Log.w("Floating Image", "Framerate limiting sleep interrupted.", e);
				}
	        }
        }
        
        //*
        ++mFrames;
        if(realTime - mLastFPSTime > 1000){
        	//Log.v("Floating Image", "Framerate is " + mFrames + " frames per second");
        	mFrames = 0;
        	mLastFPSTime = realTime;
        }
        //*/
        fadeOffset(realTime);
        applyFowardRewind(realTime);
        mOffset = mRenderer.editOffset(mOffset, realTime - mStartTime, mPause);
        long time = realTime + mOffset;
        
        mDisplay.setFrameTime(realTime);
        if(mShowOSD){
        	mShowOSD = false;
        	mOSD.show(realTime);
        }else if(mHideOSD){
        	mHideOSD = false;
        	if(mOSD.hide(realTime)){
        		mMoveEventHandled = true;
        	}
        }else if(mClicked){
        	mClicked = false;
        	if (!mRenderer.click(gl, mClickedPos.getX(), mClickedPos.getY(), time, realTime)){
        		toggleMenu();
        	}
        }else if(mDoubleClicked){
        	mDoubleClicked = false;
        	if(!mRenderer.doubleClick(gl, mClickedPos.getX(), mClickedPos.getY(), time, realTime)){
        		toggleMenu();
        	}
        }
        mRenderer.update(gl, time - mStartTime, realTime);
        
        ///////// DRAWING /////////
        gl.glRotatef(mDisplay.getRotation(), 0.0f, 0.0f, 1.0f);
        mRenderer.render(gl, time - mStartTime, realTime);
        if(!mDisplay.isTurning()){
        	mFeedProgress.draw(gl, mFeedsLoaded, mFeedsTotal, mDisplay);
        	mOSD.draw(gl, realTime);
        }
        
        mLastFrameTime = realTime;
	}
	
	private void fadeOffset(long time) {
		float timeFactor = (3000 - (time - mUpTime)) / 3000.0f;
		float fadeOffset = mFadeOffset * timeFactor * timeFactor;// * mSensitivityX;
		if(timeFactor > 0){
			mOffset += fadeOffset;
		}else{
			mFadeOffset = 0.0f;
		}
	}
	
	private void applyFowardRewind(long time){
		long frameMultiplier = time - mLastFrameTime;
		if(mForwardPressed != -1){
			mForwardPressedTime = time - mForwardPressed;
			long offset = Math.min(mForwardPressedTime, 2000) / 100;
			mOffset += offset * frameMultiplier;
		}else if(mForwardReleased != -1){
			long stopTime = Math.min(mForwardPressedTime, 2000);
			long startSpeed = stopTime / 100;
			long offset = startSpeed - Math.min(time - mForwardReleased, stopTime) / 100;
			mOffset += offset * frameMultiplier;
		}
		if(mRewindPressed != -1){
			mRewindPressedTime = time - mRewindPressed;
			long offset = Math.min(mRewindPressedTime, 2000) / 100;
			mOffset -= offset * frameMultiplier;
		}else if(mRewindReleased != -1){
			long stopTime = Math.min(mRewindPressedTime, 2000);
			long startSpeed = stopTime / 100;
			long offset = startSpeed - Math.min(time - mRewindReleased, stopTime) / 100;
			mOffset -= offset * frameMultiplier;
		}
	}
	
	public void setFeeds(int progress, int total){
		this.mFeedsTotal = total;
		this.mFeedsLoaded = progress;
	}
	
	public Intent followSelected(){
		return mRenderer.followCurrent();
	}
	
	public ImageReference getSelected(){
		return mRenderer.getCurrent();
	}
	
	public void deleteSelected(){
		mRenderer.deleteCurrent();
	}
	
	public boolean unselect(){
		return mRenderer.back();
	}
	
	public void onResume(){
		mFadeOffset = 0.0f;
		mReinit = true;
		if(mActivity.getSettings().galleryMode){
			mPause = true;
		}
		mRenderer.onResume();
	}
	public void onPause(){
		mRenderer.onPause();
	}
	
	public void setBackground(){
		mRenderer.setBackground();
	}
	
	public int[] getConfigSpec() {
        int[] configSpec = {
                EGL10.EGL_RED_SIZE,      8,
                EGL10.EGL_GREEN_SIZE,    8,
                EGL10.EGL_BLUE_SIZE,     8,
                EGL10.EGL_ALPHA_SIZE,    0,
                EGL10.EGL_DEPTH_SIZE,    0,
                EGL10.EGL_NONE
        };
        return configSpec;
	}
	
	public void resetImages(){
		Log.v("Floating Image", "Resetting images");
		if(mRenderer != null){
			mRenderer.resetImages();
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		mDisplay.onSurfaceChanged(width, height);
		
        /*
         * Set our projection matrix. This doesn't have to be done
         * each time we draw, but usually a new projection needs to
         * be set when the viewport is resized.
         */
						
		// Half screen width * depth (plus a little) + a little for rotation of pictures + jitter distance
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float screenAspect = (float)width / height;
        
        gl.glFrustumf(-screenAspect, screenAspect, -1, 1, 1, 50);
	}
	
	public void xChanged(float amount){
		if(mDisplay.getOrientation() == Surface.ROTATION_0){
			mOffset += amount * mSensitivityX;
		}else if(mDisplay.getOrientation() == Surface.ROTATION_180){
			mOffset -= amount * mSensitivityX;
		}
	}
	
	public void yChanged(float amount){
		if(mDisplay.getOrientation() == Surface.ROTATION_270){
			mOffset += amount * mSensitivityX;
		}else if(mDisplay.getOrientation() == Surface.ROTATION_90){
			mOffset -= amount * mSensitivityX;
		}
	}
	
	/*
	private boolean showOSD(float x, float y){
		if(y > 0){
			mHideOSD = true;
			return true;
		}
		return false;
	}
	*/
	
	public boolean isVertical(float speedX, float speedY){
		return Math.abs(speedY) > Math.abs(speedX);
	}
	
	public boolean pause(){
		this.mPause ^= true;
		return this.mPause;
	}
	
	public boolean isPaused(){
		return mPause;
	}
	
	public void beginFastForward(){
		if(mForwardPressed == -1){
			mForwardPressed = System.currentTimeMillis();
		}
	}
	
	public void endFastForward(){
		mForwardPressed = -1;
		mForwardReleased = System.currentTimeMillis();
	}
	
	public void beginRewind(){
		if(mRewindPressed == -1){
			mRewindPressed = System.currentTimeMillis();
		}
	}
	
	public void endRewind(){
		mRewindPressed = -1;
		mRewindReleased = System.currentTimeMillis();
	}
	
	public void zoomIn(){
		mRenderer.zoomIn();
	}
	
	public void zoomOut(){
		mRenderer.zoomOut();
	}
	
	float lastX;
	float lastY;
	
	public void wallpaperMove(float fraction){
		mRenderer.wallpaperMove(fraction);
	}
	
	public void move(float x, float y, float speedX, float speedY){
		if(!mActivity.getSettings().moveStream) return;
		// Transform event!
		int orientation = mDisplay.getOrientation();
		float tmp;
		switch(orientation){
		case Surface.ROTATION_0:
			// Do nothing
			break;
		case Surface.ROTATION_270:
			tmp = x; x = y; y = tmp;
			y *= -1;
			tmp = speedX; speedX = speedY; speedY = tmp;
			speedY *= -1;
			break;
		case Surface.ROTATION_90:
			tmp = x; x = y; y = tmp;
			x *= -1;
			tmp = speedX; speedX = speedY; speedY = tmp;
			speedX *= -1;
			break;
		case Surface.ROTATION_180:
			x *= -1;
			y *= -1;
			speedX *= -1;
			speedY *= -1;
		}
		
		mRenderer.streamMoved(speedX, speedY);
		
		// Free image movement, override gestures
		if(mRenderer.freeMove()){
			mMoveEventHandled = true;
			y *= -1;
			x = x / mDisplay.getWidthPixels() * mDisplay.getWidth() * 2.0f;
			y = y / mDisplay.getHeightPixels() * mDisplay.getHeight() * 2.0f;
			mRenderer.move(x, y);
		}else{
			if(!mMoveEventHandled){
				// Pull up OSD?
				//if(isVertical(speedX, speedY)){
					//showOSD(speedX, speedY);
				if(mRenderer.getCurrent() == null){
					mFadeOffset = mRenderer.adjustOffset(speedX, speedY);
					mUpTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	public void moveEnd(float speedX, float speedY){
		if(!mActivity.getSettings().moveStream) return;
		if(!mMoveEventHandled){
			// Transform event!
			int orientation = mDisplay.getOrientation();
			float tmp;
			switch(orientation){
			case Surface.ROTATION_0:
				// Do nothing
				break;
			case Surface.ROTATION_270:				
				tmp = speedX; speedX = speedY; speedY = tmp;
				speedY *= -1;
				break;
			case Surface.ROTATION_90:
				tmp = speedX; speedX = speedY; speedY = tmp;
				speedX *= -1;
				break;
			case Surface.ROTATION_180:
				speedX *= -1;
				speedY *= -1;
			}
			
			// Slide right or left gesture?
			if(Math.abs(speedX) > Math.abs(speedY)){
				if(speedX > 0.0f){
					mRenderer.slideRight(System.currentTimeMillis());
				}
				else{
					mRenderer.slideLeft(System.currentTimeMillis());
				}
			}
		}
		transformEnd();
	}
	
	public void transform(float centerX, float centerY, float x, float y, float rotate, float scale){
		int orientation = mDisplay.getOrientation();
		float tmp;
		y *= -1;
		centerY = mDisplay.getPortraitHeightPixels() - centerY;
		switch(orientation){
		case Surface.ROTATION_0:
			break;
		case Surface.ROTATION_270:
			tmp = x; x = y; y = tmp;
			x *= -1;
			tmp = centerX; centerX = centerY; centerY = tmp;
			break;
		case Surface.ROTATION_90:
			tmp = x; x = y; y = tmp;
			y *= -1;
			tmp = centerX; centerX = centerY; centerY = tmp;
			break;
		case Surface.ROTATION_180:
			x *= -1;
			y *= -1;
			break;
		}
		
		Log.v("Floating Image", "Transform (" + centerX + "," + centerY + ")");
		
		x /= mDisplay.getWidthPixels();
		y /= mDisplay.getHeightPixels();
		centerX /= mDisplay.getWidthPixels();
		centerY /= mDisplay.getHeightPixels();
		
		x *= mDisplay.getWidth() * 2.0f;
		y *= mDisplay.getHeight() * 2.0f;
		centerX *= mDisplay.getWidth() * 2.0f;
		centerY *= mDisplay.getHeight() * 2.0f;
		
		switch(orientation){
		case Surface.ROTATION_0:
			centerX -=  mDisplay.getWidth();
			centerY -=  mDisplay.getHeight();
			break;
		case Surface.ROTATION_270:
			centerY -=  mDisplay.getHeight();
			centerX -=  mDisplay.getWidth();
			centerX *= -1;
			break;
		case Surface.ROTATION_90:
			centerY -=  mDisplay.getHeight();
			centerY *= -1;
			centerX -=  mDisplay.getWidth();
			break;
		case Surface.ROTATION_180:
			centerX -=  mDisplay.getWidth();
			centerY -=  mDisplay.getHeight();
			centerX *= -1;
			centerY *= -1;
			break;
		}
		Log.v("Floating Image", "Adjusted transform (" + centerX + "," + centerY + ")");
		
		mRenderer.transform(centerX, centerY, x, y, rotate, scale);
	}
	
	public void moveInit(){
		mMoveEventHandled = false;
		mRenderer.initTransform();
	}
	
	public void transformEnd(){
		mRenderer.transformEnd();
	}
	
	public void onClick(float x, float y){
		if(!mActivity.getSettings().selectImage) return;
		// Transform coordinates!
		Vec2f pos = transformClick(x, y);
		x = pos.getX();
		y = pos.getY();
		
		if(!mOSD.click(x, y, System.currentTimeMillis())){ // This is ok, we're using realtime for the OSD
			mClicked = true;
			toScreenSpace(pos);
			mClickedPos = pos;
		}
	}
	
	public void onDoubleClick(float x, float y){
		mDoubleClicked = true;
		Vec2f pos = transformClick(x, y);
		toScreenSpace(pos);
		mClickedPos = pos;
	}
	
	private Vec2f transformClick(float x, float y){
		int orientation = mDisplay.getOrientation();
		float tmp;
		switch(orientation){
		case Surface.ROTATION_0:
			// Do nothing
			break;
		case Surface.ROTATION_270:
			tmp = x; x = y; y = tmp;
			y = mDisplay.getHeightPixels() - y;
			break;
		case Surface.ROTATION_90:
			tmp = x; x = y; y = tmp;
			x = mDisplay.getWidthPixels() - x;
			break;
		case Surface.ROTATION_180:
			x = mDisplay.getWidthPixels() - x;
			y = mDisplay.getHeightPixels() - y;
		}
		
		return new Vec2f(x, y);
	}
	
	private void toScreenSpace(Vec2f pos){
		pos.setX((pos.getX()/mDisplay.getWidthPixels() * 2.0f - 1.0f) * mDisplay.getWidth() / 2.0f);
		pos.setY(-(pos.getY() / mDisplay.getHeightPixels() * 2.0f - 1.0f) * mDisplay.getHeight() / 2.0f);
	}
	
	
	public void toggleMenu(){
		if(mOSD.isShowing()){
			mHideOSD = true;
		}else{
			mShowOSD = mActivity.canShowOSD();
		}
	}
 
	@Override 
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glMatrixMode(GL10.GL_TEXTURE);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		//mOSD.init(gl);
		
		/*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glEnable(GL10.GL_DITHER);
        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
         gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                 GL10.GL_FASTEST);
         gl.glEnable(GL10.GL_TEXTURE_2D);
         
         gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
 		 gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_FASTEST);
         

         if (mTranslucentBackground) {
             gl.glClearColor(0,0,0,0);
         } else {
             gl.glClearColor(0,0,0,1);
         }
         gl.glDisable(GL10.GL_CULL_FACE);
         gl.glShadeModel(GL10.GL_FLAT);
         gl.glDisable(GL10.GL_DEPTH_TEST);
	}
}