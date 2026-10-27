package cn.steps.simple;
import org.json.*;

public class UpdateProtocolTest {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Update check "+checks);}
    static JSONObject file(String name,String type)throws Exception{return new JSONObject().put("name",name).put("type",type);}
    public static void main(String[] args)throws Exception{
        check(UpdateProtocol.compare("1.5.3","1.5.3")==0);
        check(UpdateProtocol.compare("1.5.10","1.5.9")>0);
        check(UpdateProtocol.compare("1.10.0","1.9.99")>0);
        check(UpdateProtocol.compare("2.0.0","1.999.999")>0);
        check(UpdateProtocol.compare("1.5.2","1.5.3")<0);
        JSONArray list=new JSONArray().put(file("StepSubmit-1.5.9.apk","file")).put(file("StepSubmit-1.5.10.apk","file")).put(file("StepSubmit-1.5.2.apk","file"));
        UpdateProtocol.Release latest=UpdateProtocol.latest(list);
        check(latest.version.equals("1.5.10"));
        check(latest.name.equals("StepSubmit-1.5.10.apk"));
        check(latest.url.equals("https://raw.githubusercontent.com/lyzloved/zepp-auto-submit/main/android-app/releases/StepSubmit-1.5.10.apk"));
        check(UpdateProtocol.latest(new JSONArray())==null);
        check(UpdateProtocol.latest(new JSONArray().put(file("StepSubmit-99.0.0.apk","dir")))==null);
        for(String name:new String[]{"StepSubmit-1.6.0-beta.apk","StepSubmit-1.6.apk","../StepSubmit-9.0.0.apk","StepSubmit-1.5.3.apk.exe","other-99.0.0.apk","StepSubmit-99999999999999999.1.0.apk"})
            check(UpdateProtocol.latest(new JSONArray().put(file(name,"file")))==null);
        check(UpdateProtocol.latest(new JSONArray().put(JSONObject.NULL).put("invalid").put(file("StepSubmit-1.5.3.apk","file"))).version.equals("1.5.3"));
        System.out.println("PASS: "+checks+" update checks; synthetic only");
    }
}
