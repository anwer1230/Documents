package org.telegram.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.HashMap;
import java.util.Map;

/**
 * AutoResponderActivity - Smart Automatic Reply System for Telegram
 */
public class AutoResponderActivity extends BaseActivity {

    private EditText triggerInput;
    private EditText responseInput;
    private Switch autoResponderSwitch;
    private Button addRuleButton;

    public static boolean isAutoResponderEnabled = false;
    public static Map<String, String> replyRules = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_responder);

        triggerInput = findViewById(R.id.trigger_input);
        responseInput = findViewById(R.id.response_input);
        autoResponderSwitch = findViewById(R.id.responder_switch);
        addRuleButton = findViewById(R.id.add_rule_button);

        autoResponderSwitch.setChecked(isAutoResponderEnabled);
        autoResponderSwitch.setOnCheckedChangeListener((v, isChecked) -> isAutoResponderEnabled = isChecked);

        addRuleButton.setOnClickListener(v -> {
            String trigger = triggerInput.getText().toString().trim().toLowerCase();
            String reply = responseInput.getText().toString().trim();
            if (!trigger.isEmpty() && !reply.isEmpty()) {
                replyRules.put(trigger, reply);
                AndroidUtilities.showToast("تمت إضافة قاعدة الرد التلقائي بنجاح");
                triggerInput.setText("");
                responseInput.setText("");
            }
        });
    }

    public static void checkAndAutoReply(TLRPC.Message msg) {
        if (!isAutoResponderEnabled || msg == null || msg.out || msg.message == null) return;

        String text = msg.message.trim().toLowerCase();
        for (Map.Entry<String, String> entry : replyRules.entrySet()) {
            if (text.contains(entry.getKey())) {
                TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
                req.message = entry.getValue();
                req.random_id = AndroidUtilities.generateRandomId();
                req.peer = new TLRPC.TL_inputPeerChat();
                req.peer.chat_id = msg.peer_id != null ? msg.peer_id.chat_id : msg.chat_id;

                ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, null);
                break;
            }
        }
    }
}
