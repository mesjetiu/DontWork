package info.pauek.dontwork;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

/**
 * Created by pauek on 29/10/15.
 */
public class BlockScreenView extends ViewGroup {

    private Paint redPaint;

    public BlockScreenView(Context context) {
        super(context);
        redPaint = new Paint();
        redPaint.setAntiAlias(true);
        redPaint.setTextSize(10);
        redPaint.setARGB(255, 255, 0, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // ?
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i("DontWork", "Draw!");
        super.onDraw(canvas);
        canvas.drawRect(100, 100, 300, 300, redPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("DontWork", "Touch!");
        return false;
    }
}
