package cn.steps.simple;

import org.json.JSONObject;

public final class QuoteProtocol {
    public static final class Quote {
        public final String content, source, token;
        Quote(String content, String source, String token) {
            this.content = content;
            this.source = source;
            this.token = token;
        }
    }

    public static Quote parse(JSONObject body, String fallbackToken) {
        if (!"success".equals(body.optString("status")))
            throw new IllegalArgumentException("诗词服务返回失败");
        JSONObject data = body.optJSONObject("data");
        if (data == null) throw new IllegalArgumentException("诗词内容缺失");
        String content = data.optString("content", "").trim();
        if (content.isEmpty() || content.length() > 300)
            throw new IllegalArgumentException("诗词内容无效");

        String source = "";
        JSONObject origin = data.optJSONObject("origin");
        if (origin != null) {
            String dynasty = origin.optString("dynasty", "").trim();
            String author = origin.optString("author", "").trim();
            String title = origin.optString("title", "").trim();
            StringBuilder text = new StringBuilder();
            if (!dynasty.isEmpty()) text.append(dynasty);
            if (!author.isEmpty()) {
                if (text.length() > 0) text.append(" · ");
                text.append(author);
            }
            if (!title.isEmpty()) text.append("《").append(title).append("》");
            source = text.toString();
        }
        String token = body.optString("token", "").trim();
        if (token.isEmpty()) token = fallbackToken == null ? "" : fallbackToken;
        return new Quote(content, source, token);
    }
}
