package org.telegram.ui.ActionBar;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * ActionBar - Telegram Custom Action Bar view
 */
public class ActionBar extends FrameLayout {

    private String title = "";
    private int backButtonRes;
    private ActionBarMenuOnItemClick itemClickListener;

    public interface ActionBarMenuOnItemClick {
        void onItemClick(int id);
    }

    public ActionBar(Context context) {
        super(context);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setBackButtonImage(int resId) {
        this.backButtonRes = resId;
    }

    public void setActionBarMenuOnItemClick(ActionBarMenuOnItemClick listener) {
        this.itemClickListener = listener;
    }

    public ActionBarMenuOnItemClick getActionBarMenuOnItemClick() {
        return itemClickListener;
    }
}
