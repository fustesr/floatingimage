package dk.nindroid.rss.renderers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import dk.nindroid.rss.Display;
import dk.nindroid.rss.gfx.Vec3f;

public class Dimmer {
	private static final float 	zDepth = 1.1f;
	private static Vec3f[]		mVertices;
	private static IntBuffer   	mVertexBuffer;
	private static ByteBuffer  	mIndexBuffer;
	private static IntBuffer	mColorBuffer;
	private static final int 	one = 0x10000;
	private static int r,g,b;
	
	private static final int VERTS = 4;
	
	static {
		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());

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
	
	public static void setColor(float r, float g, float b){
		Dimmer.r = (int)(r * one);
		Dimmer.g = (int)(g * one);
		Dimmer.b = (int)(b * one);
	}
	
	static void setAlpha(float col){
		int alpha = (int)(one * col);
		int[] colors  = { 
				r, g, b, alpha,
				r, g, b, alpha,
				r, g, b, alpha,
				r, g, b, alpha
				};
		ByteBuffer cbb = ByteBuffer.allocateDirect(64);
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asIntBuffer();
		mColorBuffer.put(colors);
		mColorBuffer.position(0);
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
	public static void draw(GL10 gl, float fraction, float finalIntensity, Display display){
		setAlpha(fraction * finalIntensity);
		gl.glPushMatrix();
		
			gl.glLoadIdentity(); // We fill whole screen, never rotate!
			gl.glTranslatef(0, 0, -zDepth);
			gl.glScalef(display.getPortraitWidth() * zDepth, display.getPortraitHeight() * zDepth, 1);
			
			gl.glFrontFace(GL10.GL_CCW);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
			gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuffer);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL10.GL_BLEND);
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
			gl.glDisable(GL10.GL_BLEND);
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glPopMatrix();
	}
}
