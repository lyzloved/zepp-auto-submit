package cn.steps.simple;
import java.net.URLEncoder;
import org.json.JSONObject;

/** Public webpage kmsq contract. A click consumes only the supplied card. */
public final class Redemption {
    public static final String ENDPOINT="https://zepp.net.cn/ajax.php?act=kmsq";
    public static String normalize(String card){return card.replaceAll("\\s+|\\([^)]*\\)", "");}
    public static String form(String account,String password,String card)throws Exception {
        if(account.trim().isEmpty())throw new IllegalArgumentException("请先填写 Zepp 账号");
        if(password.isEmpty())throw new IllegalArgumentException("请先填写 Zepp 密码");
        String normalized=normalize(card);
        if(normalized.isEmpty())throw new IllegalArgumentException("请输入有效卡密");
        return "qq="+URLEncoder.encode(account.trim(),"UTF-8")+"&pwd="+URLEncoder.encode(password,"UTF-8")+"&km="+URLEncoder.encode(normalized,"UTF-8");
    }
    public static boolean accepted(JSONObject body){return "0".equals(body.optString("code",""));}
}
