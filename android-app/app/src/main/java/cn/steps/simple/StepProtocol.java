package cn.steps.simple;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;

public final class StepProtocol {
    public static final String ENDPOINT = "https://zepp.net.cn/ajax.php?act=yd";
    public static int validate(String account, String password, String steps) {
        if (account.trim().isEmpty()) throw new IllegalArgumentException("请输入 Zepp 账号");
        if (password.isEmpty()) throw new IllegalArgumentException("请输入 Zepp 密码");
        int n;
        try { n = Integer.parseInt(steps.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("步数必须为 1–98800 的整数"); }
        if (n < 1 || n > 98800) throw new IllegalArgumentException("步数必须为 1–98800 的整数");
        return n;
    }
    private static String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }
    public static String form(String account, String password, int steps) throws UnsupportedEncodingException {
        validate(account, password, Integer.toString(steps));
        // apiSelect is commented out in the current site; jQuery omits undefined api.
        return "account=" + encode(account.trim()) + "&password=" + encode(password)
            + "&steps=" + steps + "&motic=0";
    }
    public static boolean accepted(JSONObject body) {
        return "200".equals(body.optString("code", ""));
    }
    public static String message(JSONObject body) {
        String s = body.optString("msg", "");
        if (s.trim().isEmpty()) return "服务未返回说明，请稍后检查微信运动";
        // Render as plain text only, never execute server markup.
        return s.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]*>", "");
    }
}
