package cn.steps.simple;
import org.json.JSONObject;
public class MembershipTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Membership check "+checks);}
    static Membership body(String code,String msg)throws Exception{return Membership.parse(new JSONObject().put("code",code).put("msg",msg));}
    public static void main(String[] args)throws Exception {
        Membership m=body("0","会员未到期:2026-10-30 12:00:00");
        check(m.expiry==1793332800000L);check(m.date.equals("2026-10-30 12:00:00"));
        check(!m.expired(m.expiry-1));check(m.expired(m.expiry));check(m.expired(m.expiry+1));
        check(m.soon(m.expiry-3*86400000L));check(!m.soon(m.expiry-3*86400000L-1));check(!m.soon(m.expiry));
        check(m.title(m.expiry).equals("卡密已过期"));check(m.title(m.expiry-1).equals("卡密即将到期"));
        check(m.title(m.expiry-4*86400000L).equals("卡密有效"));
        check(body("3","会员已过期").expired(0));check(!body("2","封禁").expired(0));
        check(body("2","").title(0).equals("账号已封禁"));
        for(String msg:new String[]{"","会员未到期:2026-02-30 12:00:00","会员未到期:2026-13-01 12:00:00","会员未到期:2026-10-30 25:00:00","会员未到期:2099-10-30"}){
            Membership bad=body("0",msg);check(bad.expiry==-1);check(!bad.expired(System.currentTimeMillis()));
        }
        check(body("0","会员未到期:2026-10-30 12:00").expiry==m.expiry);
        check(body("0","<b>会员未到期:2026-10-30 12:00:00</b>").expiry==m.expiry);
        check(body("200","成功").expiry==-1);
        check(Membership.parse(new JSONObject()).code==-1);
        check(Membership.form(" a+b@example.test ").equals("qq=a%2Bb%40example.test"));
        check(!Membership.form("a").contains("password"));
        try{Membership.form(" ");throw new AssertionError();}catch(IllegalArgumentException ok){checks++;}
        check(m.detail(m.expiry-60000).contains("0 天 0 小时 1 分钟"));
        System.out.println("PASS: "+checks+" membership checks; no network or real credentials");
    }
}
