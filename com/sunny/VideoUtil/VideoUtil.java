package com.sunny.VideoUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.widget.ImageView;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

@DesignerComponent(version = 1,
        versionName = "1.1",
        description = "An extension to get metadata information from video <br> Developed by Sunny Gupta",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.READ_EXTERNAL_STORAGE,android.permission.WRITE_EXTERNAL_STORAGE")
public final class VideoUtil extends AndroidNonvisibleComponent {
    public Context context;
    public Activity activity;
    public boolean doe = false;
    public VideoUtil(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = (Activity) context;
    }
    @SimpleProperty()
    public void DeleteThumbOnExit(boolean delete){
        doe = delete;
    }
    /*@SimpleFunction(description="Set source of video.This can be either local video path or online video's url")
    public void SetSource(String videoPath){
        if (!videoPath.isEmpty()) {
            if (videoPath.startsWith("http")){
                mediaMetadataRetriever.setDataSource(videoPath,new HashMap<String,String>());
            }else{
                mediaMetadataRetriever.setDataSource(videoPath);
            }
        }

    }*/

    @SimpleFunction(description = "Get thumbnail path of video from source.format can be png,jpeg or webm.quality should be between 1 and 100")
    public String GetThumbnail(String videoPath, String format, int quality, String thumbPath) {
        try {
            return path(videoPath, format, quality, thumbPath);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @SimpleFunction(description = "Sets image's picture to thumbnail asynchronously")
    public void SetImage(final String videoPath, final Object image) {
        AsynchUtil.runAsynchronously(new Runnable() {
            public void run() {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                try {
                    setSource(mediaMetadataRetriever,videoPath);
                    final Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime();
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            ImageView imageView = (ImageView) ((AndroidViewComponent)image).getView();
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mediaMetadataRetriever.release();
                }
            }
        });
    }

    @SimpleFunction(description = "Works same as GetThumbnail but asynchronously")
    public void GetThumbnailAsync(final String videoPath, final String format, final int quality, final String thumbPath) {
        AsynchUtil.runAsynchronously(new Runnable() {
            public void run() {
                final String[] p = new String[1];
                try {
                    p[0] = path(videoPath, format, quality, thumbPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        GotThumbnail(p[0]);
                    }
                });
            }
        });
    }

    @SimpleEvent(description = "Event raised after getting thumbnail asynchronously")
    public void GotThumbnail(String path) {
        EventDispatcher.dispatchEvent(this, "GotThumbnail", path);
    }

    public String path(String videoPath, String format, int quality, String thumbPath) throws Exception {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        setSource(mediaMetadataRetriever,videoPath);
        if (thumbPath.isEmpty()){
            thumbPath = getPath(format);
        }
        File pFile = new File(thumbPath);
        if (doe){
            pFile.deleteOnExit();
        }
        try {
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime();
            if (bitmap != null) {
                if (!pFile.exists()) {
                    boolean mkdirs = pFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(pFile);
                bitmap.compress(Bitmap.CompressFormat.valueOf(format.toUpperCase()), quality, fos);
                return pFile.getPath();
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            mediaMetadataRetriever.release();
        }
    }

    @SimpleFunction(description = "Get custom metadata from id")
    public String CustomMetadata(String videoPath, int id) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            setSource(mediaMetadataRetriever,videoPath);
            String value = mediaMetadataRetriever.extractMetadata(id);
            mediaMetadataRetriever.release();
            return value != null ? value : "";
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            mediaMetadataRetriever.release();
        }
    }
    private void setSource(MediaMetadataRetriever retriever,String source) throws Exception {
        if (source.startsWith("http")){
            retriever.setDataSource(source, new HashMap<String, String>());
        }else {
            retriever.setDataSource(source);
        }
    }
    private String getPath(String format){
        File file = new File(getExternalStoragePath(),"."+context.getApplicationInfo().name+ "thumbnails/" + System.currentTimeMillis()+"."+format.toLowerCase());
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        return file.getAbsolutePath();
    }
    @SuppressWarnings("deprecation")
    public String getExternalStoragePath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.getExternalFilesDir(null).getAbsolutePath();
        } else {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

}
