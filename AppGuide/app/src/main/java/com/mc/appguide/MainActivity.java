package com.mc.appguide;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String KEY_CURRENT_NAV_TAB_ID = "current_nav_tab_id";
    private static final String KEY_DISABLED_NAV_TAB_ID = "disabled_nav_tab_id";
    private LinearLayout mBottomNav;
    private int mCurrentNavTabId = R.id.nav_tab_tuner;
    private int mDisabledNavTabId = -1;
    private SparseArray<Fragment> fragmentCache = new SparseArray<>();
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getFragmentManager();

        mBottomNav = findViewById(R.id.nav_bar);
        findViewById(R.id.nav_tab_tuner).setOnClickListener(this);
        findViewById(R.id.nav_tab_record).setOnClickListener(this);
        findViewById(R.id.nav_tab_settings).setOnClickListener(this);

        if (savedInstanceState != null) {
            mCurrentNavTabId = savedInstanceState.getInt(KEY_CURRENT_NAV_TAB_ID, R.id.nav_tab_tuner);
            mDisabledNavTabId = savedInstanceState.getInt(KEY_DISABLED_NAV_TAB_ID, -1);
        }
        switchRadioFragment();

        DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                runOnUiThread(() -> {
                });
            }
        };
    }

    @Override
    public void onClick(View v) {
        int targetTabId = v.getId();
        if (targetTabId != mCurrentNavTabId && targetTabId != mDisabledNavTabId) {
            mCurrentNavTabId = targetTabId;
            switchRadioFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragmentCache.clear();
        fragmentCache = null;
    }

    @SuppressWarnings("deprecation")
    private void switchRadioFragment() {
        Fragment fragment = fragmentCache.get(mCurrentNavTabId);
        if (fragment == null) {
            fragment = createFragment(mCurrentNavTabId);
            fragmentCache.put(mCurrentNavTabId, fragment);
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        updateFragmentTabState();
    }

    @SuppressLint("NonConstantResourceId")
    private Fragment createFragment(int navTabId) {
        switch (navTabId) {
            case R.id.nav_tab_tuner:
                return new SettingsFragment();
            case R.id.nav_tab_record:
                return new RecordFilesFragment();
            case R.id.nav_tab_settings:
                return new SettingsFragment();
            default:
                return new SettingsFragment();
        }
    }

    private void updateFragmentTabState() {
        for (int i = 0; i < mBottomNav.getChildCount(); i++) {
            View child = mBottomNav.getChildAt(i);
            child.setSelected(child.getId() == mCurrentNavTabId);
            setFragmentTabEnabled(child, child.getId() != mDisabledNavTabId);
        }
    }

    public void setFragmentTabEnabled(View button, boolean enabled) {
        if (button != null) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1.0f : 0.5f);
            if (!enabled) mDisabledNavTabId = button.getId();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_NAV_TAB_ID, mCurrentNavTabId);
        outState.putInt(KEY_DISABLED_NAV_TAB_ID, mDisabledNavTabId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentNavTabId = savedInstanceState.getInt(KEY_CURRENT_NAV_TAB_ID, R.id.nav_tab_tuner);
        mDisabledNavTabId = savedInstanceState.getInt(KEY_DISABLED_NAV_TAB_ID, -1);
        switchRadioFragment();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("Main", "landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("Main", "portrait");
        }
    }
}
