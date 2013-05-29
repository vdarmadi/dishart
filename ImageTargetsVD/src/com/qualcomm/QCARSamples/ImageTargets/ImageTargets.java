/*==============================================================================
            Copyright (c) 2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary
            
@file 
    ImageTargets.java

@brief
    Sample for ImageTargets

==============================================================================*/


package com.qualcomm.QCARSamples.ImageTargets;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.qualcomm.QCAR.QCAR;


/** The main activity for the ImageTargets sample. */
public class ImageTargets extends Activity
{
    // Application status constants:
    private static final int APPSTATUS_UNINITED         = -1;
    private static final int APPSTATUS_INIT_APP         = 0;
    private static final int APPSTATUS_INIT_QCAR        = 1;
    private static final int APPSTATUS_INIT_TRACKER     = 2;    
    private static final int APPSTATUS_INIT_APP_AR      = 3;
    private static final int APPSTATUS_LOAD_TRACKER     = 4;
    private static final int APPSTATUS_INITED           = 5;
    private static final int APPSTATUS_CAMERA_STOPPED   = 6;
    private static final int APPSTATUS_CAMERA_RUNNING   = 7;
    
    // Name of the native dynamic libraries to load:
    private static final String NATIVE_LIB_SAMPLE = "ImageTargets";    
    private static final String NATIVE_LIB_QCAR = "QCAR"; 

    // Our OpenGL view:
    private QCARSampleGLView mGlView;
    
    // The view to display the sample splash screen:
    private ImageView mSplashScreenView;
    
    // The handler and runnable for the splash screen time out task.
    private Handler mSplashScreenHandler;
    private Runnable mSplashScreenRunnable;    
    
    // The minimum time the splash screen should be visible:
    private static final long MIN_SPLASH_SCREEN_TIME = 2000;    
    
    // The time when the splash screen has become visible:
    long mSplashScreenStartTime = 0;
    
    // Our renderer:
    private ImageTargetsRenderer mRenderer;
    
    // Display size of the device
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    
    // The current application status
    private int mAppStatus = APPSTATUS_UNINITED;
    
    // The async tasks to initialize the QCAR SDK 
    private InitQCARTask mInitQCARTask;
    private LoadTrackerTask mLoadTrackerTask;

    // An object used for synchronizing QCAR initialization, dataset loading and
    // the Android onDestroy() life cycle event. If the application is destroyed
    // while a data set is still being loaded, then we wait for the loading
    // operation to finish before shutting down QCAR.
    private Object mShutdownLock = new Object();   
    
    // QCAR initialization flags
    private int mQCARFlags = 0;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    private int mSplashScreenImageResource = 0;
    
    // The menu item for swapping data sets:
    MenuItem mDataSetMenuItem = null;
    boolean mIsStonesAndChipsDataSetActive  = false;
    
	CustomView customView;

	private Map<String, String> restMenus;

    /** Static initializer block to load native libraries on start-up. */
    static
    {
        loadLibrary(NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_SAMPLE);
    }
    
    
    /** An async task to initialize QCAR asynchronously. */
    private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
    {   
        // Initialize with invalid value
        private int mProgressValue = -1;
        
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mShutdownLock)
            {
                QCAR.setInitParameters(ImageTargets.this, mQCARFlags);
                
                do
                {
                    // QCAR.init() blocks until an initialization step is complete,
                    // then it proceeds to the next step and reports progress in
                    // percents (0 ... 100%)
                    // If QCAR.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = QCAR.init();
                    
                    // Publish the progress value:
                    publishProgress(mProgressValue);
                    
                    // We check whether the task has been canceled in the meantime
                    // (by calling AsyncTask.cancel(true))
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that started is.
                } while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);
                
                return (mProgressValue > 0);                
            }
        }

        
        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }

        
        protected void onPostExecute(Boolean result)
        {
            // Done initializing QCAR, proceed to next application
            // initialization status:
            if (result)
            {
                DebugLog.LOGD("InitQCARTask::onPostExecute: QCAR initialization" +
                                                            " successful");

                updateApplicationStatus(APPSTATUS_INIT_TRACKER);
            }
            else
            {
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder(ImageTargets.this).create();
                dialogError.setButton(
                    "Close",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Exiting application
                            System.exit(1);
                        }
                    }
                ); 
                
                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED)
                {
                    logMessage = "Failed to initialize QCAR because this " +
                        "device is not supported.";
                }
                else
                {
                    logMessage = "Failed to initialize QCAR.";
                }
                
                // Log error:
                DebugLog.LOGE("InitQCARTask::onPostExecute: " + logMessage +
                                " Exiting.");
                
                // Show dialog box with error message:
                dialogError.setMessage(logMessage);  
                dialogError.show();
            }
        }
    }
    
    
    /** An async task to load the tracker data asynchronously. */
    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap:
            synchronized (mShutdownLock)
            {
                // Load the tracker data set:
            	downloadAppFiles("http://www.adri-silvy.com/android/ar/Menu.xml", "Menu.xml"); // Default name is "Menu" for initialization.
            	downloadAppFiles("http://www.adri-silvy.com/android/ar/Menu.dat", "Menu.dat"); // Default name is "Menu" for initialization.
                return (loadTrackerData() > 0);                
            }
        }
        
        protected void onPostExecute(Boolean result)
        {
            DebugLog.LOGD("LoadTrackerTask::onPostExecute: execution " +
                        (result ? "successful" : "failed"));
            
            if (result)
            {
                // The stones and chips data set is now active:
                mIsStonesAndChipsDataSetActive = true;
                
                // Done loading the tracker, update application status: 
                updateApplicationStatus(APPSTATUS_INITED);
            }
            else
            {
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder(ImageTargets.this).create();
                dialogError.setButton(
                    "Close",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Exiting application
                            System.exit(1);
                        }
                    }
                ); 
                
                // Show dialog box with error message:
                dialogError.setMessage("Failed to load tracker data.");  
                dialogError.show();
            }
        }
    }

    private void storeScreenDimensions()
    {
        // Query display dimensions
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    
    /** Called when the activity first starts or the user navigates back
     * to an activity. */
    protected void onCreate(Bundle savedInstanceState)
    {
        DebugLog.LOGD("ImageTargets::onCreate");
        super.onCreate(savedInstanceState);
        
        // Set the splash screen image to display during initialization:
        mSplashScreenImageResource = R.drawable.splash_screen_image_targets;
        
        // Load any sample specific textures:  
        mTextures = new Vector<Texture>();
        loadTextures();
        
        // Query the QCAR initialization flags:
        mQCARFlags = getInitializationFlags();
        
        // Update the application status to start initializing application
        updateApplicationStatus(APPSTATUS_INIT_APP);

        initRestMenu();
    }

	private void initRestMenu() {
		try {
			restMenus = new HashMap<String, String>();
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(new URL("http://adri-silvy.com/android/restMenus.txt").openStream()));
			String line = "";
			while ((line = in.readLine()) != null) {
				String parts[] = line.split("\t");
				restMenus.put(parts[0], parts[1]);
			}
			in.close();
		} catch (IOException e1) {
			DebugLog.LOGE(e1.getMessage());
		}
	}

    
    /** We want to load specific textures from the APK, which we will later
    use for rendering. */
    private void loadTextures()
    {
    	downloadTextures("Menu"); // Default "Menu" for initialization.
    }
    
    
    /** Configure QCAR with the desired version of OpenGL ES. */
    private int getInitializationFlags()
    {
        int flags = 0;
        
        // Query the native code:
        if (getOpenGlEsVersionNative() == 1)
        {
            flags = QCAR.GL_11;
        }
        else
        {
            flags = QCAR.GL_20;
        }
        
        return flags;
    }    
    
    
    /** native method for querying the OpenGL ES version.
     * Returns 1 for OpenGl ES 1.1, returns 2 for OpenGl ES 2.0. */
    public native int getOpenGlEsVersionNative();
    
    /** Native tracker initialization and deinitialization. */
    public native int initTracker();
    public native void deinitTracker();

    /** Native functions to load and destroy tracking data. */
    public native int loadTrackerData();
    public native void destroyTrackerData();    
    
    /** Native sample initialization. */
    public native void onQCARInitializedNative();    
        
    /** Native methods for starting and stoping the camera. */ 
    private native void startCamera();
    private native void stopCamera();
    
    /** Native method for setting / updating the projection matrix for AR content rendering */
    private native void setProjectionMatrix();


   /** Called when the activity will start interacting with the user.*/
    protected void onResume()
    {
        DebugLog.LOGD("ImageTargets::onResume");
        super.onResume();
        
        // QCAR-specific resume operation
        QCAR.onResume();
        
        // We may start the camera only if the QCAR SDK has already been 
        // initialized
        if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
            
            // Reactivate flash if it was active before pausing the app
            if (mFlash)
            {
                boolean result = activateFlash(mFlash);
                DebugLog.LOGI("Turning flash "+(mFlash?"ON":"OFF")+" "+(result?"WORKED":"FAILED")+"!!");
            }
        }
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }        
    }
    
    
    public void onConfigurationChanged(Configuration config)
    {
        DebugLog.LOGD("ImageTargets::onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        storeScreenDimensions();
        
        // Set projection matrix:
        if (QCAR.isInitialized())
            setProjectionMatrix();
    }
    

    /** Called when the system is about to start resuming a previous activity.*/
    protected void onPause()
    {
        DebugLog.LOGD("ImageTargets::onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
        }
        
        // QCAR-specific pause operation
        QCAR.onPause();
    }
    
    
    /** Native function to deinitialize the application.*/
    private native void deinitApplicationNative();

    
    /** The final call you receive before your activity is destroyed.*/
    protected void onDestroy()
    {
        DebugLog.LOGD("ImageTargets::onDestroy");
        super.onDestroy();
        
        // Dismiss the splash screen time out handler:
        if (mSplashScreenHandler != null)
        {
            mSplashScreenHandler.removeCallbacks(mSplashScreenRunnable);
            mSplashScreenRunnable = null;
            mSplashScreenHandler = null;
        }        
        
        // Cancel potentially running tasks
        if (mInitQCARTask != null &&
            mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            mInitQCARTask.cancel(true);
            mInitQCARTask = null;
        }
        
        if (mLoadTrackerTask != null &&
            mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }
        
        // Ensure that all asynchronous operations to initialize QCAR and loading
        // the tracker datasets do not overlap:
        synchronized (mShutdownLock) {
            
            // Do application deinitialization in native code
            deinitApplicationNative();
            
            // Unload texture
            mTextures.clear();
            mTextures = null;
            
            // Destroy the tracking data set:
            destroyTrackerData();
            
            // Deinit the tracker:
            deinitTracker();
            
            // Deinitialize QCAR SDK
            QCAR.deinit();   
        }
        
        System.gc();
    }

    
    /** NOTE: this method is synchronized because of a potential concurrent
     * access by ImageTargets::onResume() and InitQCARTask::onPostExecute(). */
    private synchronized void updateApplicationStatus(int appStatus)
    {
        // Exit if there is no change in status
        if (mAppStatus == appStatus)
            return;

        // Store new status value      
        mAppStatus = appStatus;

        // Execute application state-specific actions
        switch (mAppStatus)
        {
            case APPSTATUS_INIT_APP:
                // Initialize application elements that do not rely on QCAR
                // initialization  
                initApplication();
                
                // Proceed to next application initialization status
                updateApplicationStatus(APPSTATUS_INIT_QCAR);
                break;

            case APPSTATUS_INIT_QCAR:
                // Initialize QCAR SDK asynchronously to avoid blocking the
                // main (UI) thread.
                // This task instance must be created and invoked on the UI
                // thread and it can be executed only once!
                try
                {
                    mInitQCARTask = new InitQCARTask();
                    mInitQCARTask.execute();
                }
                catch (Exception e)
                {
                    DebugLog.LOGE("Initializing QCAR SDK failed");
                }
                break;
                
            case APPSTATUS_INIT_TRACKER:
                // Initialize the ImageTracker
                if (initTracker() > 0)
                {
                    // Proceed to next application initialization status
                    updateApplicationStatus(APPSTATUS_INIT_APP_AR);     
                }
                break;
                
            case APPSTATUS_INIT_APP_AR:
                // Initialize Augmented Reality-specific application elements
                // that may rely on the fact that the QCAR SDK has been
                // already initialized
                initApplicationAR();
                
                // Proceed to next application initialization status
                updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
                break;
                
            case APPSTATUS_LOAD_TRACKER:
                // Load the tracking data set
                //
                // This task instance must be created and invoked on the UI
                // thread and it can be executed only once!
                try
                {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    DebugLog.LOGE("Loading tracking data set failed");
                }
                break;
                
            case APPSTATUS_INITED:
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector.
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                // Native post initialization:
                onQCARInitializedNative();
                
                // The elapsed time since the splash screen was visible:
                long splashScreenTime = System.currentTimeMillis() - 
                                            mSplashScreenStartTime;
                long newSplashScreenTime = 0;
                if (splashScreenTime < MIN_SPLASH_SCREEN_TIME)
                {
                    newSplashScreenTime = MIN_SPLASH_SCREEN_TIME -
                                            splashScreenTime;   
                }
                
                // Request a callback function after a given timeout to dismiss
                // the splash screen:
                mSplashScreenHandler = new Handler();
                mSplashScreenRunnable =
                    new Runnable() {
                        public void run()
                        {
                            // Hide the splash screen
                            mSplashScreenView.setVisibility(View.INVISIBLE);
                            
                            // Activate the renderer
                            mRenderer.mIsActive = true;
    
                            // Now add the GL surface view. It is important
                            // that the OpenGL ES surface view gets added
                            // BEFORE the camera is started and video
                            // background is configured.
                            addContentView(mGlView, new LayoutParams(
                                            LayoutParams.FILL_PARENT,
                                            LayoutParams.FILL_PARENT));
                            
                            // Start the camera:
                            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
                        }
                };

                mSplashScreenHandler.postDelayed(mSplashScreenRunnable,
                                                    newSplashScreenTime);
                break;
                
            case APPSTATUS_CAMERA_STOPPED:
                // Call the native function to stop the camera
                stopCamera();
                break;
                
            case APPSTATUS_CAMERA_RUNNING:
                // Call the native function to start the camera
                startCamera();
                setProjectionMatrix();
                customView = new CustomView(this);
                addContentView(customView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
              	customView.setVisibility(View.VISIBLE);
                break;
                
            default:
                throw new RuntimeException("Invalid application state");
        }
    }
    
    
    /** Tells native code whether we are in portait or landscape mode */
    private native void setActivityPortraitMode(boolean isPortrait);
    
    
    /** Initialize application GUI elements that are not related to AR. */
    private void initApplication()
    {
        // Set the screen orientation
        //
        // NOTE: It is recommended to set this because of the following reasons:
        //
        //    1.) Before Android 2.2 there is no reliable way to query the
        //        absolute screen orientation from an activity, therefore using 
        //        an undefined orientation is not recommended. Screen 
        //        orientation matching orientation sensor measurements is also
        //        not recommended as every screen orientation change triggers
        //        deinitialization / (re)initialization steps in internal QCAR 
        //        SDK components resulting in unnecessary overhead during 
        //        application run-time.
        //
        //    2.) Android camera drivers seem to always deliver landscape images
        //        thus QCAR SDK components (e.g. camera capturing) need to know 
        //        when we are in portrait mode. Before Android 2.2 there is no 
        //        standard, device-independent way to let the camera driver know 
        //        that we are in portrait mode as each device seems to require a
        //        different combination of settings to rotate camera preview 
        //        frames images to match portrait mode views. Because of this,
        //        we suggest that the activity using the QCAR SDK be locked
        //        to landscape mode if you plan to support Android 2.1 devices
        //        as well. Froyo is fine with both orientations.
        int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        
        // Apply screen orientation
        setRequestedOrientation(screenOrientation);
        
        // Pass on screen orientation info to native code
        setActivityPortraitMode(screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        storeScreenDimensions();
        
        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright.
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
              
        // Create and add the splash screen view
        mSplashScreenView = new ImageView(this);
        mSplashScreenView.setImageResource(mSplashScreenImageResource);
        addContentView(mSplashScreenView, new LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        mSplashScreenStartTime = System.currentTimeMillis();

    }
    
    
    /** Native function to initialize the application. */
    private native void initApplicationNative(int width, int height);


    /** Initializes AR application components. */
    private void initApplicationAR()
    {        
        // Do application initialization in native code (e.g. registering
        // callbacks, etc.)
        initApplicationNative(mScreenWidth, mScreenHeight);

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = QCAR.requiresAlpha();
        
        mGlView = new QCARSampleGLView(this);
        mGlView.init(mQCARFlags, translucent, depthSize, stencilSize);
        
        mRenderer = new ImageTargetsRenderer();
        mGlView.setRenderer(mRenderer);
 
    }

    /** Invoked the first time when the options menu is displayed to give
     *  the Activity a chance to populate its Menu with menu items. */
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
                        
        //mDataSetMenuItem = menu.add("Switch to Tarmac dataset");
        menu.add("Toggle flash");
        menu.add("Trigger autofocus");
        
        SubMenu focusModes = menu.addSubMenu("Focus Modes");
        focusModes.add("Normal").setCheckable(true);
        focusModes.add("Continuous Autofocus").setCheckable(true);
        focusModes.add("Infinity").setCheckable(true);
        focusModes.add("Macro Mode").setCheckable(true);
        
        SubMenu restaurant = menu.addSubMenu("Choose Restaurant"); // Parent menu.
        if (restMenus != null) {
        	for (String m : restMenus.keySet()) {
                restaurant.add(m); // Child menu.        		
        	}
        }
        return true;
    }
    
    
    /** Tells native code to switch dataset as soon as possible*/
    private native void switchDatasetAsap(String dataSetName); // Passing String to JNI.
    
    
    /** Invoked when the user selects an item from the Menu */
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	String dataSetName = "Menu"; // Default key is "Menu".
        //if(item == mDataSetMenuItem)
        //{
           //downloadAppFiles("http://www.adri-silvy.com/android/ar/Tarmac.xml", "Tarmac.xml");
           //downloadAppFiles("http://www.adri-silvy.com/android/ar/Tarmac.dat", "Tarmac.dat");
           //switchDatasetAsap("Tarmac");
           //mIsStonesAndChipsDataSetActive = !mIsStonesAndChipsDataSetActive;
           //if (mIsStonesAndChipsDataSetActive)
           //{
           //    item.setTitle("Switch to Tarmac dataset");
           //}
           //else
           //{
           //    item.setTitle("Switch to StonesAndChips dataset");
           //}
            
        //}
        if(item.getTitle().equals("Toggle flash"))
        {
            mFlash = !mFlash;
            boolean result = activateFlash(mFlash);
            DebugLog.LOGI("Turning flash "+(mFlash?"ON":"OFF")+" "+(result?"WORKED":"FAILED")+"!!");
        }
        else if(item.getTitle().equals("Trigger autofocus"))
        {
            boolean result = autofocus();
            DebugLog.LOGI("Autofocus requested"+(result?" successfully.":".  Not supported in current mode or on this device."));
        }
        else if (restMenus != null) 
        {
        	for (String m : restMenus.keySet()) {
        		if(item.getTitle().equals(m))
                {
                	dataSetName = restMenus.get(m);
                	// updateTrackerData(dataSetName);
                	new LoadRestaurantAsync(dataSetName).execute();
                }
        	}
        }
        else 
        {
            int arg = -1;
            if(item.getTitle().equals("Normal"))
                arg = 0;
            if(item.getTitle().equals("Continuous Autofocus"))
                arg = 1;
            if(item.getTitle().equals("Infinity"))
                arg = 2;
            if(item.getTitle().equals("Macro Mode"))
                arg = 3;
            
            if(arg != -1)
            {
                boolean result = setFocusMode(arg);
                if (result)
                {
                    item.setChecked(true);
                    if(checked != null && item != checked)
                        checked.setChecked(false);
                    checked = item;
                }
                
                DebugLog.LOGI("Requested Focus mode "+item.getTitle()+(result?" successfully.":".  Not supported on this device."));
            }
        }
        
        return true;
    }

    private ProgressDialog mProgressDialog;

    class LoadRestaurantAsync extends AsyncTask<String, String, String> {
		private String dataSetName;
		
		public LoadRestaurantAsync(String dataSetName) {
			super();
			this.dataSetName = dataSetName;
		}

		public void setDataSetName(String dataSetName) {
			this.dataSetName = dataSetName;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(ImageTargets.this);
			mProgressDialog.setMessage("Loading restaurant files..");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected String doInBackground(String... aurl) {
			if (this.dataSetName != null && this.dataSetName.trim().length() > 0) {
				updateTrackerData(this.dataSetName);
			}
			else {
				DebugLog.LOGE("Load restaurant failed, dataSetName " + this.dataSetName);
			}
			return null;
		}

		protected void onProgressUpdate(String... progress) {
	        DebugLog.LOGD("ANDRO_ASYNC " + progress[0]);
			mProgressDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(String unused) {
			mProgressDialog.dismiss();
		}
	}

	private void updateTrackerData(String dataSetName) {
		String title = dataSetName;
		downloadAppFiles("http://www.adri-silvy.com/android/ar/" + title + ".xml", title + ".xml");
		downloadAppFiles("http://www.adri-silvy.com/android/ar/" + title + ".dat", title + ".dat");
		
		// load texture.
		downloadTextures(title);
		
		deinitApplicationNative();
        initApplicationNative(mScreenWidth, mScreenHeight);
        
        // Run initRendering on the GL Thread
        mGlView.queueEvent(new Runnable()
        {
            public void run()
            {
                mRenderer.initRendering();
            }
        });
		
		switchDatasetAsap(title);
	}

	// Returns the contents of the file in a byte array.
    private byte[] getBytesFromFile(File file) throws IOException {        
        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
            throw new IOException("File is too large!");
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;

        InputStream is = new FileInputStream(file);
        try {
            while (offset < bytes.length
                   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            is.close();
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
        return bytes;
    }

	private void downloadTextures(String title) {
		mTextures = new Vector<Texture>();

		File root = Environment.getExternalStorageDirectory();
	    File localImages = new File(root, title + "_Img");

		if ("Menu".equals(title) && localImages.exists()) {
			for (File f : localImages.listFiles()) {
					Texture t = loadLocalDishOverlay(f);
					if (t != null) {
						t.mName = f.getName(); // File name must be stored due to mapping issue.
						mTextures.add(t);
					}
			}
		}
		else {
			FTPClient client = new FTPClient();
			try {
				client.connect("adri-silvy.com");
				client.enterLocalPassiveMode();
				client.login("android13", "ARdroid13");
			    client.changeWorkingDirectory("ar");
			    client.changeWorkingDirectory(title + "_Img");
			    FTPFile[] ftpFiles = client.listFiles();// if it is directory. then list of file names
			    // Arrays.sort( ftpFiles, new Comparator()
			    // {
			    //  public int compare(final Object o1, final Object o2) {
			    //    return new Long(((FTPFile)o1).getTimestamp().getTime().getTime()).compareTo
			    //         (new Long(((FTPFile) o2).getTimestamp().getTime().getTime()));
			    //  }
			    // }); 
	
				// load file
			    for (FTPFile file : ftpFiles) {
			    	if (!file.isFile()) {
			            continue;
			        }
			        DebugLog.LOGI("File is " + file.getName());
			        // loadTextures();
			        // Texture t = Texture.loadTextureFromApk("http://www.adri-silvy.com/android/ar/"+title + "_Img/" +file.getName());
			        Texture t = loadDishOverlay("http://www.adri-silvy.com/android/ar/" + title + "_Img/" + file.getName()); // Rating, Links, Overlay.
			        t.mName = file.getName(); // File name must be stored due to mapping issue.
			        mTextures.add(t);
			    }
			    client.logout();
			    client.disconnect();
			} catch (SocketException e) {
				DebugLog.LOGE(e.getMessage());
			} catch (IOException e) {
				DebugLog.LOGE(e.getMessage());
			}
		}
	}

	 /**
     * Downloads and image from an Url specified as a parameter returns the
     * array of bytes with the image Data for storing it on the Local Database
     */
    private byte[] downloadImage(final String imageUrl)
    {
        ByteArrayBuffer baf = null;

        try
        {
            URL url = new URL(imageUrl);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            baf = new ByteArrayBuffer(32768);

            // get the bytes one by one
            int current = 0;
            while ((current = bis.read()) != -1)
            {
                baf.append((byte) current);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (baf == null)
        {
            return null;
        }
        else
        {
            return baf.toByteArray();
        }
    }
    
    /** Updates a MenuOverlayView with the menu data specified in parameters */
    private void updateDishView(MenuOverlayView dishView, Dish dish)
    {
    	dishView.setDishName(dish.getName());
    	dishView.setDishPrice(dish.getPriceNormal());
    	dishView.setYourPrice(dish.getPriceDiscount());
    	dishView.setDishRatingCount(dish.getRatingTotal());
        dishView.setRating(dish.getRatingAvg());
        dishView.setIngredients(dish.getIngredients());
        dishView.setDishPictureViewFromBitmap(dish.getThumb());
    }

    private Texture loadLocalDishOverlay(File f) {
		// Initialize the current dish full url to search for the data
		int mTextureSize = 256;
		
		Texture result = null;
			
		Dish mDishData = new Dish();
		mDishData.setRatingAvg("3"); // Test value.
		mDishData.setRatingTotal("23"); // Test value.

		// Gets the dish thumb image
		
		byte[] thumb = null;
		try {
			thumb = getBytesFromFile(f);
		} catch (IOException e) {
			DebugLog.LOGE(e.getMessage());
		}

		if (thumb != null)
		{
			Bitmap bitmap = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
			mDishData.setThumb(bitmap);
		}

		result = finalizeOverlay(mTextureSize, result, mDishData);
		return result;		
	}

	private Texture loadDishOverlay(String url) {
		// Initialize the current dish full url to search for the data
		int mTextureSize = 256;
		
		Texture result = null;
			
		Dish mDishData = new Dish();
		mDishData.setDishUrl(url); // Enable link to FTP server.
		mDishData.setRatingAvg("3"); // Test value.
		mDishData.setRatingTotal("23"); // Test value.

		// Gets the dish thumb image
		byte[] thumb = downloadImage(url);

		if (thumb != null)
		{
			Bitmap bitmap = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
			mDishData.setThumb(bitmap);
		}

		result = finalizeOverlay(mTextureSize, result, mDishData);
		return result;		
	}

	private Texture finalizeOverlay(int mTextureSize, Texture result, Dish mDishData) {
		if (mDishData != null)
		{
			// Generates a View to display the dish data
		    MenuOverlayView dishView = new MenuOverlayView(ImageTargets.this);
		    updateDishView(dishView, mDishData);
			
			// Sets the layout params
		    dishView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
			
			// Sets View measure - This size should be the same as the
		    // texture generated to display the overlay in order for the
		    // texture to be centered in screen
			dishView.measure(MeasureSpec.makeMeasureSpec(mTextureSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mTextureSize, MeasureSpec.EXACTLY));

			// updates layout size
		    dishView.layout(0, 0, dishView.getMeasuredWidth(), dishView.getMeasuredHeight());

			// Draws the View into a Bitmap. Note we are allocating several
			// large memory buffers thus attempt to clear them as soon as
		    // they are no longer required:
			Bitmap bitmap = Bitmap.createBitmap(mTextureSize, mTextureSize, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bitmap);
		    dishView.draw(c);
			
			// Clear the dish view as it is no longer needed
		    dishView = null;
			System.gc();
		                
		    // Allocate int buffer for pixel conversion and copy pixels
		    int width = bitmap.getWidth();
		    int height = bitmap.getHeight();
		                
		    int[] data = new int[bitmap.getWidth() * bitmap.getHeight()];
		    bitmap.getPixels(data, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),
		    bitmap.getHeight());
		                
		    // Recycle the bitmap object as it is no longer needed
		    bitmap.recycle();
		    bitmap = null;
		    c = null;
		    System.gc();   
		                
		    // Generates the Texture from the int buffer
		    result =  Texture.loadTextureFromIntBuffer(data, width, height);
		}
		return result;
	}

	private void downloadAppFiles(String httpUrl, String fileName) {
		File root = Environment.getExternalStorageDirectory();
	    File dest = new File(root, fileName);
	    if (!dest.exists()) {
			try {			      
			      URL url = new URL(httpUrl);
			      URLConnection connection = url.openConnection();
			      connection.connect();
			      // this will be useful so that you can show a typical 0-100% progress bar
			      // int fileLength = connection.getContentLength();
	
			      // download the file
			      InputStream input = new BufferedInputStream(url.openStream());
			      OutputStream output = new FileOutputStream(dest);
	
			      byte data[] = new byte[input.available()];
			      // long total = 0;
			      int count;
			      while ((count = input.read(data)) != -1) {
			          // total += count;
			          // publishing the progress....
			          // System.out.println((int) (total * 100 / fileLength));
			          output.write(data, 0, count);
			      }
	
			      output.flush();
			      output.close();
			      input.close();
			  } catch (Exception e) {
				  DebugLog.LOGE(e.getMessage());
			  }
	    }
	}
    
    private MenuItem checked;
    private boolean mFlash = false;
    private native boolean activateFlash(boolean flash);
    private native boolean autofocus();
    private native boolean setFocusMode(int mode);
    
    /** Returns the number of registered textures. */
    public int getTextureCount()
    {
        return mTextures.size();
    }

    
    /** Returns the texture object at the specified index. */
    public Texture getTexture(int i)
    {
        return mTextures.elementAt(i);
    }

    
    /** A helper for loading native libraries stored in "libs/armeabi*". */
    public static boolean loadLibrary(String nLibName)
    {
        try
        {
            System.loadLibrary(nLibName);
            DebugLog.LOGI("Native library lib" + nLibName + ".so loaded");
            return true;
        }
        catch (UnsatisfiedLinkError ulee)
        {
            DebugLog.LOGE("The library lib" + nLibName +
                            ".so could not be loaded");
        }
        catch (SecurityException se)
        {
            DebugLog.LOGE("The library lib" + nLibName +
                            ".so was not allowed to be loaded");
        }
        
        return false;
    }    
}
