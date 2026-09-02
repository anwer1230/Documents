package org.telegram.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseActivity;
import java.util.ArrayList;

/**
 * SearchJoinActivity - Search public Telegram channels/groups by keyword & batch join instantly
 */
public class SearchJoinActivity extends BaseActivity {

    private EditText searchKeywordInput;
    private Button searchButton;
    private Button joinAllButton;
    private TextView resultsCountText;
    private ProgressBar progressBar;

    private final ArrayList<TLRPC.Chat> foundChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_join);

        searchKeywordInput = findViewById(R.id.search_keyword_input);
        searchButton = findViewById(R.id.search_button);
        joinAllButton = findViewById(R.id.join_all_button);
        resultsCountText = findViewById(R.id.results_count_text);
        progressBar = findViewById(R.id.search_progress);

        searchButton.setOnClickListener(v -> {
            String query = searchKeywordInput.getText().toString().trim();
            if (query.isEmpty()) {
                AndroidUtilities.showAlertMessage(this, "يرجى كتابة كلمة البحث");
                return;
            }
            performSearch(query);
        });

        joinAllButton.setOnClickListener(v -> {
            if (foundChats.isEmpty()) {
                AndroidUtilities.showAlertMessage(this, "لا توجد مجموعات للانضمام إليها");
                return;
            }
            joinAllFoundChats();
        });
    }

    private void performSearch(String query) {
        foundChats.clear();
        progressBar.setVisibility(android.view.View.VISIBLE);

        TLRPC.TL_contacts_search req = new TLRPC.TL_contacts_search();
        req.q = query;
        req.limit = 50;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                progressBar.setVisibility(android.view.View.GONE);
                if (response instanceof TLRPC.TL_contacts_found) {
                    TLRPC.TL_contacts_found res = (TLRPC.TL_contacts_found) response;
                    foundChats.addAll(res.chats);
                    resultsCountText.setText("تم العثور على " + foundChats.size() + " قناة/مجموعة عامة");
                    AndroidUtilities.showToast("تم العثور على " + foundChats.size() + " نتيجة");
                } else {
                    resultsCountText.setText("لم يتم العثور على نتائج أو حدث خطأ");
                }
            });
        });
    }

    private void joinAllFoundChats() {
        int delay = 0;
        for (TLRPC.Chat chat : foundChats) {
            AndroidUtilities.runOnUIThreadDelayed(() -> {
                TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                joinReq.channel = new TLRPC.TL_inputChannel();
                joinReq.channel.channel_id = chat.id;
                joinReq.channel.access_hash = chat.access_hash;

                ConnectionsManager.getInstance(currentAccount).sendRequest(joinReq, (response, error) -> {
                    if (error == null) {
                        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showToast("تم الانضمام: " + chat.title));
                    }
                });
            }, delay);
            delay += 2500; // فاصل زمني آمن لتجنب حظر تكرار الانضمام (FLOOD_WAIT)
        }
        AndroidUtilities.showAlertMessage(this, "بدأت عملية الانضمام المتتابع لـ " + foundChats.size() + " مجموعة بفاصل زمني آمن.");
    }
}
