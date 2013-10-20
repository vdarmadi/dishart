package edu.sfsu.cs.orange.ocr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;


public class UploadImageTask extends AsyncTask<String, Void, Integer> {
    /** The system calls this to perform work in a worker thread and
     * delivers it the parameters given to AsyncTask.execute() */

    protected Integer doInBackground(String... fileNames) {

        try {

            boolean mExternalStorageAvailable = false;
            boolean mExternalStorageWriteable = false;
            String state = Environment.getExternalStorageState();

            if (Environment.MEDIA_MOUNTED.equals(state)) {
                // We can read and write the media
                mExternalStorageAvailable = mExternalStorageWriteable = true;
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                // We can only read the media
                mExternalStorageAvailable = true;
                mExternalStorageWriteable = false;
            } else {
                // Something else is wrong. It may be one of many other states, but all we need
                //  to know is we can neither read nor write
                mExternalStorageAvailable = mExternalStorageWriteable = false;
            }

            Log.i("Dishartzz", "ExternalStorageAvailable" +  mExternalStorageAvailable );
            //Toast.makeText( getApplicationContext(), "ExternalStorageAvailable" +  mExternalStorageAvailable, Toast.LENGTH_SHORT).show();

       

            HttpPost httpPost = new HttpPost("http://meerab.xen.prgmr.com:3000/images");
        DefaultHttpClient httpclient = new DefaultHttpClient();


            
            //String fileName = "//sdcard/Pictures/Screenshots/Screenshot_2013-09-15-18-44-30.png";
        	String fileName = fileNames[0];
        Log.i("Tejal file name", fileName);

            // /storage/emulated/0/Pictures/Photo on 2-19-13 at 4.06 PM.jpg
            //Toast.makeText( getApplicationContext(), "Filename" +  fileName, Toast.LENGTH_SHORT).show();

            File file = new File( fileName);

            //Toast.makeText( getApplicationContext(), "ExternalStorageAvailable" +  mExternalStorageAvailable, Toast.LENGTH_SHORT);

        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new FileBody(file, "image/jpg");
        mpEntity.addPart("image[imagefile]", cbFile);


        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
      
        mpEntity.addPart("image[restaurantname]", new StringBody("Amber"));
        mpEntity.addPart("image[dishname]", new StringBody("Tikka Masala"));
        httpPost.setEntity(mpEntity);

        
          
            HttpResponse response2 = httpclient.execute(httpPost);
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            //EntityUtils.consume(entity2);
            String str = EntityUtils.toString(entity2);
            System.out.println(str);
        } catch (IOException e)
        {
            Log.e(" Exception ", e.toString());
            return null;
        }
       finally {
           // httpPost.releaseConnection();
        }


        return 1;
    }
}