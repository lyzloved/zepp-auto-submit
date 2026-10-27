package cn.steps.simple;
import org.json.JSONObject;
public final class CardStatus {
    public static final int UNKNOWN=0, AVAILABLE=1, USED=2, INVALID=3;
    public final int state,days;
    private CardStatus(int state,int days){this.state=state;this.days=days;}
    public static CardStatus parse(JSONObject body){
        if(!"1".equals(body.optString("code","")))return new CardStatus(UNKNOWN,0);
        String s=body.optString("state","");
        int value="1".equals(s)?USED:"-1".equals(s)?INVALID:body.optBoolean("valid",false)?AVAILABLE:UNKNOWN;
        return new CardStatus(value,Math.max(0,body.optInt("days",0)));
    }
}
