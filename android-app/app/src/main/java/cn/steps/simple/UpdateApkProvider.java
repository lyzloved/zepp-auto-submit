package cn.steps.simple;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Exposes only the downloaded update APK, through an explicit temporary read grant. */
public final class UpdateApkProvider extends ContentProvider {
    static final String PATH="/update.apk";
    static File apk(Context context){return new File(context.getCacheDir(),"verified-update.apk");}
    static Uri uri(Context context){return Uri.parse("content://"+context.getPackageName()+".updates"+PATH);}
    private void validate(Uri uri){if(!uri.equals(uri(getContext())))throw new SecurityException("无效的更新文件地址");}
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){validate(uri);return "application/vnd.android.package-archive";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
        validate(uri);if(!"r".equals(mode))throw new SecurityException("更新文件只允许读取");
        return ParcelFileDescriptor.open(apk(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        validate(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];
        for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"StepSubmit-update.apk":OpenableColumns.SIZE.equals(columns[i])?apk(getContext()).length():null;
        cursor.addRow(row);return cursor;
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
