package com.example.walletwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChatActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private DrawerLayout drawerLayout;

    private ImageButton btnOpenDrawer;
    private ImageButton btnBack;
    private ImageButton btnSend;

    private EditText etUserQuery;

    private LinearLayout chatContainer;
    private LinearLayout historyContainer;
    private LinearLayout btnNewChat;

    private ScrollView scrollViewChat;

    private TextView tvCurrentChatTitle;


    // =========================================================
    // USER
    // =========================================================

    private String currentEmail = "";


    // =========================================================
    // CHAT STORAGE
    // =========================================================

    private SharedPreferences chatPrefs;

    private static final String CHAT_SESSIONS = "chat_sessions";

    private String currentChatId = "";


    // =========================================================
    // GEMINI
    // =========================================================

    /*
     * WalletWise AI model
     *
     * IMPORTANT:
     * This is the only model used here.
     */
    private static final String GEMINI_MODEL =
            "gemini-3.5-flash-lite";


    private static final MediaType JSON =
            MediaType.get(
                    "application/json; charset=utf-8"
            );


    // =========================================================
    // NETWORK
    // =========================================================

    private final OkHttpClient client =
            new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .callTimeout(150, TimeUnit.SECONDS)
                    .build();


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ai_chat);


        // =====================================================
        // GET LOGGED-IN USER
        // =====================================================

        SharedPreferences globalPrefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        currentEmail =
                globalPrefs.getString(
                        "logged_in_email",
                        ""
                );

        if (currentEmail == null) {
            currentEmail = "";
        }

        currentEmail =
                currentEmail.trim().toLowerCase();


        // =====================================================
        // USER-SPECIFIC CHAT STORAGE
        // =====================================================

        chatPrefs =
                getSharedPreferences(
                        "AIChats_" + currentEmail,
                        Context.MODE_PRIVATE
                );


        // =====================================================
        // FIND VIEWS
        // =====================================================

        drawerLayout =
                findViewById(R.id.drawerLayout);

        btnOpenDrawer =
                findViewById(R.id.btnOpenDrawer);

        btnBack =
                findViewById(R.id.btnBack);

        btnSend =
                findViewById(R.id.btnSend);

        etUserQuery =
                findViewById(R.id.etUserQuery);

        chatContainer =
                findViewById(R.id.chatContainer);

        historyContainer =
                findViewById(R.id.historyContainer);

        btnNewChat =
                findViewById(R.id.btnNewChat);

        scrollViewChat =
                findViewById(R.id.scrollViewChat);

        tvCurrentChatTitle =
                findViewById(R.id.tvCurrentChatTitle);


        // =====================================================
        // LOAD / CREATE CHAT
        // =====================================================

        loadOrCreateChat();


        // =====================================================
        // LOAD HISTORY
        // =====================================================

        loadHistory();


        // =====================================================
        // DRAWER
        // =====================================================

        if (btnOpenDrawer != null) {

            btnOpenDrawer.setOnClickListener(v -> {

                if (drawerLayout != null) {

                    drawerLayout.openDrawer(
                            Gravity.LEFT
                    );
                }
            });
        }


        // =====================================================
        // BACK
        // =====================================================

        if (btnBack != null) {

            btnBack.setOnClickListener(v ->
                    finish()
            );
        }


        // =====================================================
        // SEND BUTTON
        // =====================================================

        if (btnSend != null) {

            btnSend.setOnClickListener(v ->
                    sendMessage()
            );
        }


        // =====================================================
        // KEYBOARD SEND
        // =====================================================

        if (etUserQuery != null) {

            etUserQuery.setOnEditorActionListener(
                    (v, actionId, event) -> {

                        if (actionId ==
                                EditorInfo.IME_ACTION_SEND) {

                            sendMessage();

                            return true;
                        }

                        return false;
                    }
            );
        }


        // =====================================================
        // NEW CHAT
        // =====================================================

        if (btnNewChat != null) {

            btnNewChat.setOnClickListener(v -> {

                createNewChat();

                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                }
            });
        }
    }


    // =========================================================
    // LOAD OR CREATE CHAT
    // =========================================================

    private void loadOrCreateChat() {

        String sessionsJson =
                chatPrefs.getString(
                        CHAT_SESSIONS,
                        "[]"
                );

        try {

            JSONArray sessions =
                    new JSONArray(sessionsJson);


            if (sessions.length() == 0) {

                createNewChat();

                return;
            }


            JSONObject lastChat =
                    sessions.getJSONObject(
                            sessions.length() - 1
                    );


            currentChatId =
                    lastChat.optString(
                            "id",
                            ""
                    );


            if (currentChatId.isEmpty()) {

                createNewChat();

                return;
            }


            loadChat(currentChatId);


        } catch (Exception e) {

            e.printStackTrace();

            chatPrefs.edit()
                    .remove(CHAT_SESSIONS)
                    .apply();

            createNewChat();
        }
    }


    // =========================================================
    // CREATE NEW CHAT
    // =========================================================

    private void createNewChat() {

        currentChatId =
                String.valueOf(
                        System.currentTimeMillis()
                );


        try {

            JSONArray sessions =
                    getSessions();


            JSONObject newChat =
                    new JSONObject();


            newChat.put(
                    "id",
                    currentChatId
            );

            newChat.put(
                    "title",
                    "New chat"
            );

            newChat.put(
                    "messages",
                    new JSONArray()
            );


            sessions.put(newChat);


            // Keep maximum 20 chats
            if (sessions.length() > 20) {

                JSONArray trimmed =
                        new JSONArray();

                int start =
                        sessions.length() - 20;


                for (
                        int i = start;
                        i < sessions.length();
                        i++
                ) {

                    trimmed.put(
                            sessions.getJSONObject(i)
                    );
                }

                sessions = trimmed;
            }


            saveSessions(sessions);


            if (chatContainer != null) {

                chatContainer.removeAllViews();
            }


            if (tvCurrentChatTitle != null) {

                tvCurrentChatTitle.setText(
                        "Financial Assistant"
                );
            }


            addMessageToChat(
                    "Hello! 👋 I'm WalletWise AI.\n\n" +
                            "I can help you with budgeting, " +
                            "saving money, expenses and financial planning.\n\n" +
                            "You can ask me things like:\n" +
                            "• How can I save ₹5000?\n" +
                            "• How should I manage my monthly budget?\n" +
                            "• Where am I spending too much?\n" +
                            "• Explain SIPs simply.\n\n" +
                            "What would you like to know?",
                    false
            );


            loadHistory();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // LOAD CHAT
    // =========================================================

    private void loadChat(String chatId) {

        if (chatContainer == null) {
            return;
        }

        chatContainer.removeAllViews();


        try {

            JSONArray sessions =
                    getSessions();


            for (
                    int i = 0;
                    i < sessions.length();
                    i++
            ) {

                JSONObject chat =
                        sessions.getJSONObject(i);


                if (!chat.optString(
                        "id",
                        ""
                ).equals(chatId)) {

                    continue;
                }


                String title =
                        chat.optString(
                                "title",
                                "Financial Assistant"
                        );


                if (tvCurrentChatTitle != null) {

                    tvCurrentChatTitle.setText(
                            title
                    );
                }


                JSONArray messages =
                        chat.optJSONArray(
                                "messages"
                        );


                if (messages == null ||
                        messages.length() == 0) {

                    addMessageToChat(
                            "Hello! 👋 I'm WalletWise AI.\n\n" +
                                    "How can I help you today?",
                            false
                    );

                    return;
                }


                for (
                        int j = 0;
                        j < messages.length();
                        j++
                ) {

                    JSONObject message =
                            messages.getJSONObject(j);


                    String text =
                            message.optString(
                                    "text",
                                    ""
                            );


                    boolean isUser =
                            message.optBoolean(
                                    "isUser",
                                    false
                            );


                    if (!text.isEmpty()) {

                        addMessageToChat(
                                text,
                                isUser
                        );
                    }
                }


                scrollToBottom();

                return;
            }


            createNewChat();


        } catch (Exception e) {

            e.printStackTrace();

            createNewChat();
        }
    }


    // =========================================================
    // GET SESSIONS
    // =========================================================

    private JSONArray getSessions() {

        String json =
                chatPrefs.getString(
                        CHAT_SESSIONS,
                        "[]"
                );


        try {

            return new JSONArray(json);

        } catch (Exception e) {

            return new JSONArray();
        }
    }


    // =========================================================
    // SAVE SESSIONS
    // =========================================================

    private void saveSessions(
            JSONArray sessions
    ) {

        chatPrefs.edit()
                .putString(
                        CHAT_SESSIONS,
                        sessions.toString()
                )
                .apply();
    }


    // =========================================================
    // SAVE MESSAGE
    // =========================================================

    private void saveMessage(
            String message,
            boolean isUser
    ) {

        try {

            JSONArray sessions =
                    getSessions();


            for (
                    int i = 0;
                    i < sessions.length();
                    i++
            ) {

                JSONObject chat =
                        sessions.getJSONObject(i);


                if (!chat.optString(
                        "id",
                        ""
                ).equals(currentChatId)) {

                    continue;
                }


                JSONArray messages =
                        chat.optJSONArray(
                                "messages"
                        );


                if (messages == null) {

                    messages =
                            new JSONArray();

                    chat.put(
                            "messages",
                            messages
                    );
                }


                JSONObject messageObject =
                        new JSONObject();


                messageObject.put(
                        "text",
                        message
                );


                messageObject.put(
                        "isUser",
                        isUser
                );


                messages.put(
                        messageObject
                );


                // First user message becomes title
                if (
                        isUser &&
                                "New chat".equals(
                                        chat.optString(
                                                "title",
                                                "New chat"
                                        )
                                )
                ) {

                    String title = message;


                    if (title.length() > 28) {

                        title =
                                title.substring(
                                        0,
                                        28
                                ) + "...";
                    }


                    chat.put(
                            "title",
                            title
                    );


                    if (tvCurrentChatTitle != null) {

                        tvCurrentChatTitle.setText(
                                title
                        );
                    }
                }


                break;
            }


            saveSessions(sessions);

            loadHistory();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // LOAD SIDEBAR HISTORY
    // =========================================================

    private void loadHistory() {

        if (historyContainer == null) {
            return;
        }


        historyContainer.removeAllViews();


        try {

            JSONArray sessions =
                    getSessions();


            // Newest first
            for (
                    int i = sessions.length() - 1;
                    i >= 0;
                    i--
            ) {

                JSONObject chat =
                        sessions.getJSONObject(i);


                String id =
                        chat.optString(
                                "id",
                                ""
                        );


                String title =
                        chat.optString(
                                "title",
                                "New chat"
                        );


                if (id.isEmpty()) {
                    continue;
                }


                TextView item =
                        new TextView(this);


                item.setText(title);

                item.setTextColor(
                        Color.rgb(
                                225,
                                225,
                                230
                        )
                );

                item.setTextSize(14);

                item.setPadding(
                        12,
                        14,
                        12,
                        14
                );

                item.setMaxLines(2);


                item.setOnClickListener(v -> {

                    currentChatId = id;

                    loadChat(id);

                    if (drawerLayout != null) {
                        drawerLayout.closeDrawers();
                    }
                });


                historyContainer.addView(item);
            }


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    private void sendMessage() {

        if (etUserQuery == null ||
                btnSend == null) {

            return;
        }


        String question =
                etUserQuery
                        .getText()
                        .toString()
                        .trim();


        if (question.isEmpty()) {
            return;
        }


        // User message
        addMessageToChat(
                question,
                true
        );


        saveMessage(
                question,
                true
        );


        etUserQuery.setText("");


        // Thinking message
        TextView thinking =
                addMessageToChat(
                        "Thinking... 🤔",
                        false
                );


        btnSend.setEnabled(false);


        // Ask Gemini
        askGemini(
                question,
                thinking
        );
    }


    // =========================================================
    // ASK GEMINI
    // =========================================================

    private void askGemini(
            String question,
            TextView thinkingView
    ) {

        new Thread(() -> {

            try {

                String apiKey =
                        com.example.walletwise.BuildConfig.GEMINI_API_KEY;


                if (apiKey == null ||
                        apiKey.trim().isEmpty()) {

                    runOnUiThread(() -> {

                        btnSend.setEnabled(true);

                        thinkingView.setText(
                                "Gemini API key is missing ❌"
                        );
                    });

                    return;
                }


                // =================================================
                // WALLETWISE SYSTEM PROMPT
                // =================================================

                String prompt =
                        "You are WalletWise AI, a helpful " +
                                "personal finance assistant for users in India.\n\n" +

                                "RULES:\n" +

                                "1. Use Indian Rupees (₹ / INR) by default.\n" +

                                "2. Never use dollars ($) unless the user " +
                                "specifically asks about dollars.\n" +

                                "3. Give clear, simple and practical answers.\n" +

                                "4. Help with budgeting, saving, expenses, " +
                                "financial planning and financial education.\n" +

                                "5. Use Indian examples when appropriate.\n" +

                                "6. When mentioning money amounts, use ₹.\n" +

                                "7. Keep answers reasonably concise unless " +
                                "the user asks for detailed information.\n" +

                                "8. Do not claim to be a professional financial advisor.\n\n" +

                                "User question:\n" +
                                question;


                // =================================================
                // BUILD JSON
                // =================================================

                JSONObject textPart =
                        new JSONObject();

                textPart.put(
                        "text",
                        prompt
                );


                JSONArray parts =
                        new JSONArray();

                parts.put(textPart);


                JSONObject content =
                        new JSONObject();

                content.put(
                        "parts",
                        parts
                );


                JSONArray contents =
                        new JSONArray();

                contents.put(content);


                JSONObject requestJson =
                        new JSONObject();

                requestJson.put(
                        "contents",
                        contents
                );


                // =================================================
                // GEMINI 3.5 FLASH LITE
                // =================================================

                String url =
                        "https://generativelanguage.googleapis.com/" +
                                "v1beta/models/" +
                                GEMINI_MODEL +
                                ":generateContent";


                // =================================================
                // REQUEST BODY
                // =================================================

                RequestBody body =
                        RequestBody.create(
                                requestJson.toString(),
                                JSON
                        );


                // =================================================
                // REQUEST
                // =================================================

                Request request =
                        new Request.Builder()
                                .url(url)
                                .addHeader(
                                        "Content-Type",
                                        "application/json"
                                )
                                .addHeader(
                                        "x-goog-api-key",
                                        apiKey
                                )
                                .post(body)
                                .build();


                // =================================================
                // CALL GEMINI
                // =================================================

                client.newCall(request)
                        .enqueue(
                                new Callback() {

                                    @Override
                                    public void onFailure(
                                            Call call,
                                            IOException e
                                    ) {

                                        runOnUiThread(() -> {

                                            btnSend.setEnabled(
                                                    true
                                            );


                                            String error =
                                                    e.getMessage();


                                            if (error == null ||
                                                    error.trim().isEmpty()) {

                                                error =
                                                        "Network connection failed.";
                                            }


                                            thinkingView.setText(
                                                    "Couldn't connect to Gemini 😕\n\n" +
                                                            error
                                            );
                                        });
                                    }


                                    @Override
                                    public void onResponse(
                                            Call call,
                                            Response response
                                    ) throws IOException {

                                        String responseBody =
                                                response.body() != null
                                                        ? response.body().string()
                                                        : "";


                                        // =================================================
                                        // HTTP ERROR
                                        // =================================================

                                        if (!response.isSuccessful()) {

                                            final String error =
                                                    "Gemini Error ❌\n\n" +
                                                            "HTTP " +
                                                            response.code() +
                                                            "\n\n" +
                                                            extractError(
                                                                    responseBody
                                                            );


                                            runOnUiThread(() -> {

                                                btnSend.setEnabled(
                                                        true
                                                );

                                                thinkingView.setText(
                                                        error
                                                );
                                            });

                                            return;
                                        }


                                        // =================================================
                                        // PARSE RESPONSE
                                        // =================================================

                                        try {

                                            JSONObject result =
                                                    new JSONObject(
                                                            responseBody
                                                    );


                                            JSONArray candidates =
                                                    result.optJSONArray(
                                                            "candidates"
                                                    );


                                            if (candidates == null ||
                                                    candidates.length() == 0) {

                                                runOnUiThread(() -> {

                                                    btnSend.setEnabled(
                                                            true
                                                    );

                                                    thinkingView.setText(
                                                            "Gemini didn't return an answer 😕"
                                                    );
                                                });

                                                return;
                                            }


                                            JSONObject candidate =
                                                    candidates.getJSONObject(
                                                            0
                                                    );


                                            JSONObject responseContent =
                                                    candidate.optJSONObject(
                                                            "content"
                                                    );


                                            if (responseContent == null) {

                                                runOnUiThread(() -> {

                                                    btnSend.setEnabled(
                                                            true
                                                    );

                                                    thinkingView.setText(
                                                            "Invalid Gemini response 😕"
                                                    );
                                                });

                                                return;
                                            }


                                            JSONArray responseParts =
                                                    responseContent.optJSONArray(
                                                            "parts"
                                                    );


                                            if (responseParts == null ||
                                                    responseParts.length() == 0) {

                                                runOnUiThread(() -> {

                                                    btnSend.setEnabled(
                                                            true
                                                    );

                                                    thinkingView.setText(
                                                            "Gemini returned an empty answer 😕"
                                                    );
                                                });

                                                return;
                                            }


                                            StringBuilder answerBuilder =
                                                    new StringBuilder();


                                            for (
                                                    int i = 0;
                                                    i < responseParts.length();
                                                    i++
                                            ) {

                                                JSONObject part =
                                                        responseParts.getJSONObject(
                                                                i
                                                        );


                                                String text =
                                                        part.optString(
                                                                "text",
                                                                ""
                                                        );


                                                if (!text.isEmpty()) {

                                                    answerBuilder.append(
                                                            text
                                                    );
                                                }
                                            }


                                            String answer =
                                                    answerBuilder
                                                            .toString()
                                                            .trim();


                                            if (answer.isEmpty()) {

                                                answer =
                                                        "Sorry, I couldn't generate an answer.";
                                            }


                                            final String finalAnswer =
                                                    answer;


                                            runOnUiThread(() -> {

                                                btnSend.setEnabled(
                                                        true
                                                );


                                                // Replace Thinking...
                                                thinkingView.setText(
                                                        finalAnswer
                                                );


                                                // Save AI response
                                                saveMessage(
                                                        finalAnswer,
                                                        false
                                                );


                                                scrollToBottom();
                                            });


                                        } catch (Exception e) {

                                            e.printStackTrace();


                                            runOnUiThread(() -> {

                                                btnSend.setEnabled(
                                                        true
                                                );


                                                thinkingView.setText(
                                                        "Couldn't read Gemini's response 😕"
                                                );
                                            });
                                        }
                                    }
                                }
                        );


            } catch (Exception e) {

                e.printStackTrace();


                runOnUiThread(() -> {

                    btnSend.setEnabled(true);


                    String error =
                            e.getMessage();


                    if (error == null ||
                            error.trim().isEmpty()) {

                        error =
                                "Unknown error.";
                    }


                    thinkingView.setText(
                            "Something went wrong 😕\n\n" +
                                    error
                    );
                });
            }

        }).start();
    }


    // =========================================================
    // ERROR EXTRACTION
    // =========================================================

    private String extractError(
            String responseBody
    ) {

        try {

            JSONObject root =
                    new JSONObject(responseBody);


            JSONObject error =
                    root.optJSONObject("error");


            if (error != null) {

                String message =
                        error.optString(
                                "message",
                                ""
                        );


                if (!message.isEmpty()) {

                    return message;
                }
            }

        } catch (Exception ignored) {
        }


        if (responseBody == null ||
                responseBody.trim().isEmpty()) {

            return "Unknown Gemini error.";
        }


        return responseBody;
    }


    // =========================================================
    // ADD MESSAGE TO CHAT UI
    // =========================================================

    private TextView addMessageToChat(
            String message,
            boolean isUser
    ) {

        TextView tvMessage =
                new TextView(this);


        tvMessage.setText(message);

        tvMessage.setTextSize(15);


        // =====================================================
        // MESSAGE TEXT COLOR
        // =====================================================

        if (isUser) {

            // User text — Deep Brown
            tvMessage.setTextColor(
                    Color.rgb(
                            48,
                            40,
                            32
                    )
            );

        } else {

            // AI text — Dark Warm Brown
            tvMessage.setTextColor(
                    Color.rgb(
                            63,
                            52,
                            44
                    )
            );
        }


        tvMessage.setPadding(
                28,
                18,
                28,
                18
        );


        // =====================================================
        // MESSAGE BACKGROUND
        // =====================================================

        GradientDrawable background =
                new GradientDrawable();


        background.setCornerRadius(30);


        if (isUser) {

            // User message — Sage Green
            background.setColor(
                    Color.rgb(
                            199,
                            184,
                            232
                    )
            );

        } else {

            // AI message — Warm Beige
            background.setColor(
                    Color.rgb(
                            243,
                            235,
                            221
                    )
            );
        }


        tvMessage.setBackground(background);


        // =====================================================
        // LAYOUT
        // =====================================================

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        params.setMargins(
                8,
                8,
                8,
                8
        );


        if (isUser) {

            params.gravity =
                    Gravity.END;

        } else {

            params.gravity =
                    Gravity.START;
        }


        tvMessage.setLayoutParams(params);


        if (chatContainer != null) {

            chatContainer.addView(tvMessage);
        }


        scrollToBottom();


        return tvMessage;
    }


    // =========================================================
    // SCROLL
    // =========================================================

    private void scrollToBottom() {

        if (scrollViewChat == null) {
            return;
        }


        scrollViewChat.post(() ->
                scrollViewChat.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        /*
         * Do not manually shut down OkHttp.
         */
    }
}