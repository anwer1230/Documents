package org.telegram.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.GroqAiService;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.HashMap;
import java.util.Map;

/**
 * AutoResponderActivity - Smart Automatic Reply System with Rules & Groq AI
 */
public class AutoResponderActivity extends BaseActivity {

    private EditText triggerInput;
    private EditText responseInput;
    private EditText groqApiKeyInput;
    private Switch autoResponderSwitch;
    private Switch aiGulfSwitch;
    private Button addRuleButton;

    public static boolean isAutoResponderEnabled = false;
    public static boolean isAiGulfEnabled = false;
    public static String groqApiKey = "";
    public static Map<String, String> replyRules = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_responder);

        triggerInput = findViewById(R.id.trigger_input);
        responseInput = findViewById(R.id.response_input);
        groqApiKeyInput = findViewById(R.id.groq_api_key_input);
        autoResponderSwitch = findViewById(R.id.responder_switch);
        aiGulfSwitch = findViewById(R.id.ai_gulf_switch);
        addRuleButton = findViewById(R.id.add_rule_button);

        autoResponderSwitch.setChecked(isAutoResponderEnabled);
        autoResponderSwitch.setOnCheckedChangeListener((v, isChecked) -> isAutoResponderEnabled = isChecked);

        if (aiGulfSwitch != null) {
            aiGulfSwitch.setChecked(isAiGulfEnabled);
            aiGulfSwitch.setOnCheckedChangeListener((v, isChecked) -> isAiGulfEnabled = isChecked);
        }

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

        // 1. تحقق أولاً من القواعد اليدوية المحددة
        for (Map.Entry<String, String> entry : replyRules.entrySet()) {
            if (text.contains(entry.getKey())) {
                sendDirectReply(msg, entry.getValue());
                return;
            }
        }

        // 2. إذا كان الذكاء الاصطناعي مفعلاً، يتم الرد الذكي باللهجة الخليجية عبر Groq LLM
        if (isAiGulfEnabled && groqApiKey != null && !groqApiKey.isEmpty()) {
            GroqAiService.generateGulfReply(groqApiKey, msg.message, null, new GroqAiService.GroqCallback() {
                @Override
                public void onSuccess(String reply) {
                    sendDirectReply(msg, reply);
                }

                @Override
                public void onError(String error) {
                    // تجاهل الأخطاء أو تسجيلها
                }
            });
        }
    }

    private static void sendDirectReply(TLRPC.Message msg, String replyText) {
        TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
        req.message = replyText;
        req.random_id = AndroidUtilities.generateRandomId();
        req.peer = new TLRPC.TL_inputPeerChat();
        req.peer.chat_id = msg.peer_id;

        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, null);
    }
}
