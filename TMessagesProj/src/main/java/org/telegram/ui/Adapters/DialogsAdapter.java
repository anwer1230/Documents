package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.DialogCell;
import java.util.ArrayList;

/**
 * DialogsAdapter - Provides data connection between MessagesController dialogs and ListView / Recycler.
 */
public class DialogsAdapter extends BaseAdapter {

    private final Context mContext;
    private final int currentAccount;
    private final int dialogsType; // 0 = all, 1 = unread, etc.

    public DialogsAdapter(Context context, int account, int type) {
        mContext = context;
        currentAccount = account;
        dialogsType = type;
    }

    @Override
    public int getCount() {
        ArrayList<TLRPC.Dialog> list = getDialogsArray();
        return list != null ? list.size() : 0;
    }

    @Override
    public TLRPC.Dialog getItem(int position) {
        ArrayList<TLRPC.Dialog> list = getDialogsArray();
        if (list != null && position >= 0 && position < list.size()) {
            return list.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        TLRPC.Dialog dialog = getItem(position);
        return dialog != null ? dialog.id : position;
    }

    public ArrayList<TLRPC.Dialog> getDialogsArray() {
        return MessagesController.getInstance(currentAccount).dialogs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DialogCell cell;
        if (convertView instanceof DialogCell) {
            cell = (DialogCell) convertView;
        } else {
            cell = new DialogCell(mContext);
        }

        TLRPC.Dialog dialog = getItem(position);
        cell.setDialog(dialog, currentAccount);

        return cell;
    }
}
