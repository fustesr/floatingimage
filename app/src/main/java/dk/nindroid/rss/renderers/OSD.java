package dk.nindroid.rss.renderers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.view.Surface;
import dk.nindroid.rss.Display;
import dk.nindroid.rss.MainActivity;
import dk.nindroid.rss.RiverRenderer;
import dk.nindroid.rss.gfx.Vec3f;
import dk.nindroid.rss.renderers.osd.Brightness;
import dk.nindroid.rss.renderers.osd.Button;
import dk.nindroid.rss.renderers.osd.Fullscreen;
import dk.nindroid.rss.renderers.osd.Images;
import dk.nindroid.rss.renderers.osd.Play;
import dk.nindroid.rss.renderers.osd.PlayPauseAbstractButton;
import dk.nindroid.rss.renderers.osd.PlayPauseAutoPicker;
import dk.nindroid.rss.renderers.osd.PlayPauseEventHandler;
import dk.nindroid.rss.renderers.osd.PlayPauseFloating;
import dk.nindroid.rss.renderers.osd.RotateClockwise;
import dk.nindroid.rss.renderers.osd.RotateCounterClockwise;
import dk.nindroid.rss.uiActivities.ToggleNotificationBar;

public class OSD {
	private static final long 		FADE_TIME = 400;
	private static final long 		SHOW_TIME = 4000;
	
	private boolean				mExtendDisplayTime = false;
	
	long 						mInputTime;
	float						mFraction = 0.0f;
	boolean 					mPlaying = true;
	boolean 					mShowing;
	
	private static Vec3f[]		mVertices;
	private static final int 	one = 0x10000;
	private static IntBuffer	mVertexBuffer;
	private static ByteBuffer	mIndexBuffer;
	private Display 			mDisplay;
	
	private static Brightness								mBrightness;
	private static Fullscreen								mFullscreen;
	private static Images									mImages;
	private static PlayPauseAbstractButton					mPlay;
	private static PlayPauseAbstractButton					mPlayPauseAutopicker;
	private static PlayPauseAbstractButton					mPlayPauseFloating;
	private static dk.nindroid.rss.renderers.osd.Settings 	mSettings;
	
	private boolean 										mShowRotation;
	private static RotateClockwise 							mRotateClockwise;
	private static RotateCounterClockwise				 	mRotateCounterClockwise;
	private MainActivity 									mActivity;
	
	
	private static Button[]		mButtons;
	
	private static FloatBuffer 	mTexBuffer;
	
	static {
	    int vertices[] = {
			 -one,  one, -one,
			 -one, -one, -one,
			  one,  one, -one,
			  one, -one, -one
			  };
		
		byte indices[] = {
				 0, 1, 2, 3
		};
		
		mVertices = new Vec3f[4];
		for(int i = 0; i < 4; ++i){
			Vec3f p = new Vec3f(vertices[i*3] / one, vertices[i*3 + 1] / one, vertices[i*3 + 2] / one);
			mVertices[i] = p;
		}
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);
		
		mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
		mIndexBuffer.put(indices);
		mIndexBuffer.position(0);
		
		ByteBuffer tbb = ByteBuffer.allocateDirect(4 * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();
		float tex[] = {
	        	0.0f,  0.0f,
	        	0.0f,  1.0f,	
	        	1.0f,  0.0f,
	        	1.0f,  1.0f,
	        };
		
        mTexBuffer.put(tex);
        mTexBuffer.position(0);
	}
	
	public static void init(MainActivity activity, RiverRenderer renderer){
		mBrightness = new Brightness(activity);
		mImages = new Images(activity);
		mSettings = new dk.nindroid.rss.renderers.osd.Settings(activity);
		mFullscreen = new Fullscreen();
		mPlay = new Play();
		
		mRotateClockwise = new RotateClockwise(renderer);
		mRotateCounterClockwise = new RotateCounterClockwise(renderer);
		mPlayPauseAutopicker = new PlayPauseAutoPicker();
		mPlayPauseFloating = new PlayPauseFloating();
	}
	
	public OSD(MainActivity activity){
		this.mActivity = activity;
	}
	
	public void init(GL10 gl, Display display){
		this.mDisplay = display;
		mBrightness.init(gl);
		mImages.init(gl);
		mSettings.init(gl);
		mFullscreen.init(gl, mActivity.context(), mDisplay);
		mPlay.init(gl, mActivity.context());
		mRotateClockwise.init(gl, mActivity.context());
		mRotateCounterClockwise.init(gl, mActivity.context());
		mPlayPauseAutopicker.init(gl, mActivity.context());
		mPlayPauseFloating.init(gl, mActivity.context());
	}
	
	public boolean isShowing(){
		return mFraction != 0.0f;
	}
	
	public void setEnabled(boolean play, boolean playIsAutoSelect, boolean moveImages, boolean fullscreen, boolean rotation, boolean settings, boolean images){
		List<Button> buttons = new ArrayList<Button>();
		mShowRotation = rotation;
		int amount = 1;
		
		buttons.add(mBrightness);
		if(play){
			if(playIsAutoSelect){
				buttons.add(mPlayPauseAutopicker);
			}else{
				buttons.add(mPlay);
			}
			++amount;
		}
		if(moveImages){
			buttons.add(mPlayPauseFloating);
			++amount;
		}
		if(fullscreen){
			buttons.add(mFullscreen);
			++amount;
		}
		if(settings) ++amount;
		if(images){
			buttons.add(mImages);
			++amount;
		}
		mButtons = new Button[amount];
		
		int buttonsIndex = 0;		
		
		// Ensure settings is always bottom right corner! (if shown)
		if(amount % 2 == 0){
			int i;
			for(i = 0; i < 3; ++i){
				mButtons[buttonsIndex++] = buttons.get(i);
			}
			if(settings){
				mButtons[buttonsIndex++] = mSettings;
			}
			while(i < buttons.size()){
				mButtons[buttonsIndex++] = buttons.get(i++);
			}
		}else{
			int i;
			for(i = 0; i < 2; ++i){
				mButtons[buttonsIndex++] = buttons.get(i);
			}
			if(settings){
				mButtons[buttonsIndex++] = mSettings;
			}
			while(i < buttons.size()){
				mButtons[buttonsIndex++] = buttons.get(i++);
			}
		}
	}
	
	public void draw(GL10 gl, long realtime){
		if(mExtendDisplayTime){
			mExtendDisplayTime = false;
			mInputTime = realtime - FADE_TIME;
		}
		long t = realtime - mInputTime;
		if(t < FADE_TIME){
			// Fade in
			mFraction = ((float)t) / FADE_TIME;
		}else if(t < FADE_TIME + SHOW_TIME){
			if(!mShowing){
				mActivity.runOnUiThread(new ToggleNotificationBar(mActivity, true));
				mActivity.runOnUiThread(new ToggleNotificationBar(mActivity, true));
				mShowing = true;
				mFraction = 1.0f;
			}
		}else if(t < 2 * FADE_TIME + SHOW_TIME){
			// Fade out
			mFraction = 1.0f - (((float)t - FADE_TIME - SHOW_TIME)) / FADE_TIME;
		}else{
			if(mShowing || isShowing()){
				mFraction = 0.0f;
				mShowing = false;
				mActivity.runOnUiThread(new ToggleNotificationBar(mActivity, false));
			}
		}
		if(isShowing()){
			drawOSD(gl);
		}
	}
	
	private void drawOSD(GL10 gl){
		boolean showRotation = mShowRotation && mRotateClockwise.doShow();
		gl.glPushMatrix();
			gl.glDisable(GL10.GL_DEPTH_TEST);
			gl.glEnable(GL10.GL_BLEND);
			
			float barHeight = toScreenHeight(80);
			barHeight = (mButtons.length > 4 ? barHeight * 2 : barHeight);
			if(mDisplay.getOrientation() == Surface.ROTATION_180){
				float offset = toScreenHeight(20);
				gl.glTranslatef(0.0f, offset, 0.0f);
			}
			
			gl.glColor4f(1.0f, 1.0f, 1.0f, mFraction * 0.5f);
			drawBackBar(gl, barHeight);
			float yPos = mDisplay.getHeight() * 0.5f;
			if(showRotation){
				drawRotationBoxes(gl, yPos);
			}
			
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			drawIcons(gl);
			if(showRotation){
				drawRotation(gl, yPos);
			}
			
			gl.glDisable(GL10.GL_BLEND);
			gl.glEnable(GL10.GL_DEPTH_TEST);
			
		gl.glPopMatrix();
	}
	
	private void drawIcons(GL10 gl){
		float width = 64.0f / mDisplay.getWidthPixels() * mDisplay.getWidth();
		float height = toScreenHeight(64);
		float yPos = -mDisplay.getHeight() + toScreenHeight(40) + height * 0.5f;
		float dx = mDisplay.getWidth() * 0.5f;
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glActiveTexture(GL10.GL_TEXTURE0);
				
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
        float outsideLeft = -mDisplay.getWidth() - width;
        float outsideBottom;
        outsideBottom = -mDisplay.getHeight() - height;
        float outsideRight = mDisplay.getWidth() + width;
        
        int index = -1;
        int buttons = mButtons.length;
        
        if(mButtons.length == 2){
        	gl.glPushMatrix();
			gl.glTranslatef(getPos(outsideLeft, -1.0f * dx), yPos, -1.0f);	
			gl.glScalef(width, height, 1.0f);	
			drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
		gl.glPopMatrix();
		gl.glPushMatrix();
			gl.glTranslatef(getPos(outsideRight, 1.0f * dx), yPos, -1.0f);
			gl.glScalef(width, height, 1.0f);
			drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
		gl.glPopMatrix();
        }else if(mButtons.length % 2 == 0){
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideLeft, -1.5f * dx), yPos, -1.0f);	
				gl.glScalef(width, height, 1.0f);	
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(-0.5f * dx, getPos(outsideBottom, yPos), -1.0f);	
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(0.5f * dx, getPos(outsideBottom, yPos), -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideRight, 1.5f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
		}else{
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideLeft, -1.25f * dx), yPos, -1.0f);	
				gl.glScalef(width, height, 1.0f);	
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(0.0f, getPos(outsideBottom, yPos), -1.0f);	
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideRight, 1.25f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
		}
		yPos += toScreenHeight(160);
		if(index >= buttons -1) return;
		if(index + 3 == buttons){
			// Two buttons on top, three/four bottom
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideLeft, -0.75f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideRight, 0.75f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
		}else{
			// Untested, at max capacity = 8
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideLeft - dx, 1.5f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideLeft, -0.5f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideRight, 0.5f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
			gl.glPushMatrix();
				gl.glTranslatef(getPos(outsideRight + dx, 1.5f * dx), yPos, -1.0f);
				gl.glScalef(width, height, 1.0f);
				drawIcon(gl, mButtons[++index].getTextureID(), mTexBuffer);
			gl.glPopMatrix();
		}
	}
	
	private void drawRotation(GL10 gl, float yPos){
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glActiveTexture(GL10.GL_TEXTURE0);
				
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
		float width = 64.0f / mDisplay.getWidthPixels() * mDisplay.getWidth();
		float height = toScreenHeight(64);
		float dx = -mDisplay.getWidth() + width * 1.75f;
		float outsideLeft = -mDisplay.getWidth() - width;
        float outsideRight = mDisplay.getWidth() + width;
        
		gl.glPushMatrix();
			gl.glTranslatef(getPos(outsideLeft, dx), yPos, -1.0f);	
			gl.glScalef(width, height, 1.0f);	
			drawIcon(gl, mRotateClockwise.getTextureID(), mTexBuffer);
		gl.glPopMatrix();
		gl.glPushMatrix();
			gl.glTranslatef(getPos(outsideRight, -dx), yPos, -1.0f);
			gl.glScalef(width, height, 1.0f);
			drawIcon(gl, mRotateCounterClockwise.getTextureID(), mTexBuffer);
		gl.glPopMatrix();
	}
	
	private float toScreenHeight(int pixels){
		return ((float)pixels) / mDisplay.getHeightPixels() * mDisplay.getHeight();
	}
	
	private float getPos(float notShowing, float showing){
		return notShowing + (showing - notShowing) * smoothstep(mFraction);
	}
	
	private void drawIcon(GL10 gl, int texId, FloatBuffer texBuffer){
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
	}
	
	private float smoothstep(float val){
		return Math.min(val * val * (3.0f - 2.0f * val), 1.0f);
	}
	
	private void drawRotationBoxes(GL10 gl, float yPos){
		float height = 80.0f / mDisplay.getWidthPixels() * mDisplay.getWidth();
		float width = height;
		float left = -mDisplay.getWidth() + width * 1.125f;
        float right = mDisplay.getWidth() - width * 1.125f;
        //gl.glDisable(GL10.GL_TEXTURE_2D);
        //gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glPushMatrix();
			gl.glTranslatef(left, yPos, -1.0f);
			drawRotationBox(gl, width * 1.25f, height);
		gl.glPopMatrix();
		gl.glPushMatrix();
			gl.glTranslatef(right, yPos, -1.0f);
			drawRotationBox(gl, width * 1.25f, height);
		gl.glPopMatrix();
	}
	
	private void drawRotationBox(GL10 gl, float width, float height){
		gl.glPushMatrix();
			//Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
			gl.glScalef(width, height, 1.0f);
			//Draw the vertices as triangle strip
			gl.glBlendFunc(GL10.GL_ZERO, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
		gl.glPopMatrix();
	}
	
	private void drawBackBar(GL10 gl, float height){
		gl.glPushMatrix();
			//Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
			gl.glTranslatef(0.0f, -mDisplay.getHeight() + height, -1.0f);
			gl.glScalef(mDisplay.getWidth(), height, 1.0f);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			//Draw the vertices as triangle strip
			gl.glBlendFunc(GL10.GL_ZERO, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
		gl.glPopMatrix();
	}
	
	public void show(long realtime){
		if(realtime - mInputTime < FADE_TIME + SHOW_TIME){
			// Don't fade again!
			mInputTime = realtime - FADE_TIME;
		}else{
			mInputTime = realtime;
		}
	}
	
	public boolean hide(long realtime){
		if(mFraction != 0){
			mInputTime = realtime - FADE_TIME - SHOW_TIME;
			return true;
		}
		return false;
	}
	
	public boolean click(float x, float y, long time){
		boolean inButtonArea = false;
		if(mButtons == null) return false; // Not ready!
		if(mDisplay == null) return false; // What?
		int height = mButtons.length > 4 ? 160 : 80;
		inButtonArea = y > mDisplay.getHeightPixels() - height;
		if(mFraction != 0.0f){
			if(inButtonArea){
				mExtendDisplayTime = true;
				int buttons = mButtons.length;
				if(y > mDisplay.getHeightPixels() - 80){
					// Bottom
					buttons = buttons > 4 ? 4 - buttons % 2 : buttons;
					int xPos = (int)(x / mDisplay.getWidthPixels() * buttons);
					if(xPos < mButtons.length && xPos >= 0){
						mButtons[xPos].click(time);
					}
				}else{
					int topButtons = buttons > 6 ? 4 : 2;
					float xPos = x / mDisplay.getWidthPixels() * topButtons;
					int buttonIndex = buttons - (topButtons - (int)xPos);
					if(buttonIndex < mButtons.length && buttonIndex >= 0){
						mButtons[buttonIndex].click(time);
					}
				}
				return true;
			}
			if(mShowRotation && mRotateClockwise.doShow()){ // CounterClockwise is shown if clockwise is...
				int middle = mDisplay.getHeightPixels() / 4;
				int top = middle - 50;
				int bottom = middle + 50;
				if(y > top && y < bottom){
					if(x < 80){
						mExtendDisplayTime = true;
						mRotateClockwise.click(time);
						return true;
					}else if (x > mDisplay.getWidthPixels() - 100){
						mExtendDisplayTime = true;
						mRotateCounterClockwise.click(time);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void registerPlayListener(PlayPauseEventHandler listener){
		mPlay.registerEventListener(listener);
	}
	
	public void play(){
		mPlay.play();
	}
	
	public void pause(){
		mPlay.pause();
	}
	
	public boolean isFloating(){
		return mPlayPauseFloating.isPlaying();
	}
	
	public void registerPlayAutoPickerListener(PlayPauseEventHandler listener){
		mPlayPauseAutopicker.registerEventListener(listener);
	}
	
	public void playAutopicker(){
		mPlayPauseAutopicker.play();
	}
	
	public void pauseAutopicker(){
		mPlayPauseAutopicker.pause();
	}
	
	public void registerPlayFloatingListener(PlayPauseEventHandler listener){
		mPlayPauseFloating.registerEventListener(listener);
	}
	
	public void playFloating(){
		mPlayPauseFloating.play();
	}
	
	public void pauseFloating(){
		mPlayPauseFloating.pause();
	}
}
