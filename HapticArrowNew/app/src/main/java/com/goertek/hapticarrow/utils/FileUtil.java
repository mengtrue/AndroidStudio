package com.goertek.hapticarrow.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.goertek.hapticarrow.Configuration;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Created by dell on 2016/10/26.
 */

public class FileUtil {
    public static Configuration ReadConfiguration(Context context) {
        Configuration configuration = null;
        try {

            // TODO: 2016/11/3 first check the inner configuration file
            String innerConfig = getFromAssets(context, "arrow_config.txt");

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

            File yourFile = null;
            FileInputStream stream = null;
            String jsonStr = null;
            try {
                yourFile = new File(Environment.getExternalStorageDirectory(), "arrow_config.txt");
                stream = new FileInputStream(yourFile);
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

                jsonStr = Charset.defaultCharset().decode(bb).toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }

            // TODO: 2016/11/3 replace the json str with inner configuration file
            if (TextUtils.isEmpty(jsonStr)) {
                jsonStr = innerConfig;
            }

            Gson gson = new Gson();
            configuration = gson.fromJson(jsonStr, Configuration.class);
            Log.d("FileUtil", configuration.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
        return configuration;
    }

    /**
     * 从assets中读取txt
     */
    public static String getFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
