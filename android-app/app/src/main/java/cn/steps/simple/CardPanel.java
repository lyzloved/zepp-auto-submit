package cn.steps.simple;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Private, bounded list of user-owned cards. Polling never claims cards. */
final class CardPanel extends LinearLayout {
    interface Redeemer {void open(String card);}
    private static final int LIMIT=5;
    private final LinkedHashMap<String,Integer> entries=new LinkedHashMap<>();
    private final LinkedHashMap<String,String> labels=new LinkedHashMap<>();
    private final LinearLayout rows;
    private final TextView summary;
    private final Button refresh,claim;
    private final Vault vault;
    private final Redeemer redeemer;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean checking,claiming;
    private volatile boolean active,destroyed;
    private long lastRefresh;
    private final Runnable ticker=new Runnable(){public void run(){if(!active||destroyed)return;refresh(false);main.postDelayed(this,30000);}};
    CardPanel(Context context,Redeemer redeemer){
        super(context);this.redeemer=redeemer;vault=new Vault(context,"private_cards_v1");
        setOrientation(VERTICAL);setPadding(dp(16),dp(14),dp(16),dp(12));setBackground(bg(0xfff0f4f9,20));
        LinearLayout head=new LinearLayout(context);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("我的卡密",14,0xff1f1f1f);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        refresh=button("刷新");head.addView(refresh,new LinearLayout.LayoutParams(dp(68),dp(48)));addView(head);
        summary=text("最多 5 张 · 前台每 30 秒校验",11,0xff5f6368);addView(summary);
        rows=new LinearLayout(context);rows.setOrientation(VERTICAL);addView(rows,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(context);
        Button add=button("添加已有卡密");actions.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));
        claim=button("领取网站卡密");actions.addView(claim,new LinearLayout.LayoutParams(0,dp(48),1));addView(actions);
        addView(text("按网站规则领取；仅展示你领取或添加的卡密。",11,0xff5f6368));
        add.setOnClickListener(v->showImport());refresh.setOnClickListener(v->refresh(true));claim.setOnClickListener(v->confirmClaim());
        try{JSONObject data=vault.load();if(data!=null){JSONArray a=data.optJSONArray("cards");if(a!=null)for(int i=0;i<a.length()&&entries.size()<LIMIT;i++){String key=Redemption.normalize(a.optString(i));if(!key.isEmpty()){entries.put(key,CardStatus.UNKNOWN);labels.put(key,"待校验");}}}}
        catch(Exception e){summary.setText("本机卡密读取失败，请重新添加。");}
        render();
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable b=new GradientDrawable();b.setColor(c);b.setCornerRadius(dp(r));return b;}
    private TextView text(String s,int size,int c){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(c);return t;}
    private Button button(String s){Button b=new Button(getContext());b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(0xff0b57d0);b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(dp(4),0,dp(4),0);return b;}
    private void save(){try{JSONArray a=new JSONArray();for(String key:entries.keySet())if(entries.get(key)!=CardStatus.USED)a.put(key);vault.saveObject(new JSONObject().put("cards",a));}catch(Exception e){summary.setText("卡密仅暂存在当前页面，保存失败。");}}
    private void render(){
        if(destroyed)return;rows.removeAllViews();
        if(entries.isEmpty()){TextView empty=text("暂无卡密，领取或添加后显示网站真实状态。",12,0xff5f6368);empty.setPadding(0,dp(12),0,dp(8));rows.addView(empty);}
        for(String key:new ArrayList<>(entries.keySet())){
            int state=entries.get(key);LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(6),dp(4),dp(6));row.setBackground(bg(Color.WHITE,14));
            LinearLayout content=new LinearLayout(getContext());content.setOrientation(VERTICAL);
            TextView code=text(key,15,0xff1f1f1f);code.setTextIsSelectable(true);content.addView(code);
            content.addView(text(labels.containsKey(key)?labels.get(key):"待校验",11,state==CardStatus.USED?0xffb3261e:state==CardStatus.AVAILABLE?0xff188038:0xff5f6368));
            row.addView(content,new LinearLayout.LayoutParams(0,-2,1));
            Button use=button(state==CardStatus.USED?"已使用":"兑换");use.setEnabled(state==CardStatus.AVAILABLE);use.setBackground(bg(0xffdce8ff,18));use.setMinHeight(0);use.setMinimumHeight(0);
            FrameLayout touch=new FrameLayout(getContext());FrameLayout.LayoutParams chip=new FrameLayout.LayoutParams(dp(62),dp(34),Gravity.CENTER);touch.addView(use,chip);
            touch.setOnClickListener(v->{if(entries.containsKey(key)&&entries.get(key)==CardStatus.AVAILABLE)redeemer.open(key);});use.setOnClickListener(v->redeemer.open(key));
            row.addView(touch,new LinearLayout.LayoutParams(dp(70),dp(48)));
            row.setOnLongClickListener(v->{new AlertDialog.Builder(getContext()).setTitle("从本机移除此卡密？").setMessage("仅删除本机列表记录。").setNegativeButton("取消",null).setPositiveButton("移除",(d,w)->{entries.remove(key);labels.remove(key);save();render();}).show();return true;});
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(8);rows.addView(row,rp);
        }
        claim.setEnabled(!claiming&&!checking&&entries.size()<LIMIT);refresh.setEnabled(!checking&&!claiming);
    }
    private void showImport(){
        EditText input=new EditText(getContext());input.setHint("每行一张，最多 5 张");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setMinLines(2);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4096)});
        if(Build.VERSION.SDK_INT>=26)input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle("添加已有卡密").setView(input).setNegativeButton("取消",null).setPositiveButton("添加",null).create();
        dialog.setOnDismissListener(d->input.setText(""));dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            LinkedHashSet<String> keys=new LinkedHashSet<>();for(String line:input.getText().toString().split("\\r?\\n")){String key=Redemption.normalize(line);if(!key.isEmpty())keys.add(key);}
            int extra=0;for(String key:keys)if(!entries.containsKey(key))extra++;
            if(keys.isEmpty()||extra+entries.size()>LIMIT){input.setError("请输入卡密，列表合计最多 5 张");return;}
            for(String key:keys)if(!entries.containsKey(key)){entries.put(key,CardStatus.UNKNOWN);labels.put(key,"待校验");}
            save();render();dialog.dismiss();refresh(true);
        }));dialog.show();
    }
    private void confirmClaim(){
        if(claiming||checking||entries.size()>=LIMIT)return;
        new AlertDialog.Builder(getContext()).setTitle("领取网站卡密").setMessage("按原网站规则领取一张。网站目前提示每人 30 天限领一张；点击后会消耗本次领取机会。").setNegativeButton("取消",null).setPositiveButton("领取一张",(d,w)->claim()).show();
    }
    private void claim(){
        if(claiming||destroyed||entries.size()>=LIMIT)return;claiming=true;render();summary.setText("正在领取一张，请稍候…");
        worker.execute(()->{
            JSONObject body=null;try{body=ApiClient.claimCard();}catch(Exception ignored){}
            final JSONObject response=body;main.post(()->{
                if(destroyed)return;claiming=false;
                if(response!=null&&"1".equals(response.optString("code",""))&&!response.optString("km","").trim().isEmpty()){
                    String key=Redemption.normalize(response.optString("km"));if(entries.size()<LIMIT||entries.containsKey(key)){entries.put(key,CardStatus.UNKNOWN);labels.put(key,"领取成功，待校验");save();summary.setText("网站领取成功，正在核对真实状态。");}
                    else new AlertDialog.Builder(getContext()).setTitle("领取成功，列表已满").setMessage("请保存本次卡密："+key).setPositiveButton("知道了",null).show();
                }else summary.setText(response==null?"领取结果不确定，请到原网站核实；未自动重试。":StepProtocol.message(response));
                render();refresh(true);
            });
        });
    }
    void refresh(boolean force){
        if(!active||destroyed||checking||claiming||entries.isEmpty()||(!force&&SystemClock.elapsedRealtime()-lastRefresh<30000))return;
        checking=true;lastRefresh=SystemClock.elapsedRealtime();render();summary.setText("正在校验 "+entries.size()+" 张卡密…");
        final ArrayList<String> snapshot=new ArrayList<>(entries.keySet());
        worker.execute(()->{
            for(String key:snapshot){
                if(destroyed||!active)break;CardStatus status=null;try{status=CardStatus.parse(ApiClient.checkCard(key));}catch(Exception ignored){}
                final CardStatus value=status;main.post(()->{
                    if(destroyed||!entries.containsKey(key)||entries.get(key)==CardStatus.USED)return;
                    int state=value==null?CardStatus.UNKNOWN:value.state;entries.put(key,state);
                    labels.put(key,state==CardStatus.USED?"已使用 · 即将从本机列表移除":state==CardStatus.AVAILABLE?"网站确认可用"+(value.days>0?" · "+value.days+" 天":""):state==CardStatus.INVALID?"已作废 · 长按可移除":"校验失败或状态不明，保留记录");
                    render();if(state==CardStatus.USED){save();main.postDelayed(()->{if(destroyed)return;entries.remove(key);labels.remove(key);save();render();},2500);}
                });
            }
            main.post(()->{if(destroyed)return;checking=false;summary.setText("最多 5 张 · "+new java.text.SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+" 已检查");render();});
        });
    }
    void resume(){active=true;main.removeCallbacks(ticker);refresh(true);main.postDelayed(ticker,30000);}
    void pause(){active=false;main.removeCallbacks(ticker);}
    void close(){destroyed=true;pause();main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
