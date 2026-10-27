package cn.steps.simple;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private final int blue = Color.rgb(11,87,208), ink = Color.rgb(31,31,31), muted = Color.rgb(95,99,104);
    private EditText account, password, steps;
    private ImageButton eye;
    private SeekBar stepSlider;
    private TextView selectedSteps,remainingSteps;
    private boolean passwordVisible=false;
    private TextView result, resultTitle;
    private SubmitIndicator submitIndicator;
    private CardPanel cardPanel;
    private QuotePanel quotePanel;
    private Button submit, clear;
    private CheckBox remember;
    private Vault vault;
    private TextView membershipTitle, membershipDetail;
    private Button checkExpiry;
    private boolean checkingExpiry;
    private String checkedAccount = "", alerted = "";
    private Membership membership;
    private long lastCheck;
    private final Runnable expiryTick = new Runnable() {
        public void run() { renderMembership(); main.postDelayed(this, 60000); }
    };
    private boolean busy = false;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void add(LinearLayout parent, View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(top); parent.addView(v, p);
    }
    private EditText input(String hint, int type) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(16); e.setTextColor(ink);
        e.setHintTextColor(muted); e.setHint(hint); e.setInputType(type); e.setPadding(dp(16),dp(12),dp(16),dp(12));
        e.setBackground(bg(Color.rgb(241,243,244),12)); e.setMinHeight(dp(54));
        if (Build.VERSION.SDK_INT >= 26) e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return e;
    }
    private void label(LinearLayout l, String s) { add(l, text(s,13,muted,true),18); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(255,255,255));
        getWindow().setNavigationBarColor(Color.rgb(255,255,255));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        vault = new Vault(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(255,255,255));
        LinearLayout root = column(); root.setPadding(dp(24),dp(24),dp(24),dp(20)); scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(scroll);
        add(root,text("步数助手",14,blue,true),0);
        add(root,text("轻松同步每一步",26,ink,false),12);
        add(root,text("设置目标步数，点击即可提交",13,muted,false),6);
        LinearLayout card = column(); card.setPadding(0,0,0,0); add(root,card,12);
        label(card,"Zepp 账号"); account = input("邮箱 / 手机号",InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); add(card,account,8);
        label(card,"密码");
        FrameLayout passrow = new FrameLayout(this);
        passrow.setBackground(bg(Color.rgb(241,243,244),12));
        password = input("请输入密码",InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setBackgroundColor(Color.TRANSPARENT);
        password.setPadding(dp(16),dp(12),dp(60),dp(12));
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passrow.addView(password,new FrameLayout.LayoutParams(-1,-2));
        eye=new ImageButton(this);eye.setImageDrawable(new EyeDrawable(false));
        eye.setContentDescription("显示密码");eye.setPadding(dp(12),dp(12),dp(12),dp(12));
        eye.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.rgb(218,225,235)),null,bg(Color.WHITE,24)));
        FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END|Gravity.CENTER_VERTICAL);
        ep.rightMargin=dp(4);passrow.addView(eye,ep);
        eye.setOnClickListener(v->setPasswordVisible(!passwordVisible)); add(card,passrow,8);
        label(card,"目标步数"); steps = input("1–98800",InputType.TYPE_CLASS_NUMBER); steps.setTextSize(34); steps.setTypeface(Typeface.DEFAULT,Typeface.BOLD); steps.setTextColor(blue);
        steps.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)}); add(card,steps,8);
        stepSlider=new SeekBar(this);stepSlider.setMax(98799);stepSlider.setMinHeight(dp(56));
        stepSlider.setContentDescription("拖动选择目标步数，范围 1 至 98800 步");
        stepSlider.setProgressDrawable(new StepTrackDrawable(getResources().getDisplayMetrics().density));
        stepSlider.setProgressTintList(null);stepSlider.setSplitTrack(false);stepSlider.setPadding(dp(16),0,dp(16),0);
        GradientDrawable thumb=new GradientDrawable();thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(Color.WHITE);thumb.setStroke(dp(4),blue);thumb.setSize(dp(28),dp(28));
        stepSlider.setThumb(thumb);stepSlider.setThumbTintList(null);stepSlider.setThumbOffset(dp(14));
        add(card,stepSlider,4);
        LinearLayout stepRange=new LinearLayout(this);
        selectedSteps=text("当前 1 步",12,blue,true);remainingSteps=text("还差 98,799 步",12,muted,false);
        selectedSteps.setGravity(Gravity.CENTER_VERTICAL);remainingSteps.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        selectedSteps.setMinHeight(dp(40));remainingSteps.setMinHeight(dp(40));
        selectedSteps.setContentDescription("点击选择最小值 1 步");remainingSteps.setContentDescription("点击选择最大值 98800 步");
        selectedSteps.setOnClickListener(v->{if(!busy)steps.setText("1");});
        remainingSteps.setOnClickListener(v->{if(!busy)steps.setText("98800");});
        stepRange.addView(selectedSteps,new LinearLayout.LayoutParams(0,-2,1));
        stepRange.addView(remainingSteps,new LinearLayout.LayoutParams(0,-2,1));add(card,stepRange,0);
        stepSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                updateStepLabels(progress+1);
                if(fromUser){steps.setText(Integer.toString(progress+1));steps.setSelection(steps.length());}
            }
            public void onStartTrackingTouch(SeekBar bar){
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(steps.getWindowToken(),0);
                steps.clearFocus();
            }
            public void onStopTrackingTouch(SeekBar bar){}
        });
        steps.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence value,int start,int count,int after){}
            public void onTextChanged(CharSequence value,int start,int before,int count){
                try{int n=Integer.parseInt(value.toString());if(n>=1&&n<=98800){stepSlider.setProgress(n-1);updateStepLabels(n);}}
                catch(NumberFormatException ignored){}
            }
            public void afterTextChanged(Editable value){}
        });
        remember = new CheckBox(this); remember.setText("加密记住账号和密码");remember.setTextSize(13);remember.setTextColor(muted);add(card,remember,14);
        remember.setOnCheckedChangeListener((v,checked)->{if(!checked)vault.clear();});
        submit = new Button(this);submit.setText("立即提交");submit.setTextSize(16);submit.setTextColor(Color.WHITE);submit.setAllCaps(false);submit.setBackground(bg(blue,28));submit.setMinHeight(dp(56));add(card,submit,12);submit.setOnClickListener(v->startSubmit());
        cardPanel=new CardPanel(this,key->showRedemption(key));add(card,cardPanel,14);
        TextView note=text("由 zepp.net.cn 提供提交服务，账号密码将发送至该网站。",11,muted,false);note.setLineSpacing(dp(3),1);add(card,note,14);
        LinearLayout membershipCard = column();
        membershipCard.setBackground(bg(Color.rgb(240,244,249),20));
        membershipCard.setPadding(dp(18),dp(16),dp(18),dp(12));
        add(root,membershipCard,20);
        membershipTitle=text("卡密有效期",15,ink,true);
        membershipDetail=text("查询账号已兑换的会员权益，到期检查无需密码。",13,muted,false);
        membershipDetail.setLineSpacing(dp(3),1);
        checkExpiry=new Button(this); checkExpiry.setText("重新检查"); checkExpiry.setTextColor(blue);
        checkExpiry.setTextSize(12); checkExpiry.setAllCaps(false);
        checkExpiry.setBackground(bg(Color.rgb(220,232,255),18));checkExpiry.setMinHeight(0);checkExpiry.setMinimumHeight(0);checkExpiry.setPadding(dp(10),0,dp(10),0);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout checkTouch=new FrameLayout(this);
        checkTouch.addView(checkExpiry,new FrameLayout.LayoutParams(dp(90),dp(34),Gravity.CENTER));
        checkTouch.setOnClickListener(v->checkMembership(true));
        actions.addView(membershipTitle,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(checkTouch,new LinearLayout.LayoutParams(dp(100),dp(48)));
        checkExpiry.setOnClickListener(v->checkMembership(true));
        add(membershipCard,actions,0);add(membershipCard,membershipDetail,6);
        account.setOnFocusChangeListener((v,hasFocus)->{if(!hasFocus)checkMembership(false);});
        account.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){
                membership=null; checkedAccount=""; lastCheck=0; renderMembership();
            }
            public void afterTextChanged(Editable e){}
        });
        LinearLayout status = column();status.setPadding(0,0,0,0);add(root,status,18);
        resultTitle=text("等待提交",14,ink,true);result=text("首次填写账号密码后，点击“立即提交”。",13,muted,false);result.setLineSpacing(dp(4),1);LinearLayout statusHeader=new LinearLayout(this);statusHeader.setGravity(Gravity.CENTER_VERTICAL);
        submitIndicator=new SubmitIndicator(this);submitIndicator.setVisibility(View.GONE);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(28),dp(28));ip.rightMargin=dp(10);
        statusHeader.addView(submitIndicator,ip);statusHeader.addView(resultTitle,new LinearLayout.LayoutParams(0,-2,1));
        add(status,statusHeader,0);add(status,result,6);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(root,text("需提前在 Zepp 中绑定微信运动。目标步数小于当前步数时，可能不会同步。",12,muted,false),18);
        clear=new Button(this);clear.setText("清除保存的账号");clear.setTextSize(12);clear.setTextColor(muted);clear.setBackgroundColor(Color.TRANSPARENT);add(root,clear,8);
        clear.setOnClickListener(v->{submitIndicator.pause();submitIndicator.setVisibility(View.GONE);vault.clear();remember.setChecked(false);account.setText("");password.setText("");resultTitle.setText("已清除");result.setText("本机保存的账号密码已删除。");});
        quotePanel=new QuotePanel(this);add(root,quotePanel,20);
        View space = new View(this); root.addView(space,new LinearLayout.LayoutParams(1,0,1));
        LinearLayout versionRow=new LinearLayout(this);versionRow.setGravity(Gravity.CENTER);
        String currentVersion;
        try{currentVersion=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(android.content.pm.PackageManager.NameNotFoundException e){currentVersion="未知";}
        TextView versionLabel=text("当前版本 v"+currentVersion,12,muted,false);
        versionRow.addView(versionLabel,new LinearLayout.LayoutParams(-2,-2));
        Button updateButton=new Button(this);updateButton.setText("版本更新");updateButton.setTextSize(12);updateButton.setTextColor(blue);updateButton.setAllCaps(false);
        updateButton.setMinHeight(0);updateButton.setMinimumHeight(0);updateButton.setMinWidth(0);updateButton.setMinimumWidth(0);
        updateButton.setPadding(dp(14),0,dp(14),0);updateButton.setBackground(bg(Color.rgb(240,244,249),20));
        updateButton.setContentDescription("前往 GitHub 仓库下载最新版本");
        FrameLayout updateTouch=new FrameLayout(this);
        updateTouch.addView(updateButton,new FrameLayout.LayoutParams(dp(90),dp(36),Gravity.CENTER));
        LinearLayout.LayoutParams updateParams=new LinearLayout.LayoutParams(dp(102),dp(48));updateParams.leftMargin=dp(8);versionRow.addView(updateTouch,updateParams);
        View.OnClickListener openUpdates=v->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/lyzloved/zepp-auto-submit/tree/main/android-app/releases")));}
            catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"未找到浏览器，请安装浏览器后重试。",Toast.LENGTH_LONG).show();}
        };
        updateButton.setOnClickListener(openUpdates);updateTouch.setOnClickListener(openUpdates);
        add(root,versionRow,16);
        TextView developer=text("开发人员：π",12,muted,false);
        developer.setGravity(Gravity.CENTER); add(root,developer,4);
        steps.setText(Integer.toString(getPreferences(MODE_PRIVATE).getInt("steps",15570)));
        try { JSONObject saved=vault.load();if(saved!=null){account.setText(saved.optString("account"));password.setText(saved.optString("password"));remember.setChecked(true);} }
        catch(Exception e){vault.clear();result.setText("保存的信息无法解密，请重新输入。");}
    }
    private void updateStepLabels(int value){
        selectedSteps.setText(String.format(Locale.CHINA,"当前 %,d 步",value));
        remainingSteps.setText(value==98800?"已达 98,800 步":String.format(Locale.CHINA,"还差 %,d 步",98800-value));
        remainingSteps.setTextColor(value==98800?blue:muted);
        selectedSteps.setTypeface(Typeface.DEFAULT,value==1?Typeface.BOLD:Typeface.NORMAL);
    }
    private void setPasswordVisible(boolean visible) {
        passwordVisible=visible;
        int start=password.getSelectionStart(),end=password.getSelectionEnd();
        password.setTransformationMethod(visible?null:PasswordTransformationMethod.getInstance());
        if(start>=0&&end>=0)password.setSelection(Math.min(start,password.length()),Math.min(end,password.length()));
        eye.setImageDrawable(new EyeDrawable(visible));eye.setContentDescription(visible?"隐藏密码":"显示密码");
    }
    private void showRedemption(String initialCard) {
        if(busy)return;
        final String user=account.getText().toString().trim(),pass=password.getText().toString();
        if(user.isEmpty()||pass.isEmpty()){
            resultTitle.setText("请先填写账号密码");result.setText("兑换使用上方 Zepp 账号和密码，请确认账号后再兑换。");return;
        }
        LinearLayout content=column();content.setPadding(dp(24),dp(8),dp(24),dp(8));
        TextView explanation=text("兑换至："+user+"\n卡密与账号密码将发送至 zepp.net.cn。",13,muted,false);
        explanation.setLineSpacing(dp(3),1);add(content,explanation,0);
        EditText card=input("输入或粘贴卡密",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.setTransformationMethod(PasswordTransformationMethod.getInstance());card.setText(initialCard);
        card.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});add(content,card,18);
        add(content,text("点击兑换后自动刷新有效期。卡密仅用于本次请求。",12,muted,false),12);
        TextView dialogStatus=text("",13,muted,false);dialogStatus.setVisibility(View.GONE);add(content,dialogStatus,12);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("兑换卡密").setView(content)
            .setNegativeButton("取消",null).setPositiveButton("立即兑换",null).create();
        dialog.setOnDismissListener(d->card.setText(""));
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            final String key=Redemption.normalize(card.getText().toString());
            try{Redemption.form(user,pass,key);}catch(Exception e){card.setError(e.getMessage());return;}
            InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(card.getWindowToken(),0);
            dialogStatus.setVisibility(View.GONE);
            submitIndicator.pause();submitIndicator.setVisibility(View.GONE);
            setBusy(true);submit.setText("正在兑换…");card.setEnabled(false);dialog.setCancelable(false);dialog.setCanceledOnTouchOutside(false);
            Button positive=dialog.getButton(AlertDialog.BUTTON_POSITIVE);positive.setEnabled(false);positive.setText("正在兑换…");
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            worker.execute(()->{
                boolean success=false;String detail;
                try{JSONObject response=ApiClient.redeem(user,pass,key);success=Redemption.accepted(response);
                    detail=response.optString("msg","").trim().isEmpty()?(success?"兑换成功，正在刷新有效期。":"服务未返回错误说明，请核实有效期。"):redact(StepProtocol.message(response).replace(key,"[卡密已隐藏]"),user,pass);}
                catch(SocketTimeoutException e){detail="兑换结果暂不确定。请先检查有效期或到原网站核实，再决定是否重试。";}
                catch(Exception e){detail="兑换连接或响应异常。请先检查有效期，确认结果后再决定是否重试。";}
                final boolean ok=success;final String message=detail;
                main.post(()->{
                    if(isFinishing()||isDestroyed())return;
                    setBusy(false);resultTitle.setText(ok?"兑换成功":"兑换未确认成功");result.setText(message);
                    if(ok){dialog.dismiss();cardPanel.refresh(true);checkedAccount="";lastCheck=0;checkMembership(true);}
                    else{card.setEnabled(true);dialog.setCancelable(true);dialog.setCanceledOnTouchOutside(true);
                        positive.setEnabled(true);positive.setText("重试兑换");dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        dialogStatus.setText(message);dialogStatus.setVisibility(View.VISIBLE);}
                });
            });
        }));
        dialog.show();
    }
    private void setBusy(boolean active) {
        busy=active;submit.setEnabled(!active);submit.setText(active?"正在提交…":"立即提交");account.setEnabled(!active);password.setEnabled(!active);steps.setEnabled(!active);stepSlider.setEnabled(!active);eye.setEnabled(!active);remember.setEnabled(!active);clear.setEnabled(!active);
    }
    private String redact(String s,String user,String pass) {
        if(!pass.isEmpty())s=s.replace(pass,"[密码已隐藏]");if(!user.isEmpty())s=s.replace(user,"[账号已隐藏]");return s.length()>1200?s.substring(0,1200):s;
    }
    private void startSubmit() {
        if(busy)return;
        submitIndicator.pause();submitIndicator.setVisibility(View.GONE);
        final String user=account.getText().toString().trim(), pass=password.getText().toString(); final int n;
        try { n=StepProtocol.validate(user,pass,steps.getText().toString()); }
        catch(IllegalArgumentException e){resultTitle.setText("请检查输入");result.setText(e.getMessage());return;}
        try{if(remember.isChecked())vault.save(user,pass);else vault.clear();}
        catch(Exception e){resultTitle.setText("无法加密保存");result.setText("请取消记住密码后重试。");return;}
        getPreferences(MODE_PRIVATE).edit().putInt("steps",n).apply();
        InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(steps.getWindowToken(),0);
        setBusy(true);submitIndicator.resume();submitIndicator.showState(SubmitIndicator.LOADING);resultTitle.setText("正在连接服务");result.setText("正在提交 "+n+" 步，请勿重复操作。");
        worker.execute(()->{
            String title, detail; int feedback=SubmitIndicator.UNKNOWN;
            try {JSONObject body=ApiClient.submit(user,pass,n); boolean ok=StepProtocol.accepted(body);
                feedback=ok?SubmitIndicator.ACCEPTED:SubmitIndicator.ERROR;
                title=ok?"服务已接受请求":"提交未成功";
                detail=redact(StepProtocol.message(body),user,pass)+"\n返回码："+body.optString("code","未知");
                if(ok)detail+="\n请打开微信运动确认实际同步结果。";
            }catch(SocketTimeoutException e){title="请求超时";detail="提交结果暂不确定，请先检查微信运动，再决定是否重试。";}
            catch(javax.net.ssl.SSLException e){title="安全连接失败";detail="请检查网络、系统时间和服务证书后重试。";}
            catch(Exception e){title="连接或响应异常";detail=redact(e.getMessage()==null?"请检查网络后重试。":e.getMessage(),user,pass)+"\n请先确认微信运动是否已更新。";}
            final String t=title,d=detail; final int outcome=feedback; main.post(()->{if(isFinishing()||isDestroyed())return;setBusy(false);submitIndicator.showState(outcome);checkMembership(true);resultTitle.setText(t);result.setText(d+"\n"+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date()));});
        });
    }
    private void checkMembership(boolean force) {
        final String user=account.getText().toString().trim();
        if(user.isEmpty()) { if(force)membershipDetail.setText("请先填写 Zepp 账号，无需填写密码。"); return; }
        if(checkingExpiry || (!force && user.equals(checkedAccount) && SystemClock.elapsedRealtime()-lastCheck<60000))return;
        checkingExpiry=true; checkExpiry.setEnabled(false); checkExpiry.setText("正在检查…");
        membershipTitle.setText("正在查询有效期"); membershipDetail.setText("正在读取账号已兑换的权益…");
        worker.execute(()->{
            Membership found=null; String error=null;
            try { found=Membership.parse(ApiClient.checkMembership(user)); }
            catch(Exception e) { error="查询失败，请检查网络后重试。有效期暂时无法确认。"; }
            final Membership value=found; final String message=error;
            main.post(()->{
                if(isFinishing()||isDestroyed())return;
                checkingExpiry=false;checkExpiry.setEnabled(true);checkExpiry.setText("重新检查");
                if(!user.equals(account.getText().toString().trim())) { renderMembership(); checkMembership(false); return; }
                membership=value;checkedAccount=user;lastCheck=SystemClock.elapsedRealtime();
                if(message!=null){membershipTitle.setText("暂时无法查询");membershipDetail.setText(message);}
                else renderMembership();
            });
        });
    }
    private void renderMembership() {
        if(membershipTitle==null || checkingExpiry)return;
        if(membership==null){
            if(checkedAccount.isEmpty()){membershipTitle.setText("卡密有效期");membershipTitle.setTextColor(ink);
            membershipDetail.setText("查询账号已兑换的会员权益，到期检查无需密码。");}
            return;
        }
        long now=System.currentTimeMillis();
        boolean expired=membership.expired(now), soon=membership.soon(now);
        membershipTitle.setText(membership.title(now));
        membershipTitle.setTextColor(expired?Color.rgb(179,38,30):soon?Color.rgb(146,83,0):ink);
        membershipDetail.setText(membership.detail(now)+"\n再次进入应用或点击按钮可重新查询。");
        String key=checkedAccount+":"+membership.expiry+":"+(expired?"expired":"soon");
        if((expired||soon)&&!key.equals(alerted)&&hasWindowFocus()) {
            alerted=key;
            new AlertDialog.Builder(this).setTitle(expired?"卡密已过期":"卡密即将到期")
                .setMessage(membership.detail(now)).setPositiveButton("知道了",null).show();
        }
    }
    @Override protected void onResume(){super.onResume();main.post(expiryTick);if(submitIndicator!=null)submitIndicator.resume();if(cardPanel!=null)cardPanel.resume();if(quotePanel!=null)quotePanel.resume();main.post(()->checkMembership(false));}
    @Override protected void onPause(){super.onPause();if(quotePanel!=null)quotePanel.pause();if(cardPanel!=null)cardPanel.pause();if(submitIndicator!=null)submitIndicator.pause();main.removeCallbacks(expiryTick);if(password!=null)setPasswordVisible(false);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)renderMembership();}
    @Override public void onBackPressed(){if(busy){Toast.makeText(this,"提交中，请等待结果",Toast.LENGTH_SHORT).show();return;}super.onBackPressed();}
    @Override protected void onDestroy(){super.onDestroy();if(quotePanel!=null)quotePanel.close();if(cardPanel!=null)cardPanel.close();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
