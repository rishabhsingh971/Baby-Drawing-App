package com.india.apkcrew.babysdrawingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * The type Paint view.
 */
public class PaintView extends View {
    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = Color.RED;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    private boolean bitmapSet = false;
    /**
     * The Drawing changed.
     */
    public boolean drawingChanged = false, /**
     * The Drawing changing.
     */
    drawingChanging = false;
    private ArrayList<PathColorPair> paths = new ArrayList<>();
    private ArrayList<PathColorPair> undonePaths = new ArrayList<>();
    /**
     * The Bg music.
     */
    public MediaPlayer bg_music;

    private class PathColorPair {
        /**
         * The Path.
         */
        Path path;
        /**
         * The Color.
         */
        int color;

        /**
         * Instantiates a new Path color pair.
         *
         * @param path  the path
         * @param color the color
         */
        PathColorPair(Path path, int color) {
            this.path = path;
            this.color = color;
        }

        /**
         * Instantiates a new Path color pair.
         *
         * @param other the other
         */
        PathColorPair(PathColorPair other) {
            this.path = other.path;
            this.color = other.color;
        }
    }

    /**
     * Instantiates a new Paint view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawPath = new Path();
        drawPaint = new Paint();
        drawCanvas = new Canvas();
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        int brushSize = getResources().getInteger(R.integer.brush_size);
        drawPaint.setAntiAlias(true);
        drawPaint.setDither(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setColor(paintColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //view given size
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    /**
     * Sets color.
     *
     * @param newColor the new color
     */
    public void setColor(String newColor) {
        //set color
        invalidate();
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);
    }

    /**
     * Sets erase.
     *
     * @param isErase the is erase
     */
    public void setErase(boolean isErase) {
        if (isErase) {
            paintColor = Color.WHITE;
            drawPaint.setColor(Color.WHITE);
        }
    }

    /**
     * Start new.
     */
    public void startNew() {
        bitmapSet = false;
        paths.clear();
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    /**
     * Sets bit map.
     *
     * @param fileName the file name
     */
    public void setBitMap(String fileName) {
//        startNew();
        paths.clear();
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvasBitmap = BitmapFactory.decodeFile(fileName).copy(Bitmap.Config.ARGB_8888, true);
//    drawCanvas = new_icon Canvas(bmp);
        bitmapSet = true;
        drawCanvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        invalidate();
    }

    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;

    private void touch_start(float x, float y) {
        undonePaths.clear();
        drawPath.reset();
        drawPath.moveTo(x, y);
        mX = x;
        mY = y;
        drawingChanging = true;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            drawPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {
        drawPath.lineTo(mX, mY);
        drawCanvas.drawPath(drawPath, drawPaint);
        paths.add(new PathColorPair(drawPath, paintColor));
        drawPath = new Path();
        drawingChanging = false;
        drawingChanged = true;
    }

    /**
     * Can undo boolean.
     *
     * @return the boolean
     */
    boolean canUndo() {
        return paths.size() > 0;
    }

    /**
     * Can redo boolean.
     *
     * @return the boolean
     */
    boolean canRedo() {
        return undonePaths.size() > 0;
    }

    /**
     * On click undo.
     */
    public void onClickUndo() {
        if (canUndo()) {
            undonePaths.add(new PathColorPair(paths.remove(paths.size() - 1)));
            drawingChanged = true;
            invalidate();
        } else {
            Toast.makeText(getContext(), "Nothing to undo", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * On click redo.
     */
    public void onClickRedo() {
        if (canRedo()) {
            paths.add(new PathColorPair(undonePaths.remove(undonePaths.size() - 1)));
            drawingChanged = true;
            invalidate();
        } else {
            Toast.makeText(getContext(), "Nothing to redo", Toast.LENGTH_SHORT)
                    .show();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmapSet) {
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        }
        for (PathColorPair pc : paths) {
            drawPaint.setColor(pc.color);
            canvas.drawPath(pc.path, drawPaint);
        }
        drawPaint.setColor(paintColor);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                bg_music.start();
                bg_music.setLooping(true);
                touch_start(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                break;
            case MotionEvent.ACTION_UP:
                bg_music.pause();
                touch_up();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
}
