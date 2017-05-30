package com.india.apkcrew.babysdrawingapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Message;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_EXTERNAL_STORAGE = 10;
    private PaintView paintView;
    private ImageButton currPaint, thumbNail1, thumbNail2, thumbNail3;
    private MediaPlayer fg_voice;
    Animation rotate, moveRight, moveLeft, zoomIO, fadeIn, slideUD;
    private String saveName, savePath, prevFilePath[];
    private boolean paused = false;
    private int inActivityTime = 0;
    final Context context = this;
    final String albumName = "Baby's Drawings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paintView = (PaintView) findViewById(R.id.drawing);
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors);
        for (int i = 0; i < paintLayout.getChildCount(); ++i) {
            ImageButton c = (ImageButton) paintLayout.getChildAt(i);
            if (i == 0) {
                c.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_selected, null));
                currPaint = c;
                continue;
            }
            c.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_pallete, null));
        }
        ImageButton eraseBtn = (ImageButton) findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);
        ImageButton newBtn = (ImageButton) findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);
        ImageButton undoBtn = (ImageButton) findViewById(R.id.undo_btn);
        undoBtn.setOnClickListener(this);
        ImageButton redoBtn = (ImageButton) findViewById(R.id.redo_btn);
        redoBtn.setOnClickListener(this);
        thumbNail1 = (ImageButton) findViewById(R.id.load1);
        thumbNail1.setOnClickListener(this);
        thumbNail2 = (ImageButton) findViewById(R.id.load2);
        thumbNail2.setOnClickListener(this);
        thumbNail3 = (ImageButton) findViewById(R.id.load3);
        thumbNail3.setOnClickListener(this);
        rotate = AnimationUtils.loadAnimation(this, R.anim.anim_rotate);
        moveLeft = AnimationUtils.loadAnimation(this, R.anim.move_left);
        moveRight = AnimationUtils.loadAnimation(this, R.anim.move_right);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        zoomIO = AnimationUtils.loadAnimation(this, R.anim.zoom_in_and_out);
        slideUD = AnimationUtils.loadAnimation(this, R.anim.slide_up_and_down);
        setMusic(R.raw.m1);
        saveName = setSaveName();
        savePath = setSavePath();
        createThumbNail();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        st.start();
        paused = false;
        Log.i("info", "onResume: autsave called");
        autoSave();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (paintView.drawingChanged) {
                saveDrawing();
                paintView.drawingChanged = false;
                Log.i("info", "handleMessage: autosave called after save");
                autoSave();
                inActivityTime = 0;
            } else if (!paintView.drawingChanging) {
                if (inActivityTime >= 30) {
                    inActivityTime = 0;
                    startVoice(R.raw.inactivity);
                    Log.i("info", "handleMessage: dialog show ");
                    final Dialog timeoutDialog = new Dialog(context);
                    timeoutDialog.setContentView(R.layout.custom_dialog);
                    TextView dialogText = (TextView) timeoutDialog.findViewById(R.id.dialog_text);
                    dialogText.setText(R.string.timeout_msg);
                    ImageButton yesBtn = (ImageButton) timeoutDialog.findViewById(R.id.btn_yes);
                    ImageButton noBtn = (ImageButton) timeoutDialog.findViewById(R.id.btn_no);
                    yesBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startVoice(R.raw.yes);
                            timeoutDialog.dismiss();
                            Log.i("info", "onClick: autosave called in yes");
                            autoSave();
                        }
                    });
                    noBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startVoice(R.raw.no);
                            timeoutDialog.dismiss();
                            paintView.startNew();
                            Log.i("info", "onClick: autosave called in no");
                            autoSave();
                        }
                    });
                    timeoutDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
                    timeoutDialog.show();
                } else {
                    inActivityTime += 10;
                    Log.i("info", "handleMessage: autosave called");
                    autoSave();
                }
            } else {
                inActivityTime = 0;
                autoSave();
            }
        }
    };

    public void autoSave() {
        if (!paused) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Log.i("info", "run: runnable started");
                    long start = System.currentTimeMillis();
                    long end;
                    while (true) {
                        end = System.currentTimeMillis();
                        if (end - start >= 10000)
                            break;
                    }
                    handler.sendEmptyMessage(0);
                }
            };
            Thread myThread = new Thread(runnable);
            myThread.start();
        }
    }


    public void paintClicked(View view) {
        paintView.drawingChanging = true;
        //use chosen color
        if (view.getId() == R.id.rc) {
            startVoice(R.raw.red);
            setMusic(R.raw.m1);
            view.startAnimation(rotate);
        } else if (view.getId() == R.id.yc) {
            startVoice(R.raw.yellow);
            setMusic(R.raw.m2);
            view.startAnimation(rotate);
        } else if (view.getId() == R.id.gc) {
            startVoice(R.raw.green);
            setMusic(R.raw.m3);
            view.startAnimation(rotate);
        } else if (view.getId() == R.id.bc) {
            startVoice(R.raw.blue);
            setMusic(R.raw.m4);
            view.startAnimation(rotate);
        } else if (view.getId() == R.id.pc) {
            startVoice(R.raw.pink);
            setMusic(R.raw.m5);
            view.startAnimation(rotate);
        }
        ImageButton imgView = (ImageButton) view;
        String color = view.getTag().toString();
        paintView.setColor(color);
        if (view != currPaint) {
            //update color
            imgView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_selected, null));
            currPaint.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_pallete, null));
            currPaint = (ImageButton) view;
        }
    }

    @Override
    public void onClick(View view) {
        //respond to clicks
        switch (view.getId()) {
            case R.id.erase_btn:
                eraserClicked(view);
                break;
            case R.id.new_btn:
                newClicked(view);
                break;
            case R.id.undo_btn:
                view.startAnimation(moveLeft);
                if (paintView.canUndo()) {
                    startVoice(R.raw.undo);
                } else {
                    startVoice(R.raw.noundo);
                }
                paintView.onClickUndo();
                break;
            case R.id.redo_btn:
                view.startAnimation(moveRight);
                if (paintView.canRedo()) {
                    startVoice(R.raw.redo);
                } else {
                    startVoice(R.raw.noredo);
                }
                paintView.onClickRedo();
                break;
            case R.id.load1:
                loadClicked(view, 0);
                break;
            case R.id.load2:
                loadClicked(view, 1);
                break;
            case R.id.load3:
                loadClicked(view, 2);
                break;
        }
    }

    private void loadClicked(View view, final int i) {
        paintView.drawingChanging = true;
        view.startAnimation(zoomIO);
        if (prevFilePath[i] == null) {
            startVoice(R.raw.noprev);
            Toast.makeText(this, "No previous drawings", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        startVoice(R.raw.load);
        final Dialog loadDialog = new Dialog(context);
        loadDialog.setContentView(R.layout.custom_dialog);
        TextView dialogText = (TextView) loadDialog.findViewById(R.id.dialog_text);
        dialogText.setText(R.string.load_msg);
        ImageButton yesBtn = (ImageButton) loadDialog.findViewById(R.id.btn_yes);
        ImageButton noBtn = (ImageButton) loadDialog.findViewById(R.id.btn_no);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoice(R.raw.yes);
                loadDialog.dismiss();
                paintView.setBitMap(prevFilePath[i]);
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoice(R.raw.no);
                loadDialog.dismiss();
            }
        });
        loadDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
        loadDialog.show();
    }

    private void newClicked(View view) {
        startVoice(R.raw.clear);
        view.startAnimation(slideUD);
        paintView.drawingChanging = true;
        final Dialog newDialog = new Dialog(context);
        newDialog.setContentView(R.layout.custom_dialog);
        TextView dialogText = (TextView) newDialog.findViewById(R.id.dialog_text);
        ImageButton yesBtn = (ImageButton) newDialog.findViewById(R.id.btn_yes);
        ImageButton noBtn = (ImageButton) newDialog.findViewById(R.id.btn_no);
        dialogText.setText(R.string.new_msg);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoice(R.raw.yes);
                newDialog.dismiss();
                saveDrawing();
                paintView.startNew();
                saveName = setSavePath();
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoice(R.raw.no);
                newDialog.dismiss();
            }
        });
        newDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
        newDialog.show();
    }

    void eraserClicked(View view) {
        startVoice(R.raw.erase);
        ImageButton imgView = (ImageButton) view;
        currPaint.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_pallete, null));
        imgView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.color_selected, null));
        currPaint = imgView;
        paintView.drawingChanging = true;
        view.startAnimation(fadeIn);
        setMusic(R.raw.erasor);
        paintView.setErase(true);
    }

    private void saveDrawing() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // Here, thisActivity is the current activity
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
        } else {
            paintView.setDrawingCacheEnabled(true);
            paintView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            Bitmap bitmap = paintView.getDrawingCache();
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                Toast unsavedToast = Toast.makeText(getApplicationContext(),
                        "Oops! Image could not be saved. ", Toast.LENGTH_SHORT);
                unsavedToast.show();
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast savedToast = Toast.makeText(getApplicationContext(),
                        "Drawing Saved! " + pictureFile.getAbsolutePath(), Toast.LENGTH_SHORT);
                savedToast.show();
            } catch (Exception e) {
                Toast unsavedToast = Toast.makeText(getApplicationContext(),
                        "Oops! Image could not be saved. " + pictureFile.getAbsolutePath(), Toast.LENGTH_SHORT);
                unsavedToast.show();
            }
        }
        paintView.setDrawingCacheEnabled(false);
    }

    public void createThumbNail() {
        if (savePath != null) {
            File files[] = new File(savePath).listFiles();
            prevFilePath = new String[3];
            if (files.length > 0) {
                File file1 = files[0];
                File file2 = null;
                File file3 = null;

                for (int i = 1; i < files.length; i++) {
                    if (file1.lastModified() < files[i].lastModified()) {
                        file3 = file2;
                        file2 = file1;
                        file1 = files[i];
                    } else if ((file2 == null) || (file2.lastModified() < files[i].lastModified())) {
                        file3 = file2;
                        file2 = files[i];
                    } else if ((file3 == null) || (file3.lastModified() < files[i].lastModified())) {
                        file3 = files[i];
                    }
                }
                Log.i("info", "createThumbNail: " + files.length);
                try {
                    Log.i("file", "createThumbNail: " + file1.getName() + " " + file2.getName() + " " + file3.getName());
                } catch (Exception e) {
                    Log.e("file", "createThumbNail: ", e);
                }

                if (file1 != null) {
                    prevFilePath[0] = file1.getAbsolutePath();
                    Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(prevFilePath[0]), 64, 64);
                    thumbNail1.setImageBitmap(ThumbImage);
                }
                if (file2 != null) {
                    prevFilePath[1] = file2.getAbsolutePath();
                    Bitmap ThumbImage2 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(prevFilePath[1]), 64, 64);
                    thumbNail2.setImageBitmap(ThumbImage2);
                }
                if (file3 != null) {
                    prevFilePath[2] = file3.getAbsolutePath();
                    Bitmap ThumbImage3 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(prevFilePath[2]), 64, 64);
                    thumbNail3.setImageBitmap(ThumbImage3);
                }
            }
        }
    }

    @Nullable
    private File getOutputMediaFile() {
        // Create a media file name
        savePath = setSavePath();
        if (savePath != null) {
            File mediaFile;
            mediaFile = new File(savePath + File.separator + saveName);
            return mediaFile;
        }
        return null;
    }

    private String setSaveName() {
        String timeStamp = new SimpleDateFormat("ddMMyy_kkmmss", Locale.getDefault()).format(new Date());
        return "BD_" + timeStamp + ".png";
    }

    @Nullable
    private String setSavePath() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // Here, thisActivity is the current activity
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
        } else {
            if (!isExternalStorageWritable()) {
                return null;
            }
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            return mediaStorageDir.getAbsolutePath();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveDrawing();
                    // permission was granted

                } else {
                    Toast unsavedToast = Toast.makeText(getApplicationContext(),
                            "Permission Denied! Image could not be saved.", Toast.LENGTH_SHORT);
                    unsavedToast.show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }*/

    void startVoice(int voiceId) {
        if (fg_voice != null) {
            fg_voice.release();
        }
        fg_voice = MediaPlayer.create(context, voiceId);
        fg_voice.start();
    }

    void setMusic(int musicId) {
        if (paintView.bg_music != null) {
            paintView.bg_music.release();
        }
        paintView.bg_music = MediaPlayer.create(context, musicId);

    }
}
