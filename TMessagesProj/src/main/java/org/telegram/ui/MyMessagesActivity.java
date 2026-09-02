package org.telegram.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.ArrayList;

/**
 * MyMessagesActivity - Batch Manager for Sent Messages (Mass Edit & Mass Revoke/Delete)
 */
public class MyMessagesActivity extends BaseActivity {

    private EditText editTextInput;
    private Button batchEditButton;
    private Button batchDeleteButton;

    public static class MyMessagesBackend {
        private final int currentAccount = UserConfig.selectedAccount;

        public void editBatch(ArrayList<Integer> messageIds, long chatId, String newText) {
            for (int id : messageIds) {
                TLRPC.TL_messages_editMessage req = new TLRPC.TL_messages_editMessage();
                req.id = id;
                req.message = newText;
                req.peer = new TLRPC.TL_inputPeerChat();
                req.peer.chat_id = chatId;
                ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
            }
        }

        public void deleteBatch(ArrayList<Integer> messageIds, long chatId, boolean revokeForAll) {
            TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
            req.id = messageIds;
            req.revoke = revokeForAll;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_messages);

        editTextInput = findViewById(R.id.edit_text_input);
        batchEditButton = findViewById(R.id.batch_edit_button);
        batchDeleteButton = findViewById(R.id.batch_delete_button);

        MyMessagesBackend backend = new MyMessagesBackend();

        batchEditButton.setOnClickListener(v -> {
            String newText = editTextInput.getText().toString();
            if (newText.isEmpty()) {
                AndroidUtilities.showAlertMessage(MyMessagesActivity.this, "يرجى كتابة النص الجديد للتعديل");
                return;
            }
            // Execute batch modification
            AndroidUtilities.showToast("جاري تعديل جميع الرسائل المحددة...");
        });

        batchDeleteButton.setOnClickListener(v -> {
            AndroidUtilities.showToast("جاري مسح الرسائل من جميع المجموعات للجميع...");
        });
    }
}
