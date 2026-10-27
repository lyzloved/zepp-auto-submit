package cn.steps.simple;

import java.util.regex.*;
import org.json.*;

public final class UpdateProtocol {
    public static final String INDEX="https://api.github.com/repos/lyzloved/zepp-auto-submit/contents/android-app/releases?ref=main";
    private static final Pattern APK=Pattern.compile("StepSubmit-(\\d{1,5}\\.\\d{1,5}\\.\\d{1,5})\\.apk");
    public static final class Release {
        public final String name,version,url;
        Release(String name,String version){this.name=name;this.version=version;this.url="https://raw.githubusercontent.com/lyzloved/zepp-auto-submit/main/android-app/releases/"+name;}
    }
    public static int compare(String left,String right){
        String[] a=left.split("\\."),b=right.split("\\.");
        if(a.length!=3||b.length!=3)throw new IllegalArgumentException("版本格式不正确");
        for(int i=0;i<3;i++){int x=Integer.parseInt(a[i]),y=Integer.parseInt(b[i]);if(x!=y)return Integer.compare(x,y);}
        return 0;
    }
    public static Release latest(JSONArray files){
        Release latest=null;
        for(int i=0;i<files.length();i++){
            JSONObject file=files.optJSONObject(i);if(file==null||!"file".equals(file.optString("type")))continue;
            String name=file.optString("name","");Matcher match=APK.matcher(name);
            if(!match.matches())continue;
            Release candidate=new Release(name,match.group(1));
            if(latest==null||compare(candidate.version,latest.version)>0)latest=candidate;
        }
        return latest;
    }
}
