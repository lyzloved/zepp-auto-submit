package cn.steps.simple;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small outline eye in the password field; closed eyelid when concealed. */
final class EyeDrawable extends Drawable {
    private final boolean visible;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    EyeDrawable(boolean visible){this.visible=visible;paint.setColor(Color.rgb(95,99,104));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    @Override public void draw(Canvas canvas){
        canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
        Path p=new Path();
        if(visible){p.moveTo(2,12);p.cubicTo(7,4,17,4,22,12);p.cubicTo(17,20,7,20,2,12);canvas.drawPath(p,paint);canvas.drawCircle(12,12,3,paint);}
        else{p.moveTo(3,9);p.cubicTo(7,16,17,16,21,9);canvas.drawPath(p,paint);canvas.drawLine(6,12,4,15,paint);canvas.drawLine(10,14,9,18,paint);canvas.drawLine(14,14,15,18,paint);canvas.drawLine(18,12,20,15,paint);}
        canvas.restore();
    }
    @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
