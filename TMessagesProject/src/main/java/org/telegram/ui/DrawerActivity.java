package org.telegram.ui;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * DrawerActivity - Official Telegram Navigation Drawer Item Manager
 */
public class DrawerActivity {

    public static class DrawerItem {
        public String title;
        public int iconRes;
        public Runnable onClick;

        public DrawerItem(String title, int iconRes, Runnable onClick) {
            this.title = title;
            this.iconRes = iconRes;
            this.onClick = onClick;
        }
    }

    private final List<DrawerItem> items = new ArrayList<>();
    private final BaseActivity parentActivity;

    public DrawerActivity(BaseActivity parentActivity) {
        this.parentActivity = parentActivity;
        setupAutomationDrawerItems();
    }

    public void addDrawerItem(String title, int iconRes, Runnable onClick) {
        items.add(new DrawerItem(title, iconRes, onClick));
    }

    public List<DrawerItem> getItems() {
        return items;
    }

    private void setupAutomationDrawerItems() {
        addDrawerItem("🔍 البحث والانضمام الفوري", R.drawable.ic_search, () -> {
            if (parentActivity != null) {
                parentActivity.presentFragment(new SearchJoinActivity());
            }
        });

        addDrawerItem("📤 الإرسال الذكي", R.drawable.ic_send, () -> {
            if (parentActivity != null) {
                parentActivity.presentFragment(new SenderActivity());
            }
        });

        addDrawerItem("🚨 المراقبة ورصد الكلمات", R.drawable.ic_eye, () -> {
            if (parentActivity != null) {
                parentActivity.presentFragment(new MonitorActivity());
            }
        });

        addDrawerItem("📋 رسائلي والتعديل الجماعي", R.drawable.ic_messages, () -> {
            if (parentActivity != null) {
                parentActivity.presentFragment(new MyMessagesActivity());
            }
        });

        addDrawerItem("🤖 الردود التلقائية والذكاء", R.drawable.ic_bot, () -> {
            if (parentActivity != null) {
                parentActivity.presentFragment(new AutoResponderActivity());
            }
        });
    }
}
