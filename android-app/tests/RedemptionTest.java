package cn.steps.simple;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.util.*;
public class RedemptionTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Redemption check "+checks);}
    static void invalid(String user,String pass,String card)throws Exception {try{Redemption.form(user,pass,card);throw new AssertionError("Invalid data accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        check(Redemption.normalize(" ABC-123 (月卡) \n DEF ").equals("ABC-123DEF"));
        check(Redemption.normalize(" +&=%中文 ").equals("+&=%中文"));
        Map<String,String> fields=new HashMap<>();
        for(String pair:Redemption.form(" u+1@example.test "," +&=密码 "," ABC (年卡) 12 ").split("&")){
            String[] kv=pair.split("=",2);fields.put(kv[0],URLDecoder.decode(kv[1],"UTF-8"));
        }
        check(fields.size()==3);check(fields.get("qq").equals("u+1@example.test"));
        check(fields.get("pwd").equals(" +&=密码 "));check(fields.get("km").equals("ABC12"));
        invalid(" ","pass","ABC");invalid("user","","ABC");invalid("user","pass"," (月卡) ");
        check(Redemption.accepted(new JSONObject("{\"code\":0}")));
        check(Redemption.accepted(new JSONObject("{\"code\":\"0\"}")));
        for(String value:new String[]{"200","1","2","-1"})check(!Redemption.accepted(new JSONObject().put("code",value)));
        check(!Redemption.accepted(new JSONObject()));
        check(Redemption.ENDPOINT.equals("https://zepp.net.cn/ajax.php?act=kmsq"));
        System.out.println("PASS: "+checks+" redemption checks; no network or real cards");
    }
}
