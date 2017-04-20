package dk.nindroid.rss.renderers.osd;

import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import dk.nindroid.rss.R;
import dk.nindroid.rss.RiverRenderer;
import dk.nindroid.rss.data.ImageReference;

public class RotateClockwise extends Button {
	RiverRenderer mRenderer;
	int		mTexId;
	
	public RotateClockwise(RiverRenderer renderer){
		this.mRenderer = renderer;
	}

	@Override
	public void click(long time) {
		ImageReference ir = mRenderer.getSelected();
		ir.turn(time, -90);
	}

	@Override
	public int getTextureID() {
		return mTexId;
	}

	public void init(GL10 gl, Context context) {
		InputStream is = context.getResources().openRawResource(R.drawable.osd_rotate_clockwise);
		Bitmap bmp = BitmapFactory.decodeStream(is);
		
		int[] textures = new int[1];
		gl.glGenTextures(1, textures, 0);
		mTexId = textures[0];
		setTexture(gl, bmp, mTexId);
		
		bmp.recycle();
	}
	public boolean doShow(){
		return mRenderer.getSelected() != null;
	}
}
