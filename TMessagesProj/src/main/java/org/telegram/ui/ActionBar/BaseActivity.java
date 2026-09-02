package org.telegram.ui.ActionBar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import org.telegram.messenger.UserConfig;

/**
 * BaseActivity & Fragment base simulation for DrKLO/Telegram UI hierarchy
 */
public class BaseActivity extends Activity {

    public int currentAccount = UserConfig.selectedAccount;
    public ActionBar actionBar;
    public View fragmentView;
    public BaseActivity parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = new ActionBar(this);
    }

    public boolean onFragmentCreate() {
        return true;
    }

    public void onFragmentDestroy() {
    }

    public View createView(Context context) {
        return null;
    }

    public void presentFragment(BaseActivity fragment) {
        if (fragment != null) {
            fragment.parentLayout = this;
            View view = fragment.createView(this);
            if (view != null) {
                setContentView(view);
            }
        }
    }

    public DrawerLayoutContainer getDrawerLayoutContainer() {
        return new DrawerLayoutContainer();
    }

    public static class DrawerLayoutContainer {
        public void openDrawer(boolean animated) {
        }
        public void closeDrawer(boolean animated) {
        }
    }
}
