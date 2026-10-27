package cn.steps.simple;

import org.json.JSONObject;

public class QuoteProtocolTest {
    static int checks;
    static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Quote check " + checks);
    }
    static void invalid(String json) throws Exception {
        try {
            QuoteProtocol.parse(new JSONObject(json), "fallback");
            throw new AssertionError("Invalid quote accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
    public static void main(String[] args) throws Exception {
        String body = "{\"status\":\"success\",\"token\":\"new-token\",\"data\":{" +
            "\"content\":\"君问归期未有期。\",\"origin\":{" +
            "\"dynasty\":\"唐\",\"author\":\"李商隐\",\"title\":\"夜雨寄北\"}}}";
        QuoteProtocol.Quote quote = QuoteProtocol.parse(new JSONObject(body), "old-token");
        check(quote.content.equals("君问归期未有期。"));
        check(quote.source.equals("唐 · 李商隐《夜雨寄北》"));
        check(quote.token.equals("new-token"));
        QuoteProtocol.Quote fallback = QuoteProtocol.parse(
            new JSONObject("{\"status\":\"success\",\"data\":{\"content\":\"测试诗句\"}}"),
            "old-token");
        check(fallback.source.isEmpty());
        check(fallback.token.equals("old-token"));
        invalid("{}");
        invalid("{\"status\":\"error\"}");
        invalid("{\"status\":\"success\",\"data\":{}}");
        invalid("{\"status\":\"success\",\"data\":{\"content\":\"   \"}}");
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 301; i++) longText.append('字');
        invalid(new JSONObject().put("status", "success")
            .put("data", new JSONObject().put("content", longText.toString())).toString());
        System.out.println("PASS: " + checks + " quote checks; synthetic only");
    }
}
