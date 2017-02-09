package com.goertek.unitylauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.unity3d.player.UnityPlayerActivity;

import java.util.List;

/*
   若继承 UnityPlayerActivity，需要注释掉 setContentView()
   若不继承 UnityPlayerActivity，则需要 setContentView( new new UnityPlayer(this) )
 */
//public class MainActivity extends UnityPlayerActivity {
public class MainActivity extends Activity {

    private List<ResolveInfo> apps;
    GridView appsGrid;

    GTKWiFi mGTKWiFi;
    GTKBluetooth mGTKBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadApps();
        appsGrid = (GridView) findViewById(R.id.apps_list);
        appsGrid.setAdapter(new AppsAdapter());
        appsGrid.setOnItemClickListener(clickListener);

        /*mGTKWiFi = new GTKWiFi(getApplicationContext());
        mGTKWiFi.GTKGetWifiScanResults();*/

        mGTKBluetooth = new GTKBluetooth(getApplicationContext());
        mGTKBluetooth.GTKScanBT();
    }

    public void showTestToast() {
        Toast.makeText(getApplicationContext(), "This is only for test", Toast.LENGTH_LONG).show();
    }

    private void loadApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        new ImageView(MainActivity.this);
        apps = getPackageManager().queryIntentActivities(intent, 0);
    }

    private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ResolveInfo info = apps.get(position);
            String pkg = info.activityInfo.packageName;           //包名
            String cls = info.activityInfo.name;                  //主Activity类
            ComponentName componet = new ComponentName(pkg, cls);

            Intent intent = new Intent();
            intent.setComponent(componet);
            startActivity(intent);
        }
    };

    public class AppsAdapter extends BaseAdapter {
        public AppsAdapter() {

        }

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int i) {
            return apps.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView iv;
            if (view == null) {
                iv = new ImageView(MainActivity.this);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setLayoutParams(new GridView.LayoutParams(60, 60));
            } else {
                iv = (ImageView) view;
            }
            ResolveInfo info = apps.get(i);
            iv.setImageDrawable(info.activityInfo.loadIcon(getPackageManager()));
            return iv;
        }
    }
}
