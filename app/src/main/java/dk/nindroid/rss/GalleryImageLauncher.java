package dk.nindroid.rss;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import dk.nindroid.rss.data.LocalImage;
import dk.nindroid.rss.menu.GallerySettings;
import dk.nindroid.rss.settings.FeedsDbAdapter;
import dk.nindroid.rss.settings.Settings;

public class GalleryImageLauncher extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri uri = getIntent().getData();
		File f = null;
		if(uri.getScheme().equalsIgnoreCase("file")){
			try {
				f = new File(new URI(getIntent().getData().toString()));
			} catch (URISyntaxException e) {
				Log.e("Floating Image", "Cannot read uri", e);
				this.finish();
				return;
			}
		}else if(uri.getScheme().equalsIgnoreCase("content")){
			try{
				Cursor cursor = getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA, android.provider.MediaStore.Images.ImageColumns._ID}, null, null, null);
				if(cursor.moveToFirst())
				{
					String uriString = cursor.getString(0);
					if(uriString != null){
						Uri dataUri = Uri.parse(uriString);
						if(dataUri != null){
						    String pathUri = dataUri.getPath();
						    f = new File(pathUri);
						}
					}
				}
				cursor.close();
			}catch(Exception e){
				Toast.makeText(this, "Apologies, image cannot be shown in Floating Image.", Toast.LENGTH_LONG).show();
				this.finish();
				return;
			}
		}
		
		if(f == null){
			showUri(uri.toString());
			return;
		}
		
		Editor e = this.getSharedPreferences(GallerySettings.SHARED_PREFS_NAME, 0).edit();
		e.putBoolean("galleryMode", true);
		e.commit();
		
		FeedsDbAdapter db = new FeedsDbAdapter(this).open();
		String dirPath = f.getParent();
		Cursor c = db.fetchFeed(dirPath);
		if(c != null && c.moveToFirst()){
			showImage(c.getInt(c.getColumnIndex(FeedsDbAdapter.KEY_ROWID)), f);
			c.close();
		}else{
			c.close();
			db.addFeed(dirPath, dirPath, Settings.TYPE_LOCAL, "");
			c = db.fetchFeed(dirPath);
			showImage(c.getInt(c.getColumnIndex(FeedsDbAdapter.KEY_ROWID)), f);
			c.close();
		}
		db.close();
		
		this.finish();
	}
	void showUri(String uri){
		Intent intent = new Intent(this, ShowStreams.class);
		intent.putExtra(ShowStreams.SHOW_FEED_ID, FeedController.FORCE_CONTENT_URI);
		intent.putExtra(ShowStreams.SHOW_IMAGE_ID, uri);
		intent.putExtra(ShowStreams.SETTINGS_NAME, GallerySettings.SHARED_PREFS_NAME);
		this.startActivityForResult(intent, 0);
	}
	
	void showImage(int feedId, File f){
		Intent intent = new Intent(this, ShowStreams.class);
		intent.putExtra(ShowStreams.SHOW_FEED_ID, feedId);
		intent.putExtra(ShowStreams.SHOW_IMAGE_ID, LocalImage.getID(f));
		intent.putExtra(ShowStreams.SETTINGS_NAME, GallerySettings.SHARED_PREFS_NAME);
		this.startActivity(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		finish();
	}
}
