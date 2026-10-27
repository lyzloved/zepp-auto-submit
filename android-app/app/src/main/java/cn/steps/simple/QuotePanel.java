package cn.steps.simple;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Foreground-only quote card backed by the official Jinrishici client API. */
final class QuotePanel extends LinearLayout {
    private static final long REFRESH_MS = 2L * 60L * 60L * 1000L;
    private final SharedPreferences prefs;
    private final TextView quote;
    private final TextView source;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean active, loading, destroyed;
    private final Runnable refreshTask = new Runnable() {
        public void run() { if (active && !destroyed) refreshIfNeeded(); }
    };

    QuotePanel(Context context) {
        super(context);
        prefs = context.getSharedPreferences("daily_quote_v1", Context.MODE_PRIVATE);
        setOrientation(VERTICAL);
        setPadding(dp(18), dp(16), dp(18), dp(16));
        setBackground(background(Color.rgb(247, 249, 252), 18));

        TextView title = label("每日一言", 13, Color.rgb(11, 87, 208));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addView(title);
        quote = label("山重水复疑无路，柳暗花明又一村。", 15, Color.rgb(31, 31, 31));
        quote.setLineSpacing(dp(4), 1f);
        LayoutParams quoteParams = new LayoutParams(-1, -2);
        quoteParams.topMargin = dp(10);
        addView(quote, quoteParams);
        source = label("宋 · 陆游《游山西村》", 11, Color.rgb(95, 99, 104));
        LayoutParams sourceParams = new LayoutParams(-1, -2);
        sourceParams.topMargin = dp(8);
        addView(source, sourceParams);
        showCached();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView label(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void showCached() {
        String cachedQuote = prefs.getString("content", "");
        if (!cachedQuote.isEmpty()) quote.setText(cachedQuote);
        String cachedSource = prefs.getString("source", "");
        if (!cachedSource.isEmpty()) source.setText(cachedSource);
    }

    void resume() {
        active = true;
        showCached();
        main.removeCallbacks(refreshTask);
        refreshIfNeeded();
    }

    void pause() {
        active = false;
        main.removeCallbacks(refreshTask);
    }

    void close() {
        destroyed = true;
        pause();
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }

    private void refreshIfNeeded() {
        long age = System.currentTimeMillis() - prefs.getLong("updated_at", 0L);
        if (age >= 0 && age < REFRESH_MS) {
            main.postDelayed(refreshTask, REFRESH_MS - age);
            return;
        }
        if (loading) return;
        loading = true;
        source.setText("正在更新…");
        worker.execute(() -> {
            String token = prefs.getString("token", "");
            String content = null;
            String attribution = null;
            try {
                if (token.isEmpty()) {
                    JSONObject tokenBody = get("https://v2.jinrishici.com/token", null);
                    if (!"success".equals(tokenBody.optString("status")))
                        throw new java.io.IOException("Token request failed");
                    token = tokenBody.optString("data", "").trim();
                    if (token.isEmpty()) throw new java.io.IOException("Empty token");
                }
                QuoteProtocol.Quote parsed = QuoteProtocol.parse(
                    get("https://v2.jinrishici.com/sentence", token), token);
                content = parsed.content;
                attribution = parsed.source;
                token = parsed.token;
            } catch (Exception ignored) {
                // Keep the last successfully cached quote on network or service errors.
            }
            final String newToken = token;
            final String newContent = content;
            final String newAttribution = attribution;
            main.post(() -> {
                if (destroyed) return;
                loading = false;
                if (newContent != null) {
                    SharedPreferences.Editor editor = prefs.edit()
                        .putString("token", newToken)
                        .putString("content", newContent)
                        .putLong("updated_at", System.currentTimeMillis());
                    if (newAttribution != null && !newAttribution.isEmpty())
                        editor.putString("source", newAttribution);
                    editor.apply();
                    showCached();
                } else {
                    showCached();
                }
                if (active) main.postDelayed(refreshTask, REFRESH_MS);
            });
        });
    }

    private JSONObject get(String endpoint, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty())
                connection.setRequestProperty("X-User-Token", token);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300)
                throw new java.io.IOException("HTTP " + status);
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream stream = input) {
                byte[] bytes = new byte[2048];
                int count;
                while ((count = stream.read(bytes)) != -1) {
                    if (buffer.size() + count > 65536)
                        throw new java.io.IOException("Quote response too large");
                    buffer.write(bytes, 0, count);
                }
            }
            return new JSONObject(buffer.toString("UTF-8"));
        } finally {
            connection.disconnect();
        }
    }
}
