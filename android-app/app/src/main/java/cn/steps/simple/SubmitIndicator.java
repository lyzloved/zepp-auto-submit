package cn.steps.simple;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.os.Build;

/** Indeterminate network activity; never claims a percentage or WeChat sync. */
final class SubmitIndicator extends View {
    static final int LOADING=1, ACCEPTED=2, ERROR=3, UNKNOWN=4;
    private int state=LOADING;
    private float angle;
    private boolean paused;
    private ValueAnimator rotation;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    SubmitIndicator(Context context){super(context);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.3f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void showState(int next){
        stop();state=next;setVisibility(VISIBLE);angle=0;
        if(next==LOADING)start();
        else if(!paused&&animationsEnabled()){setScaleX(.8f);setScaleY(.8f);setAlpha(.4f);animate().scaleX(1).scaleY(1).alpha(1).setDuration(180).start();}
        invalidate();
    }
    private boolean animationsEnabled(){return Build.VERSION.SDK_INT<26||ValueAnimator.areAnimatorsEnabled();}
    private void start(){
        if(paused||!animationsEnabled()||!isAttachedToWindow())return;
        rotation=ValueAnimator.ofFloat(0,360);rotation.setDuration(950);rotation.setInterpolator(new LinearInterpolator());rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.addUpdateListener(a->{angle=(Float)a.getAnimatedValue();invalidate();});rotation.start();
    }
    private void stop(){if(rotation!=null){rotation.cancel();rotation.removeAllUpdateListeners();rotation=null;}animate().cancel();setAlpha(1);setScaleX(1);setScaleY(1);}
    void pause(){paused=true;stop();}
    void resume(){paused=false;if(state==LOADING&&getVisibility()==VISIBLE&&rotation==null)start();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();resume();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/28f,getHeight()/28f);
        if(state==LOADING){paint.setColor(Color.rgb(220,232,255));canvas.drawCircle(14,14,10,paint);paint.setColor(Color.rgb(11,87,208));canvas.save();canvas.rotate(angle,14,14);canvas.drawArc(4,4,24,24,-90,260,false,paint);canvas.restore();}
        else{
            paint.setColor(state==ACCEPTED?Color.rgb(24,128,56):state==ERROR?Color.rgb(179,38,30):Color.rgb(146,83,0));canvas.drawCircle(14,14,10,paint);
            Path p=new Path();
            if(state==ACCEPTED){p.moveTo(9,14);p.lineTo(12.5f,17.5f);p.lineTo(19,10.5f);canvas.drawPath(p,paint);}
            else if(state==ERROR){canvas.drawLine(14,8.5f,14,15,paint);canvas.drawPoint(14,19,paint);}
            else{p.moveTo(10,10);p.cubicTo(10,5.5f,19,5.5f,18,11);p.cubicTo(17.5f,13,14,13,14,15.5f);canvas.drawPath(p,paint);canvas.drawPoint(14,19,paint);}
        }
        canvas.restore();
    }
}
