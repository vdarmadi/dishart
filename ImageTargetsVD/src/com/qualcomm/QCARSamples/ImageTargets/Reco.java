package com.qualcomm.QCARSamples.ImageTargets;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class Reco extends Activity {
	long mSplashScreenStartTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

		// Apply screen orientation
		setRequestedOrientation(screenOrientation);

		setContentView(R.layout.reco);

		ImageView img = (ImageView) findViewById(R.id.ImageView01);
		img.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Reco.this, FullImage1.class);
				startActivity(intent);
				finish();
			}
		});
		
		ImageView img2 = (ImageView) findViewById(R.id.imageView1);
		img2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Reco.this, FullImage2.class);
				startActivity(intent);
				finish();
			}
		});

		final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Reco.this, ImageTargets.class);
				startActivity(intent);
				finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.reco, menu);
		return true;
	}

}
