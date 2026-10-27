package cn.steps.simple;
import org.json.JSONObject;
public class CardStatusTest {
    static int checks;
    static void check(boolean b){checks++;if(!b)throw new AssertionError("Card status check "+checks);}
    static CardStatus parse(String json)throws Exception{return CardStatus.parse(new JSONObject(json));}
    public static void main(String[] args)throws Exception{
        check(parse("{\"code\":1,\"valid\":true,\"state\":2,\"days\":30}").state==CardStatus.AVAILABLE);
        check(parse("{\"code\":1,\"valid\":true,\"state\":2,\"days\":30}").days==30);
        check(parse("{\"code\":1,\"valid\":false,\"state\":1}").state==CardStatus.USED);
        check(parse("{\"code\":\"1\",\"state\":\"1\"}").state==CardStatus.USED);
        check(parse("{\"code\":1,\"valid\":true,\"state\":1}").state==CardStatus.USED);
        check(parse("{\"code\":1,\"state\":-1}").state==CardStatus.INVALID);
        check(parse("{\"code\":1,\"state\":2,\"valid\":false}").state==CardStatus.UNKNOWN);
        check(parse("{\"code\":-1,\"state\":1}").state==CardStatus.UNKNOWN);
        check(parse("{\"code\":200,\"valid\":true}").state==CardStatus.UNKNOWN);
        check(parse("{}").state==CardStatus.UNKNOWN);
        check(parse("{\"code\":1,\"valid\":true,\"days\":-2}").days==0);
        System.out.println("PASS: "+checks+" card-state checks; synthetic only");
    }
}
