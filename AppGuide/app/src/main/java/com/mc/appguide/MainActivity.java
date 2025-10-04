package com.mc.appguide;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements View.OnClickListener {
    private LinearLayout navBar;
    private Button lockBtn;
    private int currentTabId = R.id.nav_home;
    private boolean isNavigationBlocked = false; // Fragment缓存
    private SparseArray<Fragment> fragmentCache = new SparseArray<>();
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager = getFragmentManager();

        // 初始化导航栏
        navBar = findViewById(R.id.nav_bar);

        // 设置导航按钮点击监听
        findViewById(R.id.nav_home).setOnClickListener(this);
        findViewById(R.id.nav_contacts).setOnClickListener(this);
        findViewById(R.id.nav_discover).setOnClickListener(this);
        findViewById(R.id.nav_me).setOnClickListener(this);

        // 锁定按钮
        lockBtn = findViewById(R.id.lock_btn);
        lockBtn.setOnClickListener(v -> {
            isNavigationBlocked = !isNavigationBlocked;
            updateNavButtonsState();
            lockBtn.setText(isNavigationBlocked ? "解锁导航" : "锁定导航");
        });

        // 恢复保存的状态
        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt("CURRENT_TAB", R.id.nav_home);
            isNavigationBlocked = savedInstanceState.getBoolean("IS_BLOCKED", false);
        }

        // 显示初始Fragment
        switchToTab(currentTabId);
        setSelectedTab(currentTabId);
        lockBtn.setText(isNavigationBlocked ? "解锁导航" : "锁定导航");

        // 初始化禁用"发现"按钮
        setNavButtonEnabled(R.id.nav_discover, false);
    }

    @Override
    public void onClick(View v) {
        if (isNavigationBlocked) {
            showBlockedMessage();
            return;
        }

        int targetTabId = v.getId();
        if (targetTabId != currentTabId) {
            switchToTab(targetTabId);
            setSelectedTab(targetTabId);
            currentTabId = targetTabId;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragmentCache.clear();
        fragmentCache = null;
    }

    private void switchToTab(int tabId) {
        // 从缓存中获取Fragment
        Fragment fragment = fragmentCache.get(tabId);

        if (fragment == null) {
            // 创建新的Fragment实例
            fragment = createFragmentForTab(tabId);
            fragmentCache.put(tabId, fragment);
        }

        // 执行Fragment切换
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @SuppressLint("NonConstantResourceId")
    private Fragment createFragmentForTab(int tabId) {
        switch (tabId) {
            case R.id.nav_contacts:
                return new ContactsFragment();
            case R.id.nav_discover:
                return new DiscoverFragment();
            case R.id.nav_me:
                return new MeFragment();
            case R.id.nav_home:
            default:
                return new HomeFragment();
        }
    }

    private void setSelectedTab(int selectedId) {
        for (int i = 0; i < navBar.getChildCount(); i++) {
            View child = navBar.getChildAt(i);
            if (child.getId() == R.id.lock_btn) continue;

            child.setSelected(child.getId() == selectedId);
        }
    }

    private void showBlockedMessage() {
        Toast.makeText(this, "请完成当前操作后再切换", Toast.LENGTH_SHORT).show();
    }

    // ====== 导航栏控制方法 =======
    public void setNavigationBlocked(boolean blocked) {
        isNavigationBlocked = blocked;
        updateNavButtonsState();
    }

    private void updateNavButtonsState() {
        for (int i = 0; i < navBar.getChildCount(); i++) {
            View child = navBar.getChildAt(i);
            if (child.getId() == R.id.lock_btn) continue;

            child.setEnabled(!isNavigationBlocked);
            float alpha = isNavigationBlocked ? 0.5f : 1.0f;
            child.setAlpha(alpha);
        }
    }

    public void setNavButtonEnabled(int buttonId, boolean enabled) {
        View button = findViewById(buttonId);
        if (button != null) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CURRENT_TAB", currentTabId);
        outState.putBoolean("IS_BLOCKED", isNavigationBlocked);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentTabId = savedInstanceState.getInt("CURRENT_TAB", R.id.nav_home);
        isNavigationBlocked = savedInstanceState.getBoolean("IS_BLOCKED", false);
        lockBtn.setText(isNavigationBlocked ? "解锁导航" : "锁定导航");
        switchToTab(currentTabId);
        setSelectedTab(currentTabId);
        updateNavButtonsState();
    }

    // === Fragment 定义 ===
    public static class HomeFragment extends Fragment {
        public HomeFragment() {}
    }

    public static class ContactsFragment extends Fragment {
        public ContactsFragment() {}
    }

    public static class DiscoverFragment extends Fragment {
        public DiscoverFragment() {}
    }

    public static class MeFragment extends Fragment {
        public MeFragment() {}
    }
}
