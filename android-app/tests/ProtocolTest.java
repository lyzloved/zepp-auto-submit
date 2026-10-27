package cn.steps.simple;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.util.*;
public class ProtocolTest {
    static int checks;
    static void check(boolean ok) { checks++; if(!ok) throw new AssertionError("Check " + checks); }
    static void invalid(String a,String p,String n) {try{StepProtocol.validate(a,p,n);throw new AssertionError("Accepted invalid input");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        check(StepProtocol.validate("u@example.test","test","1")==1);
        check(StepProtocol.validate("u@example.test","test","98800")==98800);
        for(String n:new String[]{"0","98801","-1","1.5","NaN","","999999999999"})invalid("u","p",n);
        invalid("  ","p","10");invalid("u","","10");
        String pass=" +&=中文%/ ";
        Map<String,String> data=new HashMap<>();
        for(String pair:StepProtocol.form(" u+1@example.test ",pass,15570).split("&")) {
            String[] kv=pair.split("=",2);data.put(URLDecoder.decode(kv[0],"UTF-8"),URLDecoder.decode(kv[1],"UTF-8"));
        }
        check(data.get("account").equals("u+1@example.test"));check(data.get("password").equals(pass));
        check(data.get("steps").equals("15570"));check(data.get("motic").equals("0"));check(!data.containsKey("api"));
        check(StepProtocol.accepted(new JSONObject("{\"code\":200}")));
        check(StepProtocol.accepted(new JSONObject("{\"code\":\"200\"}")));
        for(String body:new String[]{"{\"code\":0}","{\"code\":1}","{\"code\":-1}","{}"})check(!StepProtocol.accepted(new JSONObject(body)));
        check(StepProtocol.message(new JSONObject("{\"msg\":\"<b>接受</b><br>请检查\"}")).equals("接受\n请检查"));
        System.out.println("PASS: "+checks+" checks (synthetic credentials only)");
    }
}
