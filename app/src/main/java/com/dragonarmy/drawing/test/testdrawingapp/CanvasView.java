package com.dragonarmy.drawing.test.testdrawingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by kevinbachman on 5/11/15.
 */
public class CanvasView extends View {

    // Enumeration for Mode
    public enum Mode {
        DRAW,
        TEXT,
        ERASER,
        POINTER;
    }

    // Enumeration for Drawer
    public enum Drawer {
        PEN,
        LINE,
        RECTANGLE,
        CIRCLE,
        ELLIPSE,
        QUADRATIC_BEZIER,
        QUBIC_BEZIER;
    }

    // Enumeration for Object Type
    public enum objType {
        PATH,
        BITMAP,
        TEXT;
    }

    private Context context = null;
    private Canvas canvas   = null;
    private Bitmap bitmap   = null;

    private List<Path> pathLists  = new ArrayList<Path>();
    private List<Paint> paintLists = new ArrayList<Paint>();
    private List<HashMap> bitmapLists = new ArrayList<HashMap>();
    private List<HashMap> textLists = new ArrayList<HashMap>();

    private int touchObjectIndex = -1;
    private objType touchObjectType;
    private int touchOffsetX = 0;
    private int touchOffsetY = 0;

    // for Eraser
    private int baseColor = Color.WHITE;

    // for Undo, Redo
    private int historyPointer = 0;

    // Flags
    private Mode mode      = Mode.DRAW;
    private Drawer drawer  = Drawer.PEN;
    private boolean isDown = false;

    // for Paint
    private Paint.Style paintStyle = Paint.Style.STROKE;
    private int paintStrokeColor   = Color.BLACK;
    private int paintFillColor     = Color.BLACK;
    private float paintStrokeWidth = 3F;
    private int opacity            = 255;
    private float blur             = 0F;
    private Paint.Cap lineCap      = Paint.Cap.ROUND;

    // for Text
    private String text           = "";
    private Typeface fontFamily   = Typeface.DEFAULT;
    private float fontSize        = 32F;
    private Paint.Align textAlign = Paint.Align.LEFT;  // fixed
    private Paint textPaint       = new Paint();
    private float textX           = 0F;
    private float textY           = 0F;

    // for Drawer
    private float startX   = 0F;
    private float startY   = 0F;
    private float controlX = 0F;
    private float controlY = 0F;

    // for bitmap
    private float bitmapX   = 0F;
    private float bitmapY   = 0F;

    /**
     * Copy Constructor
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setup(context);
    }

    /**
     * Copy Constructor
     *
     * @param context
     * @param attrs
     */
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setup(context);
    }

    /**
     * Copy Constructor
     *
     * @param context
     */
    public CanvasView(Context context) {
        super(context);
        this.setup(context);
    }

    /**
     * Common initialization.
     *
     * @param context
     */
    private void setup(Context context) {
        this.context = context;

        this.pathLists.add(new Path());
        this.paintLists.add(this.createPaint());
        this.bitmapLists.add(null);
        this.textLists.add(null);
        this.historyPointer++;

        this.textPaint.setARGB(0, 255, 255, 255);
    }

    /**
     * This method creates the instance of Paint.
     * In addition, this method sets styles for Paint.
     *
     * @return paint This is returned as the instance of Paint
     */
    private Paint createPaint() {
        Paint paint = new Paint();

        paint.setAntiAlias(true);
        paint.setStyle(this.paintStyle);
        paint.setStrokeWidth(this.paintStrokeWidth);
        paint.setStrokeCap(this.lineCap);
        paint.setStrokeJoin(Paint.Join.MITER);  // fixed

        // for Text
        if (this.mode == Mode.TEXT) {
            paint.setTypeface(this.fontFamily);
            paint.setTextSize(this.fontSize);
            paint.setTextAlign(this.textAlign);
            paint.setStrokeWidth(0F);
        }

        if (this.mode == Mode.ERASER) {
            // Eraser
            // paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            // paint.setARGB(0, 0, 0, 0);
            paint.setColor(this.baseColor);
            paint.setShadowLayer(this.blur, 0F, 0F, this.baseColor);
        } else {
            // Otherwise
            paint.setColor(this.paintStrokeColor);
            paint.setShadowLayer(this.blur, 0F, 0F, this.paintStrokeColor);
        }

        paint.setAlpha(this.opacity);

        return paint;
    }

    /**
     * This method initialize Path.
     * Namely, this method creates the instance of Path,
     * and moves current position.
     *
     * @param event This is argument of onTouchEvent method
     * @return path This is returned as the instance of Path
     */
    private Path createPath(MotionEvent event) {
        Path path = new Path();

        // Save for ACTION_MOVE
        this.startX = event.getX();
        this.startY = event.getY();

        path.moveTo(this.startX, this.startY);

        return path;
    }

    /**
     * This method updates the lists for the instance of Path and Paint.
     * "Undo" and "Redo" are enabled by this method.
     *
     * @param path the instance of Path
     * @param bitmapHash the instance of Bitmap
     * @param textHash the instance of Bitmap
     */
    public void updateHistory(Path path, HashMap bitmapHash, HashMap textHash) {

        // Clear the time stream if changing history after Undo
        if (this.historyPointer < this.pathLists.size()) {
            for(int i = this.pathLists.size(); i > this.historyPointer; i--) {
                this.pathLists.remove(i-1);
                this.bitmapLists.remove(i-1);
                this.textLists.remove(i-1);
                this.paintLists.remove(i-1);
            }
        }

        if(path != null) {
            if (this.historyPointer == this.pathLists.size()) {
                this.pathLists.add(path);
                this.paintLists.add(this.createPaint());
                this.historyPointer++;
            }
        } else {
            this.pathLists.add(null);
        }

        if(bitmapHash != null) {
            if (this.historyPointer == this.bitmapLists.size()) {
                this.bitmapLists.add(bitmapHash);
                this.paintLists.add(null);
                this.historyPointer++;
            }
        } else {
            this.bitmapLists.add(null);
        }

        if(textHash != null) {
            if (this.historyPointer == this.textLists.size()) {
                this.textLists.add(textHash);
                this.paintLists.add(this.createPaint());
                this.historyPointer++;
            }
        } else {
            this.textLists.add(null);
        }

        Log.d("****HISTORY", Integer.toString(this.historyPointer));
        Log.d("****PATHS", Integer.toString(this.pathLists.size()) + " - " + this.pathLists);
        Log.d("****BITMAPS", Integer.toString(this.bitmapLists.size()) + " - " + this.bitmapLists);
        Log.d("****TEXTS", Integer.toString(this.textLists.size()) + " - " + this.textLists);
        Log.d("****PAINTS", Integer.toString(this.paintLists.size()) + " - " + this.paintLists);
    }

    /**
     * This method gets the instance of Path that pointer indicates.
     *
     * @return the instance of Path
     */
    private Path getCurrentPath() {
        return this.pathLists.get(this.historyPointer - 1);
    }

    /**
     * This method draws text.
     *
     * @param canvas the instance of Canvas
     */
    private void drawText(Canvas canvas, float textX, float textY, Paint paint) {
        if (this.text.length() <= 0) {
            return;
        }

        if (this.mode == Mode.TEXT) {
            this.textX = this.startX;
            this.textY = this.startY;
        }

        this.textPaint = paint;

//        Paint paintForMeasureText = new Paint();

        // Line break automatically
//        float textLength   = paintForMeasureText.measureText(this.text);
//        float lengthOfChar = textLength / (float)this.text.length();
//        float restWidth    = this.canvas.getWidth() - textX;  // text-align : right
//        int numChars       = (lengthOfChar <= 0) ? 1 : (int)Math.floor((double)(restWidth / lengthOfChar));  // The number of characters at 1 line
//        int modNumChars    = (numChars < 1) ? 1 : numChars;
//        float y            = textY;
//
//        for (int i = 0, len = this.text.length(); i < len; i += modNumChars) {
//            String substring = "";
//
//            if ((i + modNumChars) < len) {
//                substring = this.text.substring(i, (i + modNumChars));
//            } else {
//                substring = this.text.substring(i, len);
//            }
//
//            y += this.fontSize;
//
//            canvas.drawText(substring, textX, y, this.textPaint);
//        }

        canvas.drawText(this.text, textX, textY, this.textPaint);
    }

    /**
     * This method defines processes on MotionEvent.ACTION_DOWN
     *
     * @param event This is argument of onTouchEvent method
     */
    private void onActionDown(MotionEvent event) {
        int xTouch = (int) event.getX(0);
        int yTouch = (int) event.getY(0);

        switch (this.mode) {
            case DRAW   :
            case ERASER :
                if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
                    // Oherwise
                    this.updateHistory(this.createPath(event), null, null);
                    this.isDown = true;
                } else {
                    // Bezier
                    if ((this.startX == 0F) && (this.startY == 0F)) {
                        // The 1st tap
                        this.updateHistory(this.createPath(event), null, null);
                    } else {
                        // The 2nd tap
                        this.controlX = event.getX();
                        this.controlY = event.getY();

                        this.isDown = true;
                    }
                }

                break;
            case TEXT   :
//                this.startX = event.getX();
//                this.startY = event.getY();

                break;
            case POINTER :
                for (int i = 0; i < this.historyPointer; i++) {
                    if(this.bitmapLists.get(i) != null) {
                        Bitmap bitmap = (Bitmap) this.bitmapLists.get(i).get("bitmap");
                        float borderLeft = (float) this.bitmapLists.get(i).get("x");
                        float borderRight = (float) this.bitmapLists.get(i).get("x") + bitmap.getWidth();
                        float borderTop = (float) this.bitmapLists.get(i).get("y");
                        float borderBotom = (float) this.bitmapLists.get(i).get("y") + bitmap.getWidth();

                        if((xTouch >= borderLeft && xTouch <= borderRight) && yTouch >= borderTop && yTouch <= borderBotom) {
                            this.bitmapLists.add(this.bitmapLists.get(i));
                            this.bitmapLists.remove(i);
                            this.paintLists.add(null);
                            this.paintLists.remove(i);
                            this.pathLists.add(null);
                            this.pathLists.remove(i);
                            this.textLists.add(null);
                            this.textLists.remove(i);

                            touchObjectIndex = this.bitmapLists.size()-1;
                            touchObjectType = objType.BITMAP;
                            this.touchOffsetX = xTouch - (int)borderLeft;
                            this.touchOffsetY = yTouch - (int)borderTop;

                        }
                    }

                    if(this.textLists.get(i) != null) {
                        Paint paintForMeasureText = this.paintLists.get(i);
                        Rect bounds = new Rect();

                        paintForMeasureText.getTextBounds(text, 0, text.length(), bounds);


                        float borderLeft = (float) this.textLists.get(i).get("x") - 10;
                        float borderRight = (float) this.textLists.get(i).get("x") + bounds.width();
                        float borderTop = (float) this.textLists.get(i).get("y") - 10;
                        float borderBotom = (float) this.textLists.get(i).get("y") + bounds.height();

                        Log.d("***TEXT: ", "left - " + borderLeft + "right - " + borderRight + "top - " + borderTop + "bottom - " + borderBotom );
                        Log.d("***TOUCH: ", "x - " + xTouch + ", y - " + yTouch );

                        if((xTouch >= borderLeft && xTouch <= borderRight) && yTouch >= borderTop && yTouch <= borderBotom) {
                            Log.d("***TEXT: ", "Touched");
                            this.bitmapLists.add(null);
                            this.bitmapLists.remove(i);
                            this.paintLists.add(this.paintLists.get(i));
                            this.paintLists.remove(i);
                            this.pathLists.add(null);
                            this.pathLists.remove(i);
                            this.textLists.add(this.textLists.get(i));
                            this.textLists.remove(i);

                            touchObjectIndex = this.textLists.size()-1;
                            touchObjectType = objType.TEXT;
                            this.touchOffsetX = xTouch - (int)borderLeft;
                            this.touchOffsetY = yTouch - (int)borderTop;
                        }
                    }
                }

                break;
            default :
                break;
        }
    }

    /**
     * This method defines processes on MotionEvent.ACTION_MOVE
     *
     * @param event This is argument of onTouchEvent method
     */
    private void onActionMove(MotionEvent event) {
        float xTouch = event.getX();
        float yTouch = event.getY();

        switch (this.mode) {
            case DRAW   :
            case ERASER :

                if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
                    if (!isDown) {
                        return;
                    }

                    Path path = this.getCurrentPath();

                    switch (this.drawer) {
                        case PEN :
                            path.lineTo(xTouch, yTouch);
                            break;
                        case LINE :
                            path.reset();
                            path.moveTo(this.startX, this.startY);
                            path.lineTo(xTouch, yTouch);
                            break;
                        case RECTANGLE :
                            path.reset();
                            path.addRect(this.startX, this.startY, xTouch, yTouch, Path.Direction.CCW);
                            break;
                        case CIRCLE :
                            double distanceX = Math.abs((double)(this.startX - xTouch));
                            double distanceY = Math.abs((double)(this.startX - yTouch));
                            double radius    = Math.sqrt(Math.pow(distanceX, 2.0) + Math.pow(distanceY, 2.0));

                            path.reset();
                            path.addCircle(this.startX, this.startY, (float)radius, Path.Direction.CCW);
                            break;
                        case ELLIPSE :
                            RectF rect = new RectF(this.startX, this.startY, xTouch, yTouch);

                            path.reset();
                            path.addOval(rect, Path.Direction.CCW);
                            break;
                        default :
                            break;
                    }
                } else {
                    if (!isDown) {
                        return;
                    }

                    Path path = this.getCurrentPath();

                    path.reset();
                    path.moveTo(this.startX, this.startY);
                    path.quadTo(this.controlX, this.controlY, xTouch, yTouch);
                }

                break;
            case TEXT :
//                this.startX = xTouch;
//                this.startY = yTouch;

                break;
            case POINTER :
                if(touchObjectIndex >= 0 && touchObjectType != null) {
                    switch (touchObjectType) {
                        case BITMAP:
                            if(this.bitmapLists.size() > 0) {
                                this.bitmapLists.get(touchObjectIndex).put("x", xTouch-this.touchOffsetX);
                                this.bitmapLists.get(touchObjectIndex).put("y", yTouch-this.touchOffsetY);
                                this.invalidate();
                            }
                            break;
                        case TEXT:
                            if(this.textLists.size() > 0) {
                                this.textLists.get(touchObjectIndex).put("x", xTouch-this.touchOffsetX);
                                this.textLists.get(touchObjectIndex).put("y", yTouch-this.touchOffsetY);
                                this.invalidate();
                            }
                            break;
                        default :
                            break;
                    }
                }

                break;
            default :
                break;
        }
    }

    /**
     * This method defines processes on MotionEvent.ACTION_DOWN
     *
     * @param event This is argument of onTouchEvent method
     */
    private void onActionUp(MotionEvent event) {
        float xTouch = event.getX();
        float yTouch = event.getY();

        if (isDown) {
            this.startX = 0F;
            this.startY = 0F;
            this.isDown = false;
            this.touchObjectIndex = -1;
            this.touchObjectType = null;
            this.touchOffsetX = 0;
            this.touchOffsetY = 0;
        }

        switch (this.mode) {
            case TEXT :

//                HashMap textHash = new HashMap();
//                textHash.put("text", this.text);
//                textHash.put("x", xTouch);
//                textHash.put("y", yTouch);
//
//                this.updateHistory(null, null, textHash);
//
//                this.invalidate();

                break;
            case POINTER :

                break;
            default :
                break;
        }
    }

    /**
     * This method updates the instance of Canvas (View)
     *
     * @param canvas the new instance of Canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Before "drawPath"
        canvas.drawColor(this.baseColor);

        for (int i = 0; i < this.historyPointer; i++) {
            if(this.bitmapLists.get(i) != null) {
                Bitmap bitmap = (Bitmap) this.bitmapLists.get(i).get("bitmap");
                float bitmapX = (float) this.bitmapLists.get(i).get("x");
                float bitmapY = (float) this.bitmapLists.get(i).get("y");
                canvas.drawBitmap(bitmap, bitmapX, bitmapY, new Paint());
            }

            if(this.pathLists.get(i) != null) {
                Path path = this.pathLists.get(i);
                Paint paintPath = this.paintLists.get(i);

                canvas.drawPath(path, paintPath);
            }

            if(this.textLists.get(i) != null) {
                setText((String) this.textLists.get(i).get("text"));
                float textX = (float) this.textLists.get(i).get("x");
                float textY = (float) this.textLists.get(i).get("y");
                Paint paintText = this.paintLists.get(i);
                this.drawText(canvas, textX, textY, paintText);
            }
        }

        this.canvas = canvas;
    }

    /**
     * This method set event listener for drawing.
     *
     * @param event the instance of MotionEvent
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.onActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE :
                this.onActionMove(event);
                break;
            case MotionEvent.ACTION_UP :
                this.onActionUp(event);
                break;
            default :
                break;
        }

        // Re draw
        this.invalidate();

        return true;
    }

    /**
     * This method is getter for canvas object.
     *
     * @return
     */
    public Canvas getCanvas() {
        return this.canvas;
    }

    /**
     * This method is getter for mode.
     *
     * @return
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * This method is setter for mode.
     *
     * @param mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * This method is getter for drawer.
     *
     * @return
     */
    public Drawer getDrawer() {
        return this.drawer;
    }

    /**
     * This method is setter for drawer.
     *
     * @param drawer
     */
    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    /**
     * This method draws canvas again for Undo.
     *
     * @return If Undo is enabled, this is returned as true. Otherwise, this is returned as false.
     */
    public boolean undo() {
        if (this.historyPointer > 1) {
            this.historyPointer--;
            this.invalidate();

            return true;
        } else {
            return false;
        }
    }

    /**
     * This method draws canvas again for Redo.
     *
     * @return If Redo is enabled, this is returned as true. Otherwise, this is returned as false.
     */
    public boolean redo() {
        if (this.historyPointer < this.pathLists.size()) {
            this.historyPointer++;
            this.invalidate();

            return true;
        } else {
            return false;
        }
    }

    /**
     * This method initializes canvas.
     *
     * @return
     */
    public void clear() {
        this.pathLists  = new ArrayList<Path>();
        this.paintLists = new ArrayList<Paint>();
        this.bitmapLists = new ArrayList<HashMap>();
        this.textLists = new ArrayList<HashMap>();
        this.historyPointer = 0;

        // Clear
        this.invalidate();
    }

    /**
     * This method is getter for canvas background color
     *
     * @return
     */
    public int getBaseColor() {
        return this.baseColor;
    }

    /**
     * This method is setter for canvas background color
     *
     * @param color
     */
    public void setBaseColor(int color) {
        this.baseColor = color;
    }

    /**
     * This method is getter for drawn text.
     *
     * @return
     */
    public String getText() {
        return this.text;
    }

    /**
     * This method is setter for drawn text.
     *
     * @param text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * This method is getter for stroke or fill.
     *
     * @return
     */
    public Paint.Style getPaintStyle() {
        return this.paintStyle;
    }

    /**
     * This method is setter for stroke or fill.
     *
     * @param style
     */
    public void setPaintStyle(Paint.Style style) {
        this.paintStyle = style;
    }

    /**
     * This method is getter for stroke color.
     *
     * @return
     */
    public int getPaintStrokeColor() {
        return this.paintStrokeColor;
    }

    /**
     * This method is setter for stroke color.
     *
     * @param color
     */
    public void setPaintStrokeColor(int color) {
        this.paintStrokeColor = color;
    }

    /**
     * This method is getter for fill color.
     * But, current Android API cannot set fill color (?).
     *
     * @return
     */
    public int getPaintFillColor() {
        return this.paintFillColor;
    };

    /**
     * This method is setter for fill color.
     * But, current Android API cannot set fill color (?).
     *
     * @param color
     */
    public void setPaintFillColor(int color) {
        this.paintFillColor = color;
    }

    /**
     * This method is getter for stroke width.
     *
     * @return
     */
    public float getPaintStrokeWidth() {
        return this.paintStrokeWidth;
    }

    /**
     * This method is setter for stroke width.
     *
     * @param width
     */
    public void setPaintStrokeWidth(float width) {
        this.paintStrokeWidth = width;
    }

    /**
     * This method is getter for alpha.
     *
     * @return
     */
    public int getOpacity() {
        return this.opacity;
    }

    /**
     * This method is setter for alpha.
     * The 1st argument must be between 0 and 255.
     *
     * @param opacity
     */
    public void setOpacity(int opacity) {
        if ((opacity >= 0) && (opacity <= 255)) {
            this.opacity = opacity;
        } else {
            this.opacity= 255;
        }
    }

    /**
     * This method is getter for amount of blur.
     *
     * @return
     */
    public float getBlur() {
        return this.blur;
    }

    /**
     * This method is setter for amount of blur.
     * The 1st argument is greater than or equal to 0.0.
     *
     * @param blur
     */
    public void setBlur(float blur) {
        if (blur >= 0) {
            this.blur = blur;
        } else {
            this.blur = 0F;
        }
    }

    /**
     * This method is getter for line cap.
     *
     * @return
     */
    public Paint.Cap getLineCap() {
        return this.lineCap;
    }

    /**
     * This method is setter for line cap.
     *
     * @param cap
     */
    public void setLineCap(Paint.Cap cap) {
        this.lineCap = cap;
    }

    /**
     * This method is getter for font size,
     *
     * @return
     */
    public float getFontSize() {
        return this.fontSize;
    }

    /**
     * This method is setter for font size.
     * The 1st argument is greater than or equal to 0.0.
     *
     * @param size
     */
    public void setFontSize(float size) {
        if (size >= 0F) {
            this.fontSize = size;
        }
    }

    /**
     * This method is getter for font-family.
     *
     * @return
     */
    public Typeface getFontFamily() {
        return this.fontFamily;
    }

    /**
     * This method is setter for font-family.
     *
     * @param face
     */
    public void setFontFamily(Typeface face) {
        this.fontFamily = face;
    }

    /**
     * This method gets current canvas as bitmap.
     *
     * @return This is returned as bitmap.
     */
    public Bitmap getBitmap() {
        this.setDrawingCacheEnabled(false);
        this.setDrawingCacheEnabled(true);

        return Bitmap.createBitmap(this.getDrawingCache());
    }

    /**
     * This method gets current canvas as scaled bitmap.
     *
     * @return This is returned as scaled bitmap.
     */
    public Bitmap getScaleBitmap(int w, int h) {
        this.setDrawingCacheEnabled(false);
        this.setDrawingCacheEnabled(true);

        return Bitmap.createScaledBitmap(this.getDrawingCache(), w, h, true);
    }

    /**
     * This method draws the designated bitmap to canvas.
     *
     * @param bitmap
     */
    public void drawBitmap(Bitmap bitmap, float x, float y) {

        int targetWidth = 150;
        double aspectRatio = (double) bitmap.getHeight() / (double) bitmap.getWidth();
        int targetHeight = (int) (targetWidth * aspectRatio);
        bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);


//        bitmap = Bitmap.createScaledBitmap(bitmap, 125, 100, true);

        this.bitmapX = x;
        this.bitmapY = y;
        this.bitmap = bitmap;

        HashMap bitmapHash = new HashMap();
        bitmapHash.put("bitmap", bitmap);
        bitmapHash.put("x", x);
        bitmapHash.put("y", y);

        this.updateHistory(null, bitmapHash, null);

        this.setMode(CanvasView.Mode.POINTER);

        this.invalidate();
    }

    /**
     * This method draws the designated byte array of bitmap to canvas.
     *
     * @param byteArray This is returned as byte array of bitmap.
     */
    public void drawBitmap(byte[] byteArray, float x, float y) {
        this.drawBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), x, y);
    }

    /**
     * This static method gets the designated bitmap as byte array.
     *
     * @param bitmap
     * @param format
     * @param quality
     * @return This is returned as byte array of bitmap.
     */
    public static byte[] getBitmapAsByteArray(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * This method gets the bitmap as byte array.
     *
     * @param format
     * @param quality
     * @return This is returned as byte array of bitmap.
     */
    public byte[] getBitmapAsByteArray(Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.getBitmap().compress(format, quality, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * This method gets the bitmap as byte array.
     * Bitmap format is PNG, and quality is 100.
     *
     * @return This is returned as byte array of bitmap.
     */
    public byte[] getBitmapAsByteArray() {
        return this.getBitmapAsByteArray(Bitmap.CompressFormat.PNG, 100);
    }

}