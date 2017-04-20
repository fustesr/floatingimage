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
import android.opengl.GLUtils;
import dk.nindroid.rss.R;
import dk.nindroid.rss.gfx.Vec3f;
import dk.nindroid.rss.renderers.floating.positionControllers.Rotation;

public class ShadowPainter {
	private static Bitmap shadow;
	//private static Bitmap canvas;
	private static int mTextureID;
	private static FloatBuffer mTexBuffer;
	private static Vec3f[]		mVertices;
	private static IntBuffer   mVertexBuffer;
	private static ByteBuffer  mIndexBuffer;
	
	private static final int VERTS = 4;
	
	public static void init(Context context){
		InputStream shadowIS = context.getResources().openRawResource(R.drawable.image_shadow);
		Options opts = new Options();
		opts.inPreferredConfig = Config.ARGB_8888;
		shadow = BitmapFactory.decodeStream(shadowIS, null, opts);
		
		initPlane();
	}
	
	private static void initPlane(){
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
	
	public static void initTexture(GL10 gl){		
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
        
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, shadow, 0);
        //GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, shadow);
		
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
	
	public static void setState(GL10 gl){
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glEnable(GL10.GL_BLEND);
	}
	
	public static void unsetState(GL10 gl){
		gl.glDisable(GL10.GL_BLEND);
		gl.glDisable(GL10.GL_TEXTURE_2D);
	}
	
	/**
	 * Draw glow around image at x, y, z with size szX, szY
	 * 
	 * @param gl
	 * @param x
	 * @param y
	 * @param z
	 * @param szX
	 * @param szY
	 * @param szZ
	 */
	public static void draw(GL10 gl, float x, float y, float z, Rotation rotationA, Rotation rotationB, float imageRotation, float scaleX, float scaleY){
		// Draw shadow
		gl.glPushMatrix();
			gl.glTranslatef(-0.2f + x, -0.2f + y, z);
			gl.glRotatef(rotationA.getAngle(), rotationA.getX(), rotationA.getY(), rotationA.getZ());
			gl.glRotatef(rotationB.getAngle(), rotationB.getX(), rotationB.getY(), rotationB.getZ());
			gl.glRotatef(imageRotation, 0, 0, 1);
			gl.glScalef(1.15f * scaleX, 1.15f * scaleY, 1);
			gl.glActiveTexture(GL10.GL_TEXTURE0);
	        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
	        gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
		gl.glPopMatrix();
	}
}
