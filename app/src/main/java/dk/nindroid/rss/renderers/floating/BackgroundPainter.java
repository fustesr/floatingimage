package dk.nindroid.rss.renderers.floating;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import dk.nindroid.rss.Display;
import dk.nindroid.rss.MainActivity;
import dk.nindroid.rss.R;
import dk.nindroid.rss.gfx.Vec3f;

public class BackgroundPainter {
	public static final int GREY = 0;
	public static final int BLACK = 1;
	public static final int WHITE = 2;
	public static final int BLUE = 3;
	public static final int GREEN = 4;
	public static final int RED = 5;
	public static final int YELLOW = 6;
	public static final int ALL = 7;
	public static final int PURE_BLACK = 8;
	
	private static final float zDepth = 15.0f;
	
	private int mTextureID;
	private FloatBuffer mTexBuffer;
	private Vec3f[]		mVertices;
	private IntBuffer   mVertexBuffer;
	private ByteBuffer  mIndexBuffer;
	
	private static final int VERTS = 4;
	
	public BackgroundPainter(){
		
		initPlane();
	}
	
	private void initPlane(){
		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();
        
        float tex[] = {
        	0.0f,  0.0f,
        	0.0f,  0.0f,	
        	0.0f,  0.0f,
        	0.0f,  0.0f,
        };
        mTexBuffer.put(tex);
        mTexBuffer.position(0);
		
		
		int one = 0x10000;
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
	}
	
	public void initTexture(GL10 gl, MainActivity activity, int backgroundColor){	
		InputStream shadowIS;
		Context context = activity.context();
		switch(backgroundColor){
		case GREY:
			shadowIS = context.getResources().openRawResource(R.drawable.background);
			break;
		case BLACK:
			shadowIS = context.getResources().openRawResource(R.drawable.background_dark);
			break;
		case WHITE:
			shadowIS = context.getResources().openRawResource(R.drawable.background_white);
			break;
		case BLUE:
			shadowIS = context.getResources().openRawResource(R.drawable.background_blue);
			break;
		case GREEN:
			shadowIS = context.getResources().openRawResource(R.drawable.background_green);
			break;
		case RED:
			shadowIS = context.getResources().openRawResource(R.drawable.background_red);
			break;
		case YELLOW:
			shadowIS = context.getResources().openRawResource(R.drawable.background_yellow);
			break;
		case ALL:
			shadowIS = context.getResources().openRawResource(R.drawable.background_all);
			break;
		case PURE_BLACK:
			shadowIS = null;
			break;
		default:
			shadowIS = context.getResources().openRawResource(R.drawable.background);
			break;
		}
		
		if(shadowIS != null){
			Options opts = new Options();
			opts.inPreferredConfig = Config.ARGB_8888;
			Bitmap bg = BitmapFactory.decodeStream(shadowIS, null, opts);		
			
			int[] textures = new int[1];
	        gl.glGenTextures(1, textures, 0);
			mTextureID = textures[0];		
	        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
	
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
	                GL10.GL_NEAREST);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D,
	                GL10.GL_TEXTURE_MAG_FILTER,
	                GL10.GL_LINEAR);
	
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
	                GL10.GL_CLAMP_TO_EDGE);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
	                GL10.GL_CLAMP_TO_EDGE);
	
	        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
	                GL10.GL_BLEND);
	        
	        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bg, 0);
	        //GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bg);
			
	        bg.recycle();
	        
	        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
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
	        
	        // Set drawing params
	        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,GL10.GL_REPEAT);
	        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		}
	}
	
	public void draw(GL10 gl, Display display, int backgroundColor){
		gl.glDisable(GL10.GL_BLEND);
		
		int error = gl.glGetError();
		if(error != 0){
			Log.e("Floating Image", "GL Error before painting background: " + GLU.gluErrorString(error));
			gl.glGetError(); // Clear errors
		}
		// Draw background
		gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glTranslatef(0, 0, -zDepth);
			gl.glScalef(display.getPortraitWidth() * zDepth, display.getPortraitHeight() * zDepth, 1);
			gl.glActiveTexture(GL10.GL_TEXTURE0);
	        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			if(backgroundColor != PURE_BLACK){
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
				gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
			}
			error = gl.glGetError();
			if(error != 0){
				Log.e("Floating Image", "GL Error painting background: " + GLU.gluErrorString(error));
			}
		gl.glPopMatrix();
	}
}
