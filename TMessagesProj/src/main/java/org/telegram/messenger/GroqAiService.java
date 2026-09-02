package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GroqAiService - Integration with Groq Cloud LLM (llama3-8b-8192) for Gulf/Arabic Smart Assistant
 */
public class GroqAiService {

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient();

    public interface GroqCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    public static void generateGulfReply(String apiKey, String userMessage, String personaPrompt, GroqCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("model", "llama3-8b-8192");
            json.put("temperature", 0.7);

            JSONArray messages = new JSONArray();
            
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", personaPrompt != null && !personaPrompt.isEmpty() ? personaPrompt : "أنت مساعد ذكي ولطيف، رد دائماً بلهجة خليجية بيضاء مختصرة، ودودة، ومفيدة.");
            messages.put(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);

            json.put("messages", messages);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(GROQ_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        if (callback != null) callback.onError("HTTP " + response.code() + ": " + response.message());
                        return;
                    }
                    try {
                        String bodyStr = response.body().string();
                        JSONObject resObj = new JSONObject(bodyStr);
                        String reply = resObj.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        if (callback != null) callback.onSuccess(reply);
                    } catch (Exception e) {
                        if (callback != null) callback.onError("Failed to parse JSON: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) callback.onError(e.getMessage());
        }
    }
}
