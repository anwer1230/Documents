package org.telegram.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MonitorActivity - Official Telegram Native Activity for Real-time Keywords & Channel Monitoring
 */
public class MonitorActivity extends BaseActivity {

    private EditText keywordsInput;
    private Switch monitorSwitch;
    private TextView logsView;
    private Button saveButton;

    public static boolean isMonitoringActive = false;
    public static List<String> monitoredKeywords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        keywordsInput = findViewById(R.id.keywords_input);
        monitorSwitch = findViewById(R.id.monitor_switch);
        logsView = findViewById(R.id.logs_view);
        saveButton = findViewById(R.id.save_button);

        monitorSwitch.setChecked(isMonitoringActive);

        monitorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isMonitoringActive = isChecked;
            String text = keywordsInput.getText().toString();
            updateKeywords(text);
        });

        saveButton.setOnClickListener(v -> {
            String text = keywordsInput.getText().toString();
            updateKeywords(text);
            AndroidUtilities.showAlertMessage(MonitorActivity.this, "تم حفظ الكلمات المفتاحية للمراقبة الفورية");
        });
    }

    private void updateKeywords(String text) {
        monitoredKeywords.clear();
        if (!text.trim().isEmpty()) {
            String[] parts = text.split("[,\\n]+");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    monitoredKeywords.add(p.trim().toLowerCase());
                }
            }
        }
    }

    /**
     * Process message from MessagesController and forward to Saved Messages
     */
    public static void checkAndForwardToSaved(TLRPC.Message msg) {
        if (!isMonitoringActive || msg == null || msg.message == null) return;

        String content = msg.message.toLowerCase();
        for (String kw : monitoredKeywords) {
            if (content.contains(kw)) {
                TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
                req.message = "🚨 [تنبيه رصد فوري]\nالكلمة المرصودة: #" + kw + "\nالمرسل: " + msg.from_id + "\n\nالنص:\n" + msg.message;
                req.random_id = AndroidUtilities.generateRandomId();
                req.peer = new TLRPC.TL_inputPeerUser();
                req.peer.user_id = UserConfig.getClientUserId(); // حسابك الشخصي (Saved Messages)

                ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, null);
                break;
            }
        }
    }
}
