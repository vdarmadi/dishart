package com.qualcomm.QCARSamples.ImageTargets;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class FullImage2 extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image2);
		final Button button = (Button) findViewById(R.id.full_yes_btn2);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(FullImage2.this, Reco.class);
				startActivity(intent);
				finish();
			}
		});
		
		final Button button2 = (Button) findViewById(R.id.back_cancel_btn2);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(FullImage2.this, Reco.class);
				startActivity(intent);
				finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.full_image2, menu);
		return true;
	}

}
