package cn.steps.simple;

import java.io.*;
import java.net.*;
import org.json.JSONObject;

final class ApiClient {
    static JSONObject submit(String account, String password, int steps) throws Exception {
        return post(StepProtocol.ENDPOINT, StepProtocol.form(account, password, steps));
    }
    static JSONObject checkMembership(String account) throws Exception {
        return post(Membership.ENDPOINT, Membership.form(account));
    }
    static JSONObject redeem(String account,String password,String card) throws Exception {
        return post(Redemption.ENDPOINT,Redemption.form(account,password,card));
    }
    static JSONObject checkCard(String card) throws Exception {
        return post("https://zepp.net.cn/ajax.php", "act=check_km&km="+java.net.URLEncoder.encode(card,"UTF-8"),10000);
    }
    static JSONObject claimCard() throws Exception {
        return post("https://zepp.net.cn/ajax.php", "act=km_get",10000);
    }
    private static JSONObject post(String endpoint,String form)throws Exception {return post(endpoint,form,45000);}
    private static JSONObject post(String endpoint, String form,int timeout) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(endpoint).openConnection();
        try {
            c.setInstanceFollowRedirects(false); // Never forward credentials to another origin.
            c.setConnectTimeout(Math.min(timeout,15000));
            c.setReadTimeout(timeout);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Origin", "https://zepp.net.cn");
            c.setRequestProperty("Referer", "https://zepp.net.cn/");
            c.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            byte[] data = form.getBytes("UTF-8");
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream out = c.getOutputStream()) { out.write(data); }
            int status = c.getResponseCode();
            if (status >= 300 && status < 400) throw new IOException("服务发生重定向，已停止发送凭据");
            InputStream input = status >= 400 ? c.getErrorStream() : c.getInputStream();
            if (input == null) throw new IOException("服务未返回内容（HTTP " + status + "）");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream stream = input) {
                byte[] bytes = new byte[4096]; int count;
                while ((count = stream.read(bytes)) != -1) {
                    if (buffer.size() + count > 65536) throw new IOException("服务返回内容过大");
                    buffer.write(bytes, 0, count);
                }
            }
            JSONObject body;
            try { body = new JSONObject(buffer.toString("UTF-8")); }
            catch (Exception e) { throw new IOException("服务未返回 JSON（HTTP " + status + "），可能需要网页验证"); }
            if (status < 200 || status >= 300) throw new IOException("HTTP " + status + "：" + StepProtocol.message(body));
            return body;
        } finally { c.disconnect(); }
    }
}
