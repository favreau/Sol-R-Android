package com.example.raytracerclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.example.raytracerclient.util.SystemUiHider;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {

	private static EditText edURL = null;
	private static EditText edModel = null;
	private static ImageView ivRendering = null;
	private static String imageContent = null;
	private static boolean renderingInProgress=false;
	private PointerCoords mousePos = new PointerCoords();
	private PointerCoords oldMousePos = new PointerCoords();
	private PointerCoords viewAngles = new PointerCoords();

	private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {
		protected Long doInBackground(URL... urls) {
			renderingInProgress = true;
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			
			// Prepare a request object
			HttpGet httpget = new HttpGet( edURL.getText().toString() + 
					"/get?irt=" + edModel.getText().toString() + 
					"&postprocessing=0&bkcolor=255,255,255&size=0&quality=1" +
					"&rotation=" + viewAngles.x + "," + viewAngles.y + ",0&scene=0&distance=-10000&depth=50000&fake=0&values=0,0");

			try {
				// Execute the request
				HttpResponse response = httpclient.execute(httpget, localContext);

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();

				imageContent = getASCIIContentFromEntity(entity);


			} 
			catch (Exception e) {
				ivRendering.setBackgroundColor(Color.RED);
			}
			return null;        		
		}

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			imageContent = imageContent.substring(21);
			byte[] b = Base64.decode(imageContent, Base64.DEFAULT);
			Bitmap decodedByte = BitmapFactory.decodeByteArray(b, 0, b.length);
			ivRendering.setImageBitmap(decodedByte);
			renderingInProgress = false;
		}

	}	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

		edURL     = (EditText)findViewById(R.id.etURL);
		edModel   = (EditText)findViewById(R.id.etModel);
		ivRendering  =(ImageView)findViewById(R.id.ivRendering);
		
		mousePos.x = ivRendering.getWidth()/2;
		mousePos.y = ivRendering.getHeight()/2;
		oldMousePos.x = ivRendering.getWidth()/2;
		oldMousePos.y = ivRendering.getHeight()/2;
		viewAngles.x = 20;
		viewAngles.y = 20;

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.btnRender).setOnTouchListener(mDelayHideTouchListener);
		ivRendering.setOnTouchListener(mChangeRotation);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
		InputStream in = entity.getContent();
		StringBuffer out = new StringBuffer();
		int n = 1;
		while (n>0) {
			byte[] b = new byte[4096];
			n =  in.read(b);
			if (n>0) out.append(new String(b, 0, n));
		}
		return out.toString();
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if( !renderingInProgress )
			{
				viewAngles.x = 20;
				viewAngles.y = 0;
				if( motionEvent.getAction() == MotionEvent.ACTION_DOWN )
				{
					DownloadFilesTask task = new DownloadFilesTask();
					URL url;
					try {
						url = new URL(edURL.getText().toString()); // To remove
						task.execute(url);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return false;
		}
	};

	View.OnTouchListener mChangeRotation = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent dragEvent) {
			//if( dragEvent.getAction()==MotionEvent.ACTION_MOVE )
			{
				mousePos.x = dragEvent.getX();
				mousePos.y = dragEvent.getY();
				float value = (oldMousePos.x-mousePos.x)/100;
				if( Math.abs(value)<1 )
				{
					viewAngles.y += 20*Math.asin(value);  
				}
				value = (oldMousePos.y-mousePos.y)/100;
				if( Math.abs(value)<1 )
				{
					viewAngles.x += 20*Math.asin(value);  
				}
				if( !renderingInProgress ) 
				{
					DownloadFilesTask task = new DownloadFilesTask();
					URL url;
					try {
						url = new URL(edURL.getText().toString()); // To remove
						task.execute(url);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				oldMousePos.x = mousePos.x;
				oldMousePos.y = mousePos.y;
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
	}
}
