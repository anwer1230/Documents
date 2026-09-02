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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoJoinActivity & SearchJoinActivity - Automatic Link Parsing & Group Join Engine
 */
public class AutoJoinActivity extends BaseActivity {

    private EditText linksInput;
    private Button startJoinButton;
    private Switch autoRadarSwitch;
    private TextView progressText;

    public static boolean isAutoJoinRadarEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_join);

        linksInput = findViewById(R.id.links_input);
        startJoinButton = findViewById(R.id.start_join_button);
        autoRadarSwitch = findViewById(R.id.radar_switch);
        progressText = findViewById(R.id.progress_text);

        autoRadarSwitch.setChecked(isAutoJoinRadarEnabled);
        autoRadarSwitch.setOnCheckedChangeListener((v, isChecked) -> isAutoJoinRadarEnabled = isChecked);

        startJoinButton.setOnClickListener(v -> {
            String text = linksInput.getText().toString();
            processJoinBatch(text);
        });
    }

    public static void processLiveMessageForLinks(String text) {
        if (!isAutoJoinRadarEnabled || text == null) return;
        Pattern pattern = Pattern.compile("(https?://)?t\\.me/(\\w+|\\+[a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String link = matcher.group();
            joinTelegramLink(link);
        }
    }

    public static void joinTelegramLink(String link) {
        int currentAccount = UserConfig.selectedAccount;

        if (link.contains("+") || link.contains("joinchat")) {
            String hash = link.replace("https://t.me/+", "").replace("https://t.me/joinchat/", "").replace("+", "").trim();
            TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
            req.hash = hash;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        AndroidUtilities.showToast("تم الانضمام بنجاح للرابط الخاص: " + hash);
                    }
                });
            });
        } else {
            String username = link.replace("https://t.me/", "").replace("@", "").trim();
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                    if (res.chats != null && !res.chats.isEmpty()) {
                        TLRPC.Chat chat = res.chats.get(0);
                        TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                        joinReq.channel = new TLRPC.TL_inputChannel();
                        joinReq.channel.channel_id = chat.id;
                        joinReq.channel.access_hash = chat.access_hash;

                        ConnectionsManager.getInstance(currentAccount).sendRequest(joinReq, (joinResp, joinErr) -> {
                            AndroidUtilities.runOnUIThread(() -> {
                                if (joinErr == null) {
                                    AndroidUtilities.showToast("تم الانضمام بنجاح: " + username);
                                }
                            });
                        });
                    }
                }
            });
        }
    }

    private void processJoinBatch(String text) {
        Pattern pattern = Pattern.compile("(https?://)?t\\.me/(\\w+|\\+[a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            joinTelegramLink(matcher.group());
            count++;
        }
        AndroidUtilities.showAlertMessage(this, "جاري الانضمام إلى " + count + " رابط مجموعة/قناة بالتوالي...");
    }
}
