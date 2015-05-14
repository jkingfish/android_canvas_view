package com.dragonarmy.drawing.test.testdrawingapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import io.fabric.sdk.android.Fabric;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import yuku.ambilwarna.AmbilWarnaDialog;


public class MainActivity extends Activity {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "pgNZCMVgivZR4Pvqtm5jnJgDZ";
    private static final String TWITTER_SECRET = "OEzZg6FA0y9mT3pbaFSr8NLpN1I5TzIsexrt3R8uwq6rWn0BwP";


    private CanvasView canvas = null;
    private LinearLayout addTextView;
    private EditText addText;
    private Button addTextButton;
    private AmbilWarnaDialog dialog;
    private int lastColor = 0xff000000;

    private static final int SELECT_PHOTO = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new Crashlytics());
        setContentView(R.layout.activity_main);

        // Create the instance of CanvasView
        this.canvas = (CanvasView)this.findViewById(R.id.canvas);
        this.addTextView = (LinearLayout)this.findViewById(R.id.llAddText);
        this.addText = (EditText)this.findViewById(R.id.etAddText);
        this.addTextButton = (Button)this.findViewById(R.id.btnSendText);
        addTextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (addText.getText().toString() != "") {
                    canvas.setText(addText.getText().toString());
                    addText.setText("");
                    addTextView.setVisibility(View.GONE);

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(addText.getWindowToken(), 0);

                    HashMap textHash = new HashMap();
                    textHash.put("text", canvas.getText());
                    textHash.put("x", 200f);
                    textHash.put("y", 200f);

                    canvas.updateHistory(null, null, textHash);

                    canvas.invalidate();

                    canvas.setMode(CanvasView.Mode.POINTER);
                }
            }
        });
    }

    public void clearCanvas(View v) {
        this.canvas.clear();
        canvas.setPaintStrokeWidth(1f);
        canvas.setBlur(0f);
        canvas.setPaintStrokeColor(0xff000000);
        lastColor = 0xff000000;
        canvas.setMode(CanvasView.Mode.DRAW);
    }

    public void pickColor(View v) {
        dialog = new AmbilWarnaDialog(this, lastColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // color is the color selected by the user.
                canvas.setPaintStrokeColor(color);
                lastColor = color;
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // cancel was selected by the user
            }
        });
        dialog.show();
    }

    public void pencil(View v) {
        canvas.setMode(CanvasView.Mode.DRAW);
        canvas.setPaintStrokeWidth(3f);
        canvas.setBlur(0f);
    }

    public void softBrush(View v) {
        canvas.setMode(CanvasView.Mode.DRAW);
        canvas.setPaintStrokeWidth(8f);
        canvas.setBlur(10f);
    }

    public void addImage(View v) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    public void addText(View v) {
        canvas.setMode(CanvasView.Mode.TEXT);
        this.addTextView.setVisibility(View.VISIBLE);
    }

    public void erase(View v) {
        canvas.setPaintStrokeWidth(20f);
        canvas.setBlur(0f);
        canvas.setMode(CanvasView.Mode.ERASER);
    }

    public void point(View v) {
        canvas.setMode(CanvasView.Mode.POINTER);
    }

    public void deleteCanvas(View v) {

    }

    public void undoStep(View v) {
       canvas.undo();
    }

    public void redoStep(View v) {
        canvas.redo();
    }

    public void saveCanvas(View v) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                    Random r = new Random();
                    int i1 = r.nextInt(100);

                    canvas.drawBitmap(bitmap, i1, i1);
                }
        }
    }

}
