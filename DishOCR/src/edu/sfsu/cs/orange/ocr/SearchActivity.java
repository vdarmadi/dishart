package edu.sfsu.cs.orange.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import edu.sfsu.cs.orange.util.Base64;

public class SearchActivity extends Activity {
	private static final String TAG = SearchActivity.class.getSimpleName();
	private Map restaurants = new HashMap<String, String>();
	private String clientId = "cqgqdiwm4861uty3kbolywc1h";
	private ArrayList dishNames = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		final Button searchButton = (Button) findViewById(R.id.buttonSearch);
		final EditText searchText = (EditText) findViewById(R.id.EditTextSearch);

		final ListView listview = (ListView) findViewById(R.id.listview);
		final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		listview.setAdapter(adapter);

		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				try {
					final String item = (String) parent.getItemAtPosition(position);
					String spId = (String) restaurants.get(item);
					String urlPath = "/locations/" + spId + "/menu?client=" + clientId;
					String url = getSignKey(urlPath);
					JSONObject json = queryJson(url);
					JSONArray jsonMainArr = json.getJSONArray("menus");
					if (jsonMainArr.length() == 0) {
						return;
					}
					JSONObject jsonObject = (JSONObject) jsonMainArr.get(0);
					JSONArray menus = jsonObject.getJSONArray("entries");

					// Foods / Menus
					dishNames.clear();
					for (int j = 0; j < menus.length(); j++) { // **line 2**
						JSONObject childJSONObject = menus.getJSONObject(j);
						String dishName = childJSONObject.getString("title");
						dishNames.add(dishName);
					}
					
					Intent intent = new Intent(SearchActivity.this, CaptureActivity.class);
				    intent.putStringArrayListExtra("dishNames", dishNames);
					startActivity(intent);
					finish();

				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String searchTextStr = searchText.getText().toString();
				String urlPath = "/locations/search?q=" + searchTextStr + "&count=100&client=" + clientId;
				try {
					String url = getSignKey(urlPath);
					JSONObject json = queryJson(url);

					JSONArray jsonMainArr = json.getJSONArray("results");
					adapter.clear();
					for (int i = 0; i < jsonMainArr.length(); i++) {  // **line 2**
					     JSONObject childJSONObject = jsonMainArr.getJSONObject(i);
					     String id = childJSONObject.getString("id");
					     
					     JSONObject childJSONObjectGen = childJSONObject.getJSONObject("general");
					     String name = childJSONObjectGen.getString("name");
					     
					     JSONObject childJSONObjectLoc = childJSONObject.getJSONObject("location");
					     String city =  childJSONObjectLoc.getString("city");
					     
					     JSONObject childJSONObjectPh = childJSONObject.getJSONObject("phones");
					     String phone =  childJSONObjectPh.getString("main");
					     
					     String key = name + "-" + city + "-" + phone;
					     adapter.add(key);
					     restaurants.put(key, id);
					}					
					adapter.notifyDataSetChanged();
					listview.setAdapter(adapter);
				} catch (SignatureException e) {
					Log.d(TAG, "SignatureException");
				}
				// Intent intent = new Intent(FullImage1.this, Reco.class);
				// startActivity(intent);
				// finish();
				catch (JSONException e) {
					// TODO Auto-generated catch block
					Log.d(TAG, e.getMessage());
				}
			}
		});

		

	}

	private class StableArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			for (int i = 0; i < objects.size(); ++i) {
				mIdMap.put(objects.get(i), i);
			}
		}

		@Override
		public long getItemId(int position) {
			String item = getItem(position);
			return mIdMap.get(item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}

	private static JSONObject queryJson(String url) {
		try {
			InputStream is = new URL(url).openStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
			return null;
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	private String getSignKey(String urlPath) throws SignatureException {
		String result;
		String signKey = "uF13lWeVxPZafai9D5i_nEOpbScUhTUfu426Yk7qXrA";
		if (signKey.length() % 4 != 0) {
			// The length of the signing key needs to be a multiple of 4
			for (int x = signKey.length() % 4; x < 4; x++) {
				signKey += "=";
			}
		}
		String url;
		try {
			byte[] binaryKey = Base64.decode(signKey.replaceAll("-", "\\+")
					.replaceAll("_", "/"));

			javax.crypto.spec.SecretKeySpec signingKey = new javax.crypto.spec.SecretKeySpec(
					binaryKey, "HmacSHA1");
			javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
			mac.init(signingKey);

			byte[] rawHmac = mac.doFinal(urlPath.getBytes());

			// base64-encode the hmac and remove any trailing equals
			result = Base64.encode(rawHmac).replaceAll("\\+", "-")
					.replaceAll("/", "_");
			int firstEquals = result.indexOf("=");
			if (firstEquals > 0) {
				result = result.substring(0, firstEquals);
			}

			url = "http://api.singleplatform.co" + urlPath + "&sig=" + result;

			System.out.println(url);
			return url;
		} catch (Exception e) {
			throw new java.security.SignatureException(
					"Failed to generate HMAC : " + e.getMessage());
		}
	}
}
