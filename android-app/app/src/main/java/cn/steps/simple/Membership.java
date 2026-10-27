package cn.steps.simple;

import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/** Mirrors the public webpage's getExpireDate/check_qq contract. */
public final class Membership {
    public static final String ENDPOINT="https://zepp.net.cn/ajax.php?act=check_qq";
    public final int code;
    public final long expiry;
    public final String date;
    private Membership(int c,long e,String d){code=c;expiry=e;date=d;}
    public static String form(String account)throws Exception {
        if(account.trim().isEmpty())throw new IllegalArgumentException("请输入账号");
        return "qq="+URLEncoder.encode(account.trim(),"UTF-8");
    }
    public static Membership parse(JSONObject body) {
        String raw=body.optString("code","");
        int c="0".equals(raw)?0:"2".equals(raw)?2:"3".equals(raw)?3:-1;
        String date="";long time=-1;
        if(c==0){
            Matcher m=Pattern.compile("会员未到期[:：]\\s*(\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}(?::\\d{2})?)(?![\\d:])").matcher(StepProtocol.message(body));
            if(m.find()){
                date=m.group(1);
                String pattern=date.split(":").length==3?"yyyy-M-d H:mm:ss":"yyyy-M-d H:mm";
                SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.CHINA);
                f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));f.setLenient(false);
                ParsePosition pos=new ParsePosition(0);Date d=f.parse(date,pos);
                if(d!=null && pos.getIndex()==date.length())time=d.getTime();else date="";
            }
        }
        return new Membership(c,time,date);
    }
    public boolean expired(long now){return code==3||(code==0&&expiry>=0&&now>=expiry);}
    public boolean soon(long now){return code==0&&expiry>now&&expiry-now<=3*86400000L;}
    public String title(long now){
        if(code==2)return "账号已封禁";
        if(expired(now))return "卡密已过期";
        if(soon(now))return "卡密即将到期";
        if(code==0&&expiry>=0)return "卡密有效";
        return "有效期暂无法确认";
    }
    public String detail(long now){
        if(code==2)return "网站返回账号封禁状态，请联系服务方核实。";
        if(code==3)return "网站返回会员已过期；未提供具体到期时间。请通过原网站正规续期。";
        if(code!=0||expiry<0)return "网站未返回可识别的到期时间，请稍后重试或到原网站核实。";
        String result="到期时间："+date+"（北京时间）";
        if(expired(now))return result+"\n当前已过期，请通过原网站正规续期。";
        long minutes=Math.max(1,(expiry-now+59999)/60000);
        long days=minutes/1440,hours=minutes%1440/60,mins=minutes%60;
        return result+"\n剩余 "+days+" 天 "+hours+" 小时 "+mins+" 分钟"+(soon(now)?"，请及时续期。":"。");
    }
}
