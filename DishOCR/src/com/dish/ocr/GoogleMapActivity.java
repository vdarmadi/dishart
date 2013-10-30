package com.dish.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.dish.ocr.R;
import com.dish.util.Base64;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class GoogleMapActivity extends FragmentActivity {

	private GoogleMap mMap;
	double longitude;
	double latitude;
	private HashMap<String, Restaurant> restaurants = new HashMap<String, Restaurant>();
	private static final String TAG = GoogleMapActivity.class.getSimpleName();
	private  Location location;
	private String clientId = "cqgqdiwm4861uty3kbolywc1h";
	private ArrayList<MenuData> dishNames = new ArrayList<MenuData>(); // Holding menu data for a restaurant.
	private static boolean isFirstLaunch; // True if this is the first time the app is being run
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public void onBackPressed() {
		Log.d(TAG, "Back pressed");
		finish();
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
			return null;
		}
	}

	 private boolean checkFirstLaunch() {
	    try {
	      PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
	      int currentVersion = info.versionCode;
	      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	      int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
	      if (lastVersion == 0) {
	        isFirstLaunch = true;
	      } else {
	        isFirstLaunch = false;
	      }
	      if (currentVersion > lastVersion) {
	    	prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
	        return true;
	      }
	    } catch (PackageManager.NameNotFoundException e) {
	      Log.w(TAG, e);
	    }
	    return false;
	  }
	 
	private JSONObject getLocationInfo(String address) {
		StringBuilder stringBuilder = new StringBuilder();
		try {

			address = address.replaceAll(" ", "%20");

			HttpPost httppost = new HttpPost(
					"http://maps.google.com/maps/api/geocode/json?address="
							+ address + "&sensor=false");
			HttpClient client = new DefaultHttpClient();
			HttpResponse response;
			stringBuilder = new StringBuilder();

			response = client.execute(httppost);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}

		return jsonObject;
	}
	 
	private double getLat(JSONObject jsonObject) {
		try {
			double latitude = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
					.getJSONObject("geometry").getJSONObject("location")
					.getDouble("lat");
			return latitude;
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
			return 0;
		}
	}
	private double getLong(JSONObject jsonObject) {
		try {
			double longitute = ((JSONArray) jsonObject.get("results"))
					.getJSONObject(0).getJSONObject("geometry")
					.getJSONObject("location").getDouble("lng");
			return longitude;
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
			return 0;
		}
	}
	
	 private Address getLatLongByAddress(String strAddress) {
		 Geocoder coder = new Geocoder(this);
		 List<Address> address;

		 try {
			 // e.g. "1600 Amphitheatre Parkway, Mountain View, CA".
		     address = coder.getFromLocationName(strAddress,5);
		     if (address == null) {
		         return null;
		     }
		     Address location = address.get(0);
		     return location;
		 }
		 catch(Exception ex) {
			 Log.e(TAG, ex.getMessage());
		 }
		 return null;
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		// mMap = ((MapFragment)
		// getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		//LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
		//lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
		//lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		
		if (checkFirstLaunch()) {
			// Record the last version for which we last displayed the What's New (Help) page
	        
	        Intent intent = new Intent(GoogleMapActivity.this, HelpActivity.class);
	        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	        
	        // Show the default page on a clean install, and the what's new page on an upgrade.
	        String page = HelpActivity.WHATS_NEW_PAGE;
	        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
	        startActivity(intent);
			finish();
			return;
		}
		
		GPSTracker gps = new GPSTracker(this);
		if(gps.canGetLocation()){ // gps enabled} // return boolean true/false
			this.latitude=gps.getLatitude(); // returns latitude
			this.longitude=gps.getLongitude(); // returns longitude
		}
		
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			private ProgressDialog pd;

			@Override
			protected void onPreExecute() {
				// Move the camera in the center of the location first. Otherwise it will show Africa.
				mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
				CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude));
				mMap.moveCamera(center);

				pd = new ProgressDialog(GoogleMapActivity.this);
				pd.setTitle("Locating Restaurants Nearby...");
				pd.setMessage("Please wait.");
				pd.setCancelable(false);
				pd.setIndeterminate(true);
				pd.show();
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					Intent intent = getIntent();
					restaurants = (HashMap<String, Restaurant>) intent.getSerializableExtra("restaurants");
					if (restaurants == null || restaurants.isEmpty()) {
						if (restaurants == null) { 
							restaurants = new HashMap<String, Restaurant>(); 
						}
						restaurants.clear();
						Set phones = new HashSet<String>();

						URL url2 = new URL("http://api.locu.com/v1_0/venue/search/?api_key=27412850c47e4141c8d5948abdf63392eb33d863&location="+latitude+","+longitude+"&radius=1000&has_menu=true&category=restaurant");
						URLConnection urlConnection = url2.openConnection();
									
						InputStream is2 = urlConnection.getInputStream();
						BufferedReader rd2 = new BufferedReader(new InputStreamReader(is2, Charset.forName("UTF-8")));
						String jsonText2 = readAll(rd2);
						JSONObject json2 = new JSONObject(jsonText2);

						JSONArray jsonArray = json2.getJSONArray("objects");
						
						for (int i = 0; i < jsonArray.length(); i++) {  // **line 2**
						     JSONObject childJSONObject = jsonArray.getJSONObject(i);
						     String id = childJSONObject.getString("id");
						     double lat = childJSONObject.getDouble("lat");
						     double lon = childJSONObject.getDouble("long");
						     String phone = childJSONObject.getString("phone").replaceAll("[^0-9]", "").trim();
						     String name = childJSONObject.getString("name");
						     
						     Restaurant r = new Restaurant();
						     r.setId(id);
						     r.setLatitude(lat);
						     r.setLongitude(lon);
						     r.setName(name);
						     r.setSource("Locu");
						     if(!phones.contains(phone)) {
							     restaurants.put(name, r);
							     phones.add(phone); 
						     }						
						}
						String urlPath = "http://adri-silvy.com/android/restaurants.php?lat="+latitude+"&long="+longitude;
						try {
							JSONObject json = queryJson(urlPath);
							JSONArray jsonMainArr = json.getJSONArray("results");
							for (int i = 0; i < jsonMainArr.length(); i++) { // **line 2**
								JSONObject childJSONObject = jsonMainArr.getJSONObject(i);
								String id = childJSONObject.getString("location_id");
								String name = childJSONObject.getString("name");
								double rLat = childJSONObject.getDouble("latitude");
								double rLong = childJSONObject.getDouble("longitude");
								String ph = childJSONObject.getString("phone").replaceAll("[^0-9]", "").trim();
								String address1 = childJSONObject.getString("address1");
								String city = childJSONObject.getString("city");
								String region = childJSONObject.getString("region");
								String postcode = childJSONObject.getString("postcode");
								String strAddress = address1 + "," + city + "," + postcode + "," + region;
								Address location = getLatLongByAddress(strAddress);
								if (location != null) {
									rLat = location.getLatitude();
									rLong = location.getLongitude();
								} else {
									Log.e(TAG, "Get Lat Long by Address Failed " + strAddress);
									JSONObject jsonObject = getLocationInfo(strAddress);
									double lat = getLat(jsonObject);
									double lon = getLong(jsonObject);
									if (lat != 0) { rLat = lat; }
									if (lon != 0) { rLong = lon; }
								}
								Restaurant r = new Restaurant();
								r.setId(id);
								r.setName(name);
								r.setLatitude(rLat);
								r.setLongitude(rLong);
								r.setSource("SP");
								if(!phones.contains(ph)) {
									restaurants.put(name, r);
									phones.add(ph);
								}
							}
						} catch (JSONException e) {
							Log.e(TAG, e.getMessage());
						}
						//Thread.sleep(5000);
					}
				} 
				//catch (InterruptedException e) {
				//	Log.e(TAG, e.getMessage());
				//} 
				catch (MalformedURLException e) {
					Log.e(TAG, e.getMessage());
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage());
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				pd.dismiss();
				mMap.setMyLocationEnabled(true);
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				Set latLong = new HashSet<String>();
				for (String id : (Set<String>)restaurants.keySet()) {
					Restaurant r = restaurants.get(id);
					float marker = 0;
					//if ("SP".equals(r.getSource())) {
					marker = BitmapDescriptorFactory.HUE_RED;
					//} else {
					//	marker = BitmapDescriptorFactory.HUE_ORANGE;
					//}
					double finalLat = r.getLatitude();
					double finalLong = r.getLongitude();
					if (latLong.contains(Double.toString(finalLat) + Double.toString(finalLong))) {
						double start = 0.001;
						double end = 0.002;
						double random = new java.util.Random().nextDouble();
						double randomOffset = start + (random * (end - start));
						finalLong += randomOffset;
					}
					mMap.addMarker(new MarkerOptions()
					.position(new LatLng(finalLat, finalLong))
					.title(r.getName())
					.draggable(true)
					.snippet(r.getName())
					.icon(BitmapDescriptorFactory.defaultMarker(marker)));
					latLong.add(Double.toString(finalLat) + Double.toString(finalLong));
				}
				
				mMap.getUiSettings().setCompassEnabled(true);
				mMap.getUiSettings().setZoomControlsEnabled(true);
				//Log.d(TAG, "Position: " + latitude + ", " + longitude);
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18));
				mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {			
					@Override
					public void onInfoWindowClick(Marker arg0) {
						
						final String item = arg0.getTitle();
				    	// Access HTTP async, this is issue after Android 3.0.
						AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
							private ProgressDialog pd;
							@Override
							protected void onPreExecute() {
								pd = new ProgressDialog(GoogleMapActivity.this);
								pd.setTitle("Loading menu...");
								pd.setMessage("Please wait.");
								pd.setCancelable(false);
								pd.setIndeterminate(true);
								pd.show();
							}

							@Override
							protected Void doInBackground(Void... arg0) {
								try {
									List<String> dishNameBlacklist = Arrays.asList("Curry","Bbq","Beef","Appetizers","Beverages", "Chicken", "Desserts", "Family Meals", "Home Style Sides", "Individual Meals", "Turkey", "Kids Meals");
									Restaurant r = restaurants.get(item);
									if ("SP".equals(r.getSource())) {
										String spId = r.getId();
										String urlPath = "/locations/" + spId + "/menu?client=" + clientId;
										String url = getSignKey(urlPath);
										JSONObject json = queryJson(url);
										JSONArray jsonMainArr = json.getJSONArray("menus");
										if (jsonMainArr.length() == 0) {
											return null;
										}
										JSONObject jsonObject = (JSONObject) jsonMainArr.get(0);
										JSONArray menus = jsonObject.getJSONArray("entries");
	
										// Foods / Menus
										dishNames.clear(); // This is ArrayList of parceables "MenuData".
										ArrayList<String> list = new ArrayList<String>(); // This is for menu data.
										String section = ""; // Name of the section.
										for (int j = 0; j < menus.length(); j++) {
											JSONObject childJSONObject = menus.getJSONObject(j);
											String dishName = childJSONObject.getString("title");
											String type = childJSONObject.getString("type"); // SinglePlatform always start with section then items.
											if ("section".equals(type)) {
												if (!list.isEmpty() && !section.isEmpty()) { // If this is not first iteration.
													MenuData md = new MenuData();
													md.setItems(list);
													md.setSection(section);
													dishNames.add(md);
													list = new ArrayList<String>(); // Refresh Menu list
													section = dishName; // next.
												} else { // first time
													section = dishName;
												}
											}
											else {
												list.add(dishName); // If normal item then put into the list.
											}
										}
									}
									else if ("Locu".equals(r.getSource())) {
										String locuId = r.getId();
										String url = "http://api.locu.com/v1_0/venue/"+locuId+"/?api_key=27412850c47e4141c8d5948abdf63392eb33d863";
										JSONObject json2 = queryJson(url);

										JSONArray jsonArray = json2.getJSONArray("objects");
										JSONObject jsonObject = jsonArray.getJSONObject(0);
										JSONArray jsonArray2 = jsonObject.getJSONArray("menus");
																				
										for (int i = 0; i < jsonArray2.length(); i++) {  // **line 2**
										     JSONObject childJSONObject = jsonArray2.getJSONObject(i);
										     JSONArray jsonArray3 = childJSONObject.getJSONArray("sections");
										     for (int j = 0; j < jsonArray3.length(); j++) {
										    	 JSONObject jsonObject2 = jsonArray3.getJSONObject(j);
										    	 String sectionName = jsonObject2.getString("section_name"); // Get section name.
										    	 JSONArray jsonArray4 = jsonObject2.getJSONArray("subsections");
										    	 ArrayList<String> list = new ArrayList<String>(); // Holding menu data for 1 section.
										    	 for (int k = 0; k < jsonArray4.length(); k++) {
										    		 JSONObject jsonObject3 = jsonArray4.getJSONObject(k);
										    		 JSONArray jsonArray5 = jsonObject3.getJSONArray("contents");
										    		 for (int l = 0; l < jsonArray5.length(); l++) {
										    			 JSONObject jsonObject4 = jsonArray5.getJSONObject(l);
										    			 if ("ITEM".equals(jsonObject4.get("type"))) {
										    				 String dishName = jsonObject4.getString("name");
										    				 if (!dishNameBlacklist.contains(dishName)) {
										    					 list.add(dishName); // Add item into the current section.
										    				 }
										    			 }
										    		 }
										    	 } 
										    	 MenuData md = new MenuData();
										    	 md.setItems(list);
										    	 md.setSection(sectionName);
										    	 dishNames.add(md);
										     }
										}
									}
									Intent intent = new Intent(GoogleMapActivity.this, CaptureActivity.class);
									intent.putParcelableArrayListExtra("dishNames", dishNames); // Send to capture activity.
									intent.putExtra("restaurants", restaurants);
									startActivity(intent);
									finish();
								} catch (SignatureException e) {
		        					Log.e(TAG, e.getMessage());
								} catch (JSONException e) {
		        					Log.e(TAG, e.getMessage());
								}
								return null;
							}

							@Override
							protected void onPostExecute(Void result) {
								pd.dismiss();
							}
						};
						task.execute((Void[]) null);					
						
						
					}
				});

				
			}
		};
		task.execute((Void[]) null);		
		
		
		
//		mMap.addMarker(new MarkerOptions()
//				.position(latLng)
//				.title("My Spot")
//				.snippet("This is my spot!")
//				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
//		
//		mMap.addMarker(new MarkerOptions()
//		.position(new LatLng(-32.796923, 151.922433))
//		.title("My Spot 2")
//		.snippet("This is my spot 2!")
//		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
				
		
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

			return url;
		} catch (Exception e) {
			throw new java.security.SignatureException(
					"Failed to generate HMAC : " + e.getMessage());
		}
	}
}
