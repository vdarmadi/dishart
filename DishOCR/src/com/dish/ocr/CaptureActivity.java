/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dish.ocr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dish.ocr.R;
import com.dish.ocr.camera.CameraManager;
import com.dish.ocr.camera.ShutterButton;
import com.dish.ocr.language.LanguageCodeHelper;
import com.dish.ocr.language.TranslateAsyncTask;
import com.googlecode.tesseract.android.TessBaseAPI;



/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback, 
  ShutterButton.OnShutterButtonListener {

  private static final String TAG = CaptureActivity.class.getSimpleName();
  
  // Note: These constants will be overridden by any default values defined in preferences.xml.
  
  /** ISO 639-3 language code indicating the default recognition language. */
  public static final String DEFAULT_SOURCE_LANGUAGE_CODE = "eng";
  
  /** ISO 639-1 language code indicating the default target language for translation. */
  public static final String DEFAULT_TARGET_LANGUAGE_CODE = "es";
  
  /** The default online machine translation service to use. */
  public static final String DEFAULT_TRANSLATOR = "Bing Translator";
  
  /** The default OCR engine to use. */
  public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract";
  
  /** The default page segmentation mode to use. */
  public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";
  
  /** Whether to use autofocus by default. */
  public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = true;
  
  /** Whether to beep by default when the shutter button is pressed. */
  public static final boolean DEFAULT_TOGGLE_BEEP = true;
  
  /** Whether to initially show a looping, real-time OCR display. */
  public static final boolean DEFAULT_TOGGLE_CONTINUOUS = true;
  
  /** Whether to initially reverse the image returned by the camera. */
  public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;
  
  /** Whether to enable the use of online translation services be default. */
  public static final boolean DEFAULT_TOGGLE_TRANSLATION = true;
  
  /** Whether the light should be initially activated by default. */
  public static final boolean DEFAULT_TOGGLE_LIGHT = false;

  
  /** Flag to display the real-time recognition results at the top of the scanning screen. */
  private static final boolean CONTINUOUS_DISPLAY_RECOGNIZED_TEXT = true;
  
  /** Flag to display recognition-related statistics on the scanning screen. */
  private static final boolean CONTINUOUS_DISPLAY_METADATA = true;
  
  /** Flag to enable display of the on-screen shutter button. */
  private static final boolean DISPLAY_SHUTTER_BUTTON = true;
  
  /** Languages for which Cube data is available. */
  static final String[] CUBE_SUPPORTED_LANGUAGES = { 
    "ara", // Arabic
    "eng", // English
    "hin" // Hindi
  };

  /** Languages that require Cube, and cannot run using Tesseract. */
  private static final String[] CUBE_REQUIRED_LANGUAGES = { 
    "ara" // Arabic
  };
  
  /** Resource to use for data file downloads. */
  static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";
  
  /** Download filename for orientation and script detection (OSD) data. */
  static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";
  
  /** Destination filename for orientation and script detection (OSD) data. */
  static final String OSD_FILENAME_BASE = "osd.traineddata";
  
  /** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
  static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results
  
  // Context menu
  private static final int SETTINGS_ID = Menu.FIRST;
  private static final int ABOUT_ID = Menu.FIRST + 1;
  
  // Options menu, for copy to clipboard
  private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;
  private static final int OPTIONS_COPY_TRANSLATED_TEXT_ID = Menu.FIRST + 1;
  private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 2;
  private static final int OPTIONS_SHARE_TRANSLATED_TEXT_ID = Menu.FIRST + 3;

  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private ViewfinderView viewfinderView;
  private SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;
  private TextView statusViewBottom;
  private TextView statusViewTop;
  private TextView ocrResultView;
  private TextView translationView;
  private TextView textResult;
  private static String tResult;
  private View cameraButtonView;
  private View resultView;
  private View progressView;
  private OcrResult lastResult;
  private Bitmap lastBitmap;
  private boolean hasSurface;
  private BeepManager beepManager;
  private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
  private String sourceLanguageCodeOcr; // ISO 639-3 language code
  private String sourceLanguageReadable; // Language name, for example, "English"
  private String sourceLanguageCodeTranslation; // ISO 639-1 language code
  private String targetLanguageCodeTranslation; // ISO 639-1 language code
  private String targetLanguageReadable; // Language name, for example, "English"
  private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
  private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
  private String characterBlacklist;
  private String characterWhitelist;
  private ShutterButton shutterButton;
//  private ToggleButton torchButton;
  private boolean isTranslationActive; // Whether we want to show translations
  private boolean isContinuousModeActive; // Whether we are doing OCR in continuous mode
  private SharedPreferences prefs;
  private OnSharedPreferenceChangeListener listener;
  private ProgressDialog dialog; // for initOcr - language download & unzip
  private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
  private boolean isEngineReady;
  private boolean isPaused;
  private static boolean isFirstLaunch; // True if this is the first time the app is being run
  private ImageView imageResult; // Place to display image when text is recognized.
  private double percentage = 0;
  private boolean substrMatch;
  private boolean isLoadingImg;
  private Map<String, List<String>> dishNames = new LinkedHashMap<String, List<String>>(); // Holding menu.
  private ArrayList<MenuData> mdl; // Raw menu data.
  private String prevDishName = "";
  private HashMap<String, Restaurant> restaurants; // Caching restaurants.
  //private Button backButton;

  Handler getHandler() {
    return handler;
  }

  TessBaseAPI getBaseApi() {
    return baseApi;
  }
  
  CameraManager getCameraManager() {
    return cameraManager;
  }
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    checkFirstLaunch();
    
    if (isFirstLaunch) {
      setDefaultPreferences();
    }
    
    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.capture);
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    cameraButtonView = findViewById(R.id.camera_button_view);
    resultView = findViewById(R.id.result_view);
    
    statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
    registerForContextMenu(statusViewBottom);
    statusViewTop = (TextView) findViewById(R.id.status_view_top);
    registerForContextMenu(statusViewTop);
    
    handler = null;
    lastResult = null;
    hasSurface = false;
    beepManager = new BeepManager(this);
    
    // Camera shutter button
    if (DISPLAY_SHUTTER_BUTTON) {
      shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
      shutterButton.setOnShutterButtonListener(this);
    }
   
    ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
    registerForContextMenu(ocrResultView);
    translationView = (TextView) findViewById(R.id.translation_text_view);
    registerForContextMenu(translationView);
    
    textResult = (TextView) findViewById(R.id.text_result);
    textResult.setVisibility(View.INVISIBLE);

    progressView = (View) findViewById(R.id.indeterminate_progress_indicator_view);

    cameraManager = new CameraManager(getApplication());
    viewfinderView.setCameraManager(cameraManager);
    
    Intent intent = getIntent();
    mdl = intent.getParcelableArrayListExtra("dishNames"); // Transfer data here.
    restaurants = (HashMap<String, Restaurant>) intent.getSerializableExtra("restaurants"); // Caching restaurants.
    //backButton = (Button) findViewById(R.id.backButton);
    //backButton.setOnClickListener(new OnClickListener() {
	//	@Override
	//	public void onClick(View arg0) {
	//		Intent intent = new Intent(CaptureActivity.this, GoogleMapActivity.class);
	//		startActivity(intent);
	//	}
	//});



    // Set listener to change the size of the viewfinder rectangle.
//    viewfinderView.setOnTouchListener(new View.OnTouchListener() {
//      int lastX = -1;
//      int lastY = -1;
//
//      @Override
//      public boolean onTouch(View v, MotionEvent event) {
//        switch (event.getAction()) {
//        case MotionEvent.ACTION_DOWN:
//          lastX = -1;
//          lastY = -1;
//          return true;
//        case MotionEvent.ACTION_MOVE:
//          int currentX = (int) event.getX();
//          int currentY = (int) event.getY();
//
//          try {
//            Rect rect = cameraManager.getFramingRect();
//
//            final int BUFFER = 50;
//            final int BIG_BUFFER = 60;
//            if (lastX >= 0) {
//              // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
//              if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
//                  && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
//                // Top left corner: adjust both top and left sides
//                cameraManager.adjustFramingRect( 2 * (lastX - currentX), 2 * (lastY - currentY));
//                viewfinderView.removeResultText();
//              } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER)) 
//                  && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
//                // Top right corner: adjust both top and right sides
//                cameraManager.adjustFramingRect( 2 * (currentX - lastX), 2 * (lastY - currentY));
//                viewfinderView.removeResultText();
//              } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
//                  && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
//                // Bottom left corner: adjust both bottom and left sides
//                cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
//                viewfinderView.removeResultText();
//              } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER)) 
//                  && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
//                // Bottom right corner: adjust both bottom and right sides
//                cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
//                viewfinderView.removeResultText();
//              } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
//                  && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
//                // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
//                cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
//                viewfinderView.removeResultText();
//              } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
//                  && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
//                // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
//                cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
//                viewfinderView.removeResultText();
//              } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
//                  && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
//                // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
//                cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
//                viewfinderView.removeResultText();
//              } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
//                  && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
//                // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
//                cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
//                viewfinderView.removeResultText();
//              }     
//            }
//          } catch (NullPointerException e) {
//            Log.e(TAG, "Framing rect not available", e);
//          }
//          v.invalidate();
//          lastX = currentX;
//          lastY = currentY;
//          return true;
//        case MotionEvent.ACTION_UP:
//          lastX = -1;
//          lastY = -1;
//          return true;
//        }
//        return false;
//      }
//    });
    
    imageResult = (ImageView) findViewById(R.id.image_result); // Initialize ImageView.
    imageResult.setVisibility(View.INVISIBLE); // Don't display it first.

    Display display = getWindowManager().getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width * 9/10, (int)((height - (height * 1/12)) / 3), Gravity.CENTER_HORIZONTAL);
    imageResult.setLayoutParams(layoutParams);

    isEngineReady = false;

    ListView lv = (ListView) findViewById(R.id.restList);

    // Transfering data from MenuData object to Map<String, List<String>> because of the adapter.
    for (MenuData md : mdl) {
    	dishNames.put(md.getSection(), md.getItems());
    }
    MapAdapter<Map<String, List<String>>> adapter;
    adapter = new MapAdapter<Map<String, List<String>>>(dishNames) {

        @Override
        protected View getHeaderView(int position, View convertView, ViewGroup parent) {
            TextView v = convertView == null ? new TextView(parent.getContext()) : (TextView) convertView;
            String section = (String) getItem(position);
            if (section != null && !section.isEmpty()) {
            	section += ":";
            } else {
            	section = "Others:";
            }            
            v.setText(section);
            v.setTextSize(20);
            v.setTypeface(null, Typeface.BOLD);
            v.setPadding(0, 5, 0, 0);
            return v;
        }

        @Override
        protected View getListItemView(int position, View convertView, ViewGroup parent) {
        	TextView v = convertView == null ? new TextView(parent.getContext()) : (TextView) convertView;
            String item = (String) getItem(position);
            v.setText(item);
            v.setTextSize(17);
            v.setPadding(0, 5, 0, 0);
            v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					  // Access HTTP async, this is issue after Android 3.0.
      	    	  final String dishName = ((TextView)view).getText().toString();;    	  	
      	    	  AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
      	    		  private ProgressDialog pd;
      	    		  Bitmap bm;
      	    		  
      	    		  @Override
      	    		  protected void onPreExecute() {
      	    			  isLoadingImg = true;
      	    			  pd = new ProgressDialog(CaptureActivity.this);
      	    			  pd.setTitle("Retrieving " + dishName + " Image...");
      	    			  pd.setMessage("Please wait.");
      	    			  pd.setCancelable(false);
      	    			  pd.setIndeterminate(true);
      	    			  pd.show();
      	    		  }
      	    		  
      	    		  @Override
      	    		  protected Void doInBackground(Void... arg0) {
      	    			  try {
      	    				  bm = CaptureActivity.getImage(dishName);
      	    				  if (bm == null) {
      	    					  Log.d(TAG, "Got image default");
      	    					  tResult = dishName + "\nfrom: Default";
      	    					  bm = getBitmapFromAsset("default.jpg");
      	    				  }
      	    			  } 
      	    			  catch (IOException e) {
      	    				  Log.e(TAG, "Could not read image from Google", e);
      	    			  } 
      	    			  catch (JSONException e) {
      	    				  Log.e(TAG, "Could not read image from Google", e);
      	    			  }
      	    			  return null;
      	    		  }
      	    		  @Override
      	    		  protected void onPostExecute(Void result) {
      	    			  pd.dismiss();
      	    			  imageResult.setImageBitmap(bm);
      	    			  imageResult.setVisibility(View.VISIBLE);
      	    			  imageResult.invalidate(); // Fix image flickering.
      	    			  textResult.setText(tResult);
      	    			  textResult.setVisibility(View.VISIBLE);
      	    			  textResult.invalidate();
      	    			  isLoadingImg = false;
      	    		  }
      	    		};
      	    		task.execute((Void[]) null);
				}
        	});
            return v;
        }
    };

    lv.setAdapter(adapter);

    LayoutParams lp = (LayoutParams) lv.getLayoutParams();
    
    lp.height = (int) height - (((height - (height * 1/12)) * 4 / 10) + (height * 1/12));
    lv.setLayoutParams(lp);
  } // Finish onCreate().
  
  @Override
  public boolean onTouchEvent(MotionEvent e) {
	  cameraManager.requestAutoFocus(500L);
	  return true;	  
  }
  /**
   * Method for reading image from asset folder.
   */
  private Bitmap getBitmapFromAsset(String strName) throws IOException
  {
      AssetManager assetManager = getAssets();
      InputStream istr = assetManager.open(strName);
      Bitmap bitmap = BitmapFactory.decodeStream(istr);
      return bitmap;
  }

  private static Bitmap getBitmapFromURL(String src) {
	    try {
	        URL url = new URL(src);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        return myBitmap;
	    } catch (IOException e) {
	        Log.e(TAG, e.getMessage());
	        return null;
	    }
	}

  @Override
  protected void onResume() {
    super.onResume();   
    resetStatusView();
    
    String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
    int previousOcrEngineMode = ocrEngineMode;
    
    retrievePreferences();
    
    // Set up the camera preview surface.
    surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    surfaceHolder = surfaceView.getHolder();
    if (!hasSurface) {
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    // Comment out the following block to test non-OCR functions without an SD card
    
    // Do OCR engine initialization, if necessary
    boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) || 
        ocrEngineMode != previousOcrEngineMode;
    if (doNewInit) {      
      // Initialize the OCR engine
      File storageDirectory = getStorageDirectory();
      if (storageDirectory != null) {
        initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
      }
    } else {
      // We already have the engine initialized, so just start the camera.
      resumeOCR();
    }
  }
  
  /** 
   * Method to start or restart recognition after the OCR engine has been initialized,
   * or after the app regains focus. Sets state related settings and OCR engine parameters,
   * and requests camera initialization.
   */
  void resumeOCR() {
    Log.d(TAG, "resumeOCR()");
    
    // This method is called when Tesseract has already been successfully initialized, so set 
    // isEngineReady = true here.
    isEngineReady = true;
    
    isPaused = false;

    if (handler != null) {
      handler.resetState();
    }
    if (baseApi != null) {
      baseApi.setPageSegMode(pageSegmentationMode);
      baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
      baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
    }

    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    }
  }
  
  /** Called when the shutter button is pressed in continuous mode. */
  void onShutterButtonPressContinuous() {
    isPaused = true;
    handler.stop();  
    beepManager.playBeepSoundAndVibrate();
    if (lastResult != null) {
      handleOcrDecode(lastResult);
    } else {
      Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.TOP, 0, 0);
      toast.show();
      resumeContinuousDecoding();
    }
  }

  /** Called to resume recognition after translation in continuous mode. */
  @SuppressWarnings("unused")
  void resumeContinuousDecoding() {
    isPaused = false;
    resetStatusView();
    setStatusViewForContinuous();
    DecodeHandler.resetDecodeState();
    handler.resetState();
    if (shutterButton != null && DISPLAY_SHUTTER_BUTTON) {
      //shutterButton.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "surfaceCreated()");
    
    if (holder == null) {
      Log.e(TAG, "surfaceCreated gave us a null surface");
    }
    
    // Only initialize the camera if the OCR engine is ready to go.
    if (!hasSurface && isEngineReady) {
      Log.d(TAG, "surfaceCreated(): calling initCamera()...");
      initCamera(holder);
    }
    hasSurface = true;
  }
  
  /** Initializes the camera and starts the handler to begin previewing. */
  private void initCamera(SurfaceHolder surfaceHolder) {
    Log.d(TAG, "initCamera()");
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    try {

      // Open and initialize the camera
      cameraManager.openDriver(surfaceHolder);
      
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      handler = new CaptureActivityHandler(this, cameraManager, isContinuousModeActive);
      
    } catch (IOException ioe) {
      showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
    }   
  }
  
  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
    }
    
    // Stop using the camera, to avoid conflicting with other camera-based apps
    cameraManager.closeDriver();

    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  void stopHandler() {
    if (handler != null) {
      handler.stop();
    }
  }

  @Override
  protected void onDestroy() {
    if (baseApi != null) {
      baseApi.end();
    }
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {

      // First check if we're paused in continuous mode, and if so, just unpause.
      if (isPaused) {
        Log.d(TAG, "only resuming continuous recognition, not quitting...");
        resumeContinuousDecoding();
        return true;
      }

      // Exit the app if we're not viewing an OCR result.
      if (lastResult == null) {
        setResult(RESULT_CANCELED);
        Intent intent = new Intent(CaptureActivity.this, GoogleMapActivity.class);
        intent.putExtra("restaurants", restaurants); // Caching restaurants.
		startActivity(intent);
        finish();
        return true;
      } else {
        // Go back to previewing in regular OCR mode.
        resetStatusView();
        if (handler != null) {
          handler.sendEmptyMessage(R.id.restart_preview);
        }
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
      if (isContinuousModeActive) {
        onShutterButtonPressContinuous();
      } else {
        handler.hardwareShutterButtonClick();
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {      
      // Only perform autofocus if user is not holding down the button.
      if (event.getRepeatCount() == 0) {
        cameraManager.requestAutoFocus(500L);
      }
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //    MenuInflater inflater = getMenuInflater();
    //    inflater.inflate(R.menu.options_menu, menu);
    super.onCreateOptionsMenu(menu);
    menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
    //menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
    case SETTINGS_ID: {
      intent = new Intent().setClass(this, PreferencesActivity.class);
      intent.putParcelableArrayListExtra("dishNames", mdl); // Send to capture activity.
      intent.putExtra("restaurants", restaurants); // Caching restaurants.
      startActivity(intent);
      finish();
      break;
    }
    case ABOUT_ID: {
      intent = new Intent(this, HelpActivity.class);
      intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.ABOUT_PAGE);
      startActivity(intent);
      break;
    }
    }
    return super.onOptionsItemSelected(item);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  /** Sets the necessary language code values for the given OCR language. */
  private boolean setSourceLanguage(String languageCode) {
    sourceLanguageCodeOcr = languageCode;
    sourceLanguageCodeTranslation = LanguageCodeHelper.mapLanguageCode(languageCode);
    sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, languageCode);
    return true;
  }

  /** Sets the necessary language code values for the translation target language. */
  private boolean setTargetLanguage(String languageCode) {
    targetLanguageCodeTranslation = languageCode;
    targetLanguageReadable = LanguageCodeHelper.getTranslationLanguageName(this, languageCode);
    return true;
  }

  /** Finds the proper location on the SD card where we can save files. */
  private File getStorageDirectory() {
    //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));
    
    String state = null;
    try {
      state = Environment.getExternalStorageState();
    } catch (RuntimeException e) {
      Log.e(TAG, "Is the SD card visible?", e);
      showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
    }
    
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

      // We can read and write the media
      //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
      // For Android 2.2 and above
      
      try {
        return getExternalFilesDir(Environment.MEDIA_MOUNTED);
      } catch (NullPointerException e) {
        // We get an error here if the SD card is visible, but full
        Log.e(TAG, "External storage is unavailable");
        showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
      }
      
      //        } else {
      //          // For Android 2.1 and below, explicitly give the path as, for example,
      //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
      //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator + 
      //                  "Android" + File.separator + "data" + File.separator + getPackageName() + 
      //                  File.separator + "files" + File.separator);
      //        }
    
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	// We can only read the media
    	Log.e(TAG, "External storage is read-only");
      showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
    } else {
    	// Something else is wrong. It may be one of many other states, but all we need
      // to know is we can neither read nor write
    	Log.e(TAG, "External storage is unavailable");
    	showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
    }
    return null;
  }

  /**
   * Requests initialization of the OCR engine with the given parameters.
   * 
   * @param storageRoot Path to location of the tessdata directory to use
   * @param languageCode Three-letter ISO 639-3 language code for OCR 
   * @param languageName Name of the language for OCR, for example, "English"
   */
  private void initOcrEngine(File storageRoot, String languageCode, String languageName) {    
    isEngineReady = false;
    
    // Set up the dialog box for the thermometer-style download progress indicator
    if (dialog != null) {
      dialog.dismiss();
    }
    dialog = new ProgressDialog(this);
    
    // If we have a language that only runs using Cube, then set the ocrEngineMode to Cube
    if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
      for (String s : CUBE_REQUIRED_LANGUAGES) {
        if (s.equals(languageCode)) {
          ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
          prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
        }
      }
    }

    // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
    if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
      boolean cubeOk = false;
      for (String s : CUBE_SUPPORTED_LANGUAGES) {
        if (s.equals(languageCode)) {
          cubeOk = true;
        }
      }
      if (!cubeOk) {
        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
      }
    }
    
    // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
    indeterminateDialog = new ProgressDialog(this);
    indeterminateDialog.setTitle("Please wait");
    String ocrEngineModeName = getOcrEngineModeName();
    if (ocrEngineModeName.equals("Both")) {
      indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "...");
    } else {
      indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
    }
    indeterminateDialog.setCancelable(false);
    indeterminateDialog.show();
    
    if (handler != null) {
      handler.quitSynchronously();     
    }

    // Disable continuous mode if we're using Cube. This will prevent bad states for devices 
    // with low memory that crash when running OCR with Cube, and prevent unwanted delays.
    if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
      Log.d(TAG, "Disabling continuous preview");
      isContinuousModeActive = false;
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, false);
    }
    
    // Start AsyncTask to install language data and init OCR
    baseApi = new TessBaseAPI();
    new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
      .execute(storageRoot.toString());
  }
  
  /**
   * Displays information relating to the result of OCR, and requests a translation if necessary.
   * 
   * @param ocrResult Object representing successful OCR results
   * @return True if a non-null result was received for OCR
   */
  boolean handleOcrDecode(OcrResult ocrResult) {
    lastResult = ocrResult;
    
    // Test whether the result is null
    if (ocrResult.getText() == null || ocrResult.getText().equals("")) {
      Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.TOP, 0, 0);
      toast.show();
      return false;
    }
    
    // Turn off capture-related UI elements
    shutterButton.setVisibility(View.GONE);
    statusViewBottom.setVisibility(View.GONE);
    statusViewTop.setVisibility(View.GONE);
    cameraButtonView.setVisibility(View.GONE);
    viewfinderView.setVisibility(View.GONE);
    resultView.setVisibility(View.VISIBLE);

    ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
    lastBitmap = ocrResult.getBitmap();
    if (lastBitmap == null) {
      bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
          R.drawable.ic_launcher));
    } else {
      bitmapImageView.setImageBitmap(lastBitmap);
    }

    // Display the recognized text
    TextView sourceLanguageTextView = (TextView) findViewById(R.id.source_language_text_view);
    sourceLanguageTextView.setText(sourceLanguageReadable);
    TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
    ocrResultTextView.setText(ocrResult.getText());
    // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
    int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
    ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

    TextView translationLanguageLabelTextView = (TextView) findViewById(R.id.translation_language_label_text_view);
    TextView translationLanguageTextView = (TextView) findViewById(R.id.translation_language_text_view);
    TextView translationTextView = (TextView) findViewById(R.id.translation_text_view);
    if (isTranslationActive) {
      // Handle translation text fields
      translationLanguageLabelTextView.setVisibility(View.VISIBLE);
      translationLanguageTextView.setText(targetLanguageReadable);
      translationLanguageTextView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL), Typeface.NORMAL);
      translationLanguageTextView.setVisibility(View.VISIBLE);

      // Activate/re-activate the indeterminate progress indicator
      translationTextView.setVisibility(View.GONE);
      progressView.setVisibility(View.VISIBLE);
      setProgressBarVisibility(true);
      
      // Get the translation asynchronously
      new TranslateAsyncTask(this, sourceLanguageCodeTranslation, targetLanguageCodeTranslation, 
          ocrResult.getText()).execute();
    } else {
      translationLanguageLabelTextView.setVisibility(View.GONE);
      translationLanguageTextView.setVisibility(View.GONE);
      translationTextView.setVisibility(View.GONE);
      progressView.setVisibility(View.GONE);
      setProgressBarVisibility(false);
    }
    return true;
  }
  
  /**
   * Displays information relating to the results of a successful real-time OCR request.
   * 
   * @param ocrResult Object representing successful OCR results
   */
  void handleOcrContinuousDecode(OcrResult ocrResult) {
   
    lastResult = ocrResult;
    
    // Send an OcrResultText object to the ViewfinderView for text rendering
    viewfinderView.addResultText(new OcrResultText(ocrResult.getText(), 
                                                   ocrResult.getWordConfidences(),
                                                   ocrResult.getMeanConfidence(),
                                                   ocrResult.getBitmapDimensions(),
                                                   ocrResult.getRegionBoundingBoxes(),
                                                   ocrResult.getTextlineBoundingBoxes(),
                                                   ocrResult.getStripBoundingBoxes(),
                                                   ocrResult.getWordBoundingBoxes(),
                                                   ocrResult.getCharacterBoundingBoxes()));

    Integer meanConfidence = ocrResult.getMeanConfidence();
    
    if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
      // Display the recognized text on the screen
      //statusViewTop.setText(ocrResult.getText());
      String text = "SCANNING...\n" + ocrResult.getText();
      statusViewTop.setText(text);
      //int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
      int scaledSize = Math.max(22, 32 - text.length() / 4);
      statusViewTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
      statusViewTop.setTextColor(Color.BLACK);
      statusViewTop.setBackgroundResource(R.color.status_top_text_background);

      statusViewTop.getBackground().setAlpha(meanConfidence * (255 / 100));
      //Display display = getWindowManager().getDefaultDisplay();
      //int height = display.getHeight();
      //RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
      //rlp.setMargins(0, 0, 0, height / 4); // llp.setMargins(left, top, right, bottom);
      //statusViewTop.setLayoutParams(rlp);
      //statusViewTop.setGravity(Gravity.CENTER_HORIZONTAL);
      //statusViewTop.setPadding(0, 0, 0, height/4);

      String ocrTextResult = ocrResult.getText(); // Recognized text result.
      String normOcrTextResult = ocrTextResult.replace("\n", "").replace("\r", "").replaceAll("\\s",""); // Remove line breaks, whitespace, etc. 
      
      // Iterates through the menu titles and see if there is a match.
      String imageName = "";

      mainloop: for (String section : dishNames.keySet()) { // We have now to go through the sections and items.
	      for (String menuTitle : dishNames.get(section)) {
	    	  if (match(normOcrTextResult, menuTitle.replaceAll("\\s",""))) {
	    		  imageName = menuTitle;
	    		  break mainloop; // we have to break the whole loop
	    	  }
	      }
      }
      if (imageName != null && imageName.length() > 0 && !this.isLoadingImg && !this.prevDishName.equals(imageName)) { // MATCH.
    	  this.prevDishName = imageName;
    	  // Access HTTP async, this is issue after Android 3.0.
    	  final String dishName = imageName;    	  	
    	  AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
    		  private ProgressDialog pd;
    		  Bitmap bm;
    		  
    		  @Override
    		  protected void onPreExecute() {
    			  isLoadingImg = true;
    			  pd = new ProgressDialog(CaptureActivity.this);
    			  pd.setTitle("Retrieving Image...");
    			  pd.setMessage("Please wait.");
    			  pd.setCancelable(false);
    			  pd.setIndeterminate(true);
    			  pd.show();
    		  }
    		  
    		  @Override
    		  protected Void doInBackground(Void... arg0) {
    			  try {
    				  bm = CaptureActivity.getImage(dishName);
    				  if (bm == null) {
    					  Log.d(TAG, "Got image default");
    					  tResult = dishName + "\nfrom: Default";
    					  bm = getBitmapFromAsset("default.jpg");
    				  }
    			  } 
    			  catch (IOException e) {
    				  Log.e(TAG, "Could not read image from Google", e);
    			  } 
    			  catch (JSONException e) {
    				  Log.e(TAG, "Could not read image from Google", e);
    			  }
    			  return null;
    		  }
    		  @Override
    		  protected void onPostExecute(Void result) {
    			  pd.dismiss();
    			  imageResult.setImageBitmap(bm);
    			  imageResult.setVisibility(View.VISIBLE);
    			  imageResult.invalidate(); // Fix image flickering.
    			  textResult.setText(tResult);
    			  textResult.setVisibility(View.VISIBLE);
    			  textResult.invalidate();
    			  isLoadingImg = false;
    		  }
    		};
    		task.execute((Void[]) null);			
      }
      else {
    	  // imageResult.setVisibility(View.INVISIBLE); // NO MATCH. // Fix image flickering.
      }
    }

    if (CONTINUOUS_DISPLAY_METADATA) {
      // Display recognition-related metadata at the bottom of the screen
      long recognitionTimeRequired = ocrResult.getRecognitionTimeRequired();
      statusViewBottom.setTextSize(14);
      DecimalFormat df = new DecimalFormat("####0.00");
      String currentFocusMode = cameraManager.getCamera().getParameters().getFocusMode(); // Get current focus mode just for debugging purposes.
      statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - Mean confidence: " + 
          meanConfidence.toString() + " - Time required: " + recognitionTimeRequired + " ms - Found Similarity " + df.format(this.percentage)
          + " - Found Substring Match: " + this.substrMatch + " - Focus Mode: " + currentFocusMode); // Add more debug info.
    }
  }

  private static Bitmap getImage(final String dishName)
			throws MalformedURLException, IOException, JSONException {
		String imgUrl = null;
		URL url = new URL("http://api.yummly.com/v1/api/recipes?_app_id=6320d0cc&_app_key=0474538e4ffa6899dda75fb428a578f4&q=" + URLEncoder.encode(dishName));
		URLConnection connection = url.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		StringBuilder builder = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		JSONObject jsonObject = new JSONObject(builder.toString());
		JSONArray jsonArray = jsonObject.getJSONArray("matches");
		if (jsonArray != null && jsonArray.length() > 0) {  // **line 2**
			JSONObject object = jsonArray.getJSONObject(0);
			JSONArray jsonArray2 = object.getJSONArray("smallImageUrls");
			if (jsonArray2 != null && jsonArray2.length() > 0) {
				imgUrl = jsonArray2.getString(0);
				imgUrl = imgUrl.replaceFirst("\\.s\\.", "\\.l\\.");
				Log.d(TAG, "Got image from yummly " + imgUrl);
				tResult = dishName + "\nfrom: Yummly";
			}
		}
		if (imgUrl == null) {
			String accountKey = "zOulTOJsknHTtf3zDOaL30amNg3OjIHmbhK8cu26nsM";
			byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
			String accountKeyEnc = new String(accountKeyBytes);

			URL url2 = new URL("https://api.datamarket.azure.com/Bing/Search/v1/Composite?Sources=%27image%27&Query=%27" + URLEncoder.encode(dishName) + "%27&ImageFilters=%27Size%3AMedium%2BFace%3AOther%27&$format=json");
			URLConnection urlConnection = url2.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);			
			
			reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			builder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			JSONObject json2 = new JSONObject(builder.toString());
			if (json2 != null) {
				jsonObject = json2.getJSONObject("d");
				if (jsonObject != null) {
					JSONArray array = jsonObject.getJSONArray("results");
					if (array != null && array.length() > 0) {
						JSONObject jsonObject2 = array.getJSONObject(0);
						if (jsonObject2 != null) {
							JSONArray array2 = jsonObject2.getJSONArray("Image");
							if (array2 != null && array2.length() > 0) {
								JSONObject jsonObject3 = array2.getJSONObject(0);
								if (jsonObject3 != null) {
									imgUrl = jsonObject3.getString("MediaUrl");
									Log.d(TAG, "Got image from Bing " + imgUrl);
									tResult = dishName + "\nfrom: Bing";
								}
							}
						}
					}
				}
			}
		}		
		if (imgUrl == null) {
			try {
				url = new URL("https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=" + URLEncoder.encode(dishName) + "&imgsz=medium&rsz=1");
				connection = url.openConnection();
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				builder = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				JSONObject json = new JSONObject(builder.toString());
				JSONObject responseData = json.getJSONObject("responseData");
				JSONArray results = responseData.getJSONArray("results");
				JSONObject rs = (JSONObject) results.get(0);
				//String imgUrl = rs.getString("tbUrl");
				imgUrl = rs.getString("url");
				Log.d(TAG, "Got image from Google " + imgUrl);
				tResult = dishName + "\nfrom: Google";
			}
			catch (Exception e) {
				Log.e(TAG, "Failed from Google" + e.getMessage());
				return null;
			}
		}
		return getBitmapFromURL(imgUrl);
	}

	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 1; j <= str2.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1]
								+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
										: 1));

		return distance[str1.length()][str2.length()];
	}
  	
  	/**
  	 * Method to match the recognized text with the 
  	 * existing menu titles.
  	 */
	private boolean match(String str1, String str2) {
		String first = str1.toLowerCase();
		String second = str2.toLowerCase();
		String longer;
		String shorter;

		double m;
		if (first.length() >= second.length()) {
			m = first.length();
			longer = first;
			shorter = second;
		} else {
			m = second.length();
			longer = second;
			shorter = first;
		}
		double l;
		l = computeLevenshteinDistance(first, second);
		double p;
		p = (1 - (l / m)) * 100;
		this.percentage = p;
		// Log.d(TAG, "Similarity: " + p);
		
		boolean containsTemplate = (longer.indexOf(shorter) >= 0);
		this.substrMatch = containsTemplate;
		// Log.d(TAG, "containsTemplate: " + containsTemplate);

		return (p >= 60 || containsTemplate) ? true : false;
	}

  /**
   * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
   * 
   * @param obj Metadata for the failed OCR request.
   */
  void handleOcrContinuousDecode(OcrResultFailure obj) {
    lastResult = null;
    viewfinderView.removeResultText();
    
    // Reset the text in the recognized text box.
    statusViewTop.setText("");

    if (CONTINUOUS_DISPLAY_METADATA) {
      // Color text delimited by '-' as red.
      statusViewBottom.setTextSize(14);
      CharSequence cs = setSpanBetweenTokens("OCR: " + sourceLanguageReadable + " - OCR failed - Time required: " 
          + obj.getTimeRequired() + " ms", "-", new ForegroundColorSpan(0xFFFF0000));
      statusViewBottom.setText(cs);
    }
  }
  
  /**
   * Given either a Spannable String or a regular String and a token, apply
   * the given CharacterStyle to the span between the tokens.
   * 
   * NOTE: This method was adapted from:
   *  http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
   * 
   * <p>
   * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##", new
   * ForegroundColorSpan(0xFFFF0000));} will return a CharSequence {@code
   * "Hello world!"} with {@code world} in red.
   * 
   */
  private CharSequence setSpanBetweenTokens(CharSequence text, String token,
      CharacterStyle... cs) {
    // Start and end refer to the points where the span will apply
    int tokenLen = token.length();
    int start = text.toString().indexOf(token) + tokenLen;
    int end = text.toString().indexOf(token, start);

    if (start > -1 && end > -1) {
      // Copy the spannable string to a mutable spannable string
      SpannableStringBuilder ssb = new SpannableStringBuilder(text);
      for (CharacterStyle c : cs)
        ssb.setSpan(c, start, end, 0);
      text = ssb;
    }
    return text;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    if (v.equals(ocrResultView)) {
      menu.add(Menu.NONE, OPTIONS_COPY_RECOGNIZED_TEXT_ID, Menu.NONE, "Copy recognized text");
      menu.add(Menu.NONE, OPTIONS_SHARE_RECOGNIZED_TEXT_ID, Menu.NONE, "Share recognized text");
    } else if (v.equals(translationView)){
      menu.add(Menu.NONE, OPTIONS_COPY_TRANSLATED_TEXT_ID, Menu.NONE, "Copy translated text");
      menu.add(Menu.NONE, OPTIONS_SHARE_TRANSLATED_TEXT_ID, Menu.NONE, "Share translated text");
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    switch (item.getItemId()) {

    case OPTIONS_COPY_RECOGNIZED_TEXT_ID:
        clipboardManager.setText(ocrResultView.getText());
      if (clipboardManager.hasText()) {
        Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
      }
      return true;
    case OPTIONS_SHARE_RECOGNIZED_TEXT_ID:
    	Intent shareRecognizedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
    	shareRecognizedTextIntent.setType("text/plain");
    	shareRecognizedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, ocrResultView.getText());
    	startActivity(Intent.createChooser(shareRecognizedTextIntent, "Share via"));
    	return true;
    case OPTIONS_COPY_TRANSLATED_TEXT_ID:
        clipboardManager.setText(translationView.getText());
      if (clipboardManager.hasText()) {
        Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
      }
      return true;
    case OPTIONS_SHARE_TRANSLATED_TEXT_ID:
    	Intent shareTranslatedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
    	shareTranslatedTextIntent.setType("text/plain");
    	shareTranslatedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, translationView.getText());
    	startActivity(Intent.createChooser(shareTranslatedTextIntent, "Share via"));
    	return true;
    default:
      return super.onContextItemSelected(item);
    }
  }

  /**
   * Resets view elements.
   */
  private void resetStatusView() {
    resultView.setVisibility(View.GONE);
    if (CONTINUOUS_DISPLAY_METADATA) {
      statusViewBottom.setText("");
      statusViewBottom.setTextSize(14);
      statusViewBottom.setTextColor(getResources().getColor(R.color.status_text));
      //statusViewBottom.setVisibility(View.VISIBLE);
      statusViewBottom.setVisibility(View.GONE);
    }
    if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
      statusViewTop.setText("");
      statusViewTop.setTextSize(14);
      statusViewTop.setVisibility(View.VISIBLE);
    }
    viewfinderView.setVisibility(View.VISIBLE);
    cameraButtonView.setVisibility(View.VISIBLE);
    if (DISPLAY_SHUTTER_BUTTON) {
      //shutterButton.setVisibility(View.VISIBLE);
    }
    lastResult = null;
    viewfinderView.removeResultText();
  }
  
  /** Displays a pop-up message showing the name of the current OCR source language. */
  void showLanguageName() {   
    Toast toast = Toast.makeText(this, "OCR: " + sourceLanguageReadable, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.TOP, 0, 0);
    toast.show();
  }
  
  /**
   * Displays an initial message to the user while waiting for the first OCR request to be
   * completed after starting realtime OCR.
   */
  void setStatusViewForContinuous() {
    viewfinderView.removeResultText();
    if (CONTINUOUS_DISPLAY_METADATA) {
      statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - waiting for OCR...");
    }
  }
  
  @SuppressWarnings("unused")
  void setButtonVisibility(boolean visible) {
    if (shutterButton != null && visible == true && DISPLAY_SHUTTER_BUTTON) {
      //shutterButton.setVisibility(View.VISIBLE);
    } else if (shutterButton != null) {
      shutterButton.setVisibility(View.GONE);
    }
  }
  
  /**
   * Enables/disables the shutter button to prevent double-clicks on the button.
   * 
   * @param clickable True if the button should accept a click
   */
  void setShutterButtonClickable(boolean clickable) {
    shutterButton.setClickable(clickable);
  }

  /** Request the viewfinder to be invalidated. */
  void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }
  
  @Override
  public void onShutterButtonClick(ShutterButton b) {
    if (isContinuousModeActive) {
      onShutterButtonPressContinuous();
    } else {
      if (handler != null) {
        handler.shutterButtonClick();
      }
    }
  }

  @Override
  public void onShutterButtonFocus(ShutterButton b, boolean pressed) {
    requestDelayedAutoFocus();
  }
  
  /**
   * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user 
   * just wants to click the shutter button without focusing. Quick button press/release will 
   * trigger onShutterButtonClick() before the focus kicks in.
   */
  private void requestDelayedAutoFocus() {
    // Wait 350 ms before focusing to avoid interfering with quick button presses when
    // the user just wants to take a picture without focusing.
    cameraManager.requestAutoFocus(350L);
  }
  
  static boolean getFirstLaunch() {
    return isFirstLaunch;
  }
  
  /**
   * We want the help screen to be shown automatically the first time a new version of the app is
   * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
   * it to a value stored as a preference.
   */
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
        
        // Record the last version for which we last displayed the What's New (Help) page
        prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
        Intent intent = new Intent(this, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        
        // Show the default page on a clean install, and the what's new page on an upgrade.
        String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
        //startActivity(intent); // Don't show this anymore. Otherwise exception.
        return true;
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
    }
    return false;
  }
  
  /**
   * Returns a string that represents which OCR engine(s) are currently set to be run.
   * 
   * @return OCR engine mode
   */
  String getOcrEngineModeName() {
    String ocrEngineModeName = "";
    String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
    if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_ONLY) {
      ocrEngineModeName = ocrEngineModes[0];
    } else if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY) {
      ocrEngineModeName = ocrEngineModes[1];
    } else if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
      ocrEngineModeName = ocrEngineModes[2];
    }
    return ocrEngineModeName;
  }
  
  /**
   * Gets values from shared preferences and sets the corresponding data members in this activity.
   */
  private void retrievePreferences() {
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
      
      // Retrieve from preferences, and set in this Activity, the language preferences
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
      setSourceLanguage(prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE));
      setTargetLanguage(prefs.getString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_TARGET_LANGUAGE_CODE));
      isTranslationActive = prefs.getBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, false);
      
      // Retrieve from preferences, and set in this Activity, the capture mode preference
      if (prefs.getBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, CaptureActivity.DEFAULT_TOGGLE_CONTINUOUS)) {
        isContinuousModeActive = true;
      } else {
        isContinuousModeActive = false;
      }

      // Retrieve from preferences, and set in this Activity, the page segmentation mode preference
      String[] pageSegmentationModes = getResources().getStringArray(R.array.pagesegmentationmodes);
      String pageSegmentationModeName = prefs.getString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, pageSegmentationModes[0]);
      if (pageSegmentationModeName.equals(pageSegmentationModes[0])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[1])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[2])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[3])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[4])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[5])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[6])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
      } else if (pageSegmentationModeName.equals(pageSegmentationModes[7])) {
        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT;
      }
      
      // Retrieve from preferences, and set in this Activity, the OCR engine mode
      String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
      String ocrEngineModeName = prefs.getString(PreferencesActivity.KEY_OCR_ENGINE_MODE, ocrEngineModes[0]);
      if (ocrEngineModeName.equals(ocrEngineModes[0])) {
        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
      } else if (ocrEngineModeName.equals(ocrEngineModes[1])) {
        ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
      } else if (ocrEngineModeName.equals(ocrEngineModes[2])) {
        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
      }
      
      // Retrieve from preferences, and set in this Activity, the character blacklist and whitelist
      characterBlacklist = OcrCharacterHelper.getBlacklist(prefs, sourceLanguageCodeOcr);
      characterWhitelist = OcrCharacterHelper.getWhitelist(prefs, sourceLanguageCodeOcr);
      
      prefs.registerOnSharedPreferenceChangeListener(listener);
      
      beepManager.updatePrefs();
  }
  
  /**
   * Sets default values for preferences. To be called the first time this app is run.
   */
  private void setDefaultPreferences() {
    prefs = PreferenceManager.getDefaultSharedPreferences(this);

    // Continuous preview
    prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, CaptureActivity.DEFAULT_TOGGLE_CONTINUOUS).commit();

    // Recognition language
    prefs.edit().putString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE).commit();

    // Translation
    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, CaptureActivity.DEFAULT_TOGGLE_TRANSLATION).commit();

    // Translation target language
    prefs.edit().putString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_TARGET_LANGUAGE_CODE).commit();

    // Translator
    prefs.edit().putString(PreferencesActivity.KEY_TRANSLATOR, CaptureActivity.DEFAULT_TRANSLATOR).commit();

    // OCR Engine
    prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, CaptureActivity.DEFAULT_OCR_ENGINE_MODE).commit();

    // Autofocus
    prefs.edit().putBoolean(PreferencesActivity.KEY_AUTO_FOCUS, CaptureActivity.DEFAULT_TOGGLE_AUTO_FOCUS).commit();
    
    // Beep
    prefs.edit().putBoolean(PreferencesActivity.KEY_PLAY_BEEP, CaptureActivity.DEFAULT_TOGGLE_BEEP).commit();

    // Character blacklist
    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_BLACKLIST, 
        OcrCharacterHelper.getDefaultBlacklist(CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

    // Character whitelist
    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_WHITELIST, 
        OcrCharacterHelper.getDefaultWhitelist(CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

    // Page segmentation mode
    prefs.edit().putString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, CaptureActivity.DEFAULT_PAGE_SEGMENTATION_MODE).commit();

    // Reversed camera image
    prefs.edit().putBoolean(PreferencesActivity.KEY_REVERSE_IMAGE, CaptureActivity.DEFAULT_TOGGLE_REVERSED_IMAGE).commit();
    
    // Light
    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_LIGHT, CaptureActivity.DEFAULT_TOGGLE_LIGHT).commit();
  }
  
  void displayProgressDialog() {
    // Set up the indeterminate progress dialog box
    indeterminateDialog = new ProgressDialog(this);
    indeterminateDialog.setTitle("Please wait");        
    String ocrEngineModeName = getOcrEngineModeName();
    if (ocrEngineModeName.equals("Both")) {
      indeterminateDialog.setMessage("Performing OCR using Cube and Tesseract...");
    } else {
      indeterminateDialog.setMessage("Performing OCR using " + ocrEngineModeName + "...");
    }
    indeterminateDialog.setCancelable(false);
    indeterminateDialog.show();
  }
  
  ProgressDialog getProgressDialog() {
    return indeterminateDialog;
  }
  
  /**
   * Displays an error message dialog box to the user on the UI thread.
   * 
   * @param title The title for the dialog box
   * @param message The error message to be displayed
   */
  void showErrorMessage(String title, String message) {
	  new AlertDialog.Builder(this)
	    .setTitle(title)
	    .setMessage(message)
	    .setOnCancelListener(new FinishListener(this))
	    .setPositiveButton( "Done", new FinishListener(this))
	    .show();
  }
}
