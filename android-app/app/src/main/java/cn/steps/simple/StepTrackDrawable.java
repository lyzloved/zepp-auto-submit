package cn.steps.simple;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Rounded track and rounded progress, including both end positions. */
final class StepTrackDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float thickness;
    StepTrackDrawable(float density) { thickness = 10f * density; }
    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float top = bounds.exactCenterY() - thickness / 2f;
        float bottom = top + thickness;
        paint.setColor(0xffdce8ff);
        canvas.drawRoundRect(bounds.left, top, bounds.right, bottom, thickness / 2f, thickness / 2f, paint);
        float width = bounds.width() * getLevel() / 10000f;
        if (width > 0) {
            paint.setColor(0xff0b57d0);
            canvas.drawRoundRect(bounds.left, top, bounds.left + width, bottom, thickness / 2f, thickness / 2f, paint);
        }
    }
    @Override protected boolean onLevelChange(int level) { invalidateSelf(); return true; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
