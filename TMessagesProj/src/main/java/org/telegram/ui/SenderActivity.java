package org.telegram.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.ArrayList;

/**
 * SenderActivity - Official Telegram Native Activity for Smart & Scheduled Sending
 * Replicates DrKLO/Telegram BaseActivity architecture
 */
public class SenderActivity extends BaseActivity {

    private EditText messageInput;
    private EditText targetInput;
    private Button sendButton;
    private Button scheduleButton;
    private TextView statusLabel;

    private final SenderBackend backend = new SenderBackend();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        messageInput = findViewById(R.id.message_input);
        targetInput = findViewById(R.id.target_input);
        sendButton = findViewById(R.id.send_button);
        scheduleButton = findViewById(R.id.schedule_button);
        statusLabel = findViewById(R.id.status_label);

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString();
            String target = targetInput.getText().toString();
            if (text.isEmpty()) {
                AndroidUtilities.showAlertMessage(SenderActivity.this, "الرجاء كتابة نص الرسالة");
                return;
            }
            backend.resolveAndSend(target, text);
        });

        scheduleButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString();
            ArrayList<Long> ids = new ArrayList<>();
            // Auto schedule batch
            backend.startScheduler(text, ids, 3);
        });
    }

    public static class SenderBackend {
        private final int currentAccount = UserConfig.selectedAccount;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable scheduledRunnable;

        // 1. تحويل الرابط إلى معرف (Chat ID)
        public void resolveAndSend(String link, String text) {
            if (link.contains("t.me/")) {
                String username = link.replace("https://t.me/", "").replace("@", "").trim();
                TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
                req.username = username;

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    if (error == null && response instanceof TLRPC.TL_contacts_resolvedPeer) {
                        TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                        long chatId = res.peer.chat_id != 0 ? res.peer.chat_id : res.peer.user_id;
                        sendMessage(chatId, text);
                    } else {
                        AndroidUtilities.runOnUIThread(() -> 
                            AndroidUtilities.showAlertMessage(null, "خطأ في التحقق من المعرف: " + (error != null ? error.text : "غير معروف"))
                        );
                    }
                });
            } else if (link.startsWith("+") || link.contains("joinchat")) {
                String hash = link.replace("https://t.me/+", "").replace("https://t.me/joinchat/", "").replace("+", "").trim();
                TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
                req.hash = hash;
                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    if (response instanceof TLRPC.TL_chatInvite) {
                        TLRPC.TL_chatInvite invite = (TLRPC.TL_chatInvite) response;
                        // استخراج المعرف وإرسال الرسالة
                        if (invite.chat != null) {
                            sendMessage(invite.chat.id, text);
                        }
                    }
                });
            } else {
                try {
                    long chatId = Long.parseLong(link.trim());
                    sendMessage(chatId, text);
                } catch (Exception e) {
                    AndroidUtilities.runOnUIThread(() -> 
                        AndroidUtilities.showAlertMessage(null, "معرف المحادثة أو الرابط غير صالح")
                    );
                }
            }
        }

        // 2. منطق الإرسال الفعلي مع "الحماية الذكية" (Salam Protocol)
        public void sendMessage(long chatId, String text) {
            TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
            req.message = text;
            req.random_id = AndroidUtilities.generateRandomId();
            req.peer = new TLRPC.TL_inputPeerChat();
            req.peer.chat_id = chatId;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (error != null && "CHAT_WRITE_FORBIDDEN".equals(error.text)) {
                    // المجموعة محمية: أرسل "السلام عليكم" أولاً ثم قم بالتعديل
                    sendSalamThenEdit(chatId, text);
                }
            });
        }

        public void sendSalamThenEdit(long chatId, String text) {
            TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
            req.message = "السلام عليكم ورحمة الله وبركاته";
            req.random_id = AndroidUtilities.generateRandomId();
            req.peer = new TLRPC.TL_inputPeerChat();
            req.peer.chat_id = chatId;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (response instanceof TLRPC.TL_updates) {
                    TLRPC.TL_updates updates = (TLRPC.TL_updates) response;
                    if (!updates.updates.isEmpty() && updates.updates.get(0).message != null) {
                        int msgId = updates.updates.get(0).message.id;
                        // بعد 30 ثانية، عدّل الرسالة إلى النص الأصلي لمنع الحظر
                        handler.postDelayed(() -> editMessage(chatId, msgId, text), 30000);
                    }
                }
            });
        }

        private void editMessage(long chatId, int msgId, String newText) {
            TLRPC.TL_messages_editMessage req = new TLRPC.TL_messages_editMessage();
            req.message = newText;
            req.peer = new TLRPC.TL_inputPeerChat();
            req.peer.chat_id = chatId;
            req.id = msgId;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
        }

        // 3. المجدول الزمني (الإرسال الدوري)
        public void startScheduler(String text, ArrayList<Long> chatIds, long intervalMinutes) {
            stopScheduler();
            scheduledRunnable = new Runnable() {
                @Override
                public void run() {
                    for (Long id : chatIds) {
                        sendMessage(id, text);
                    }
                    handler.postDelayed(this, intervalMinutes * 60 * 1000);
                }
            };
            handler.postDelayed(scheduledRunnable, 5000);
        }

        public void stopScheduler() {
            if (scheduledRunnable != null) {
                handler.removeCallbacks(scheduledRunnable);
                scheduledRunnable = null;
            }
        }
    }
}
