package cn.steps.simple;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONArray;

final class AppUpdater {
    static final int INSTALL_PERMISSION=4509;
    private final Activity activity;
    private final Button button;
    private final TextView status;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean busy,foreground,destroyed,pending,awaitingPermission;
    AppUpdater(Activity activity,Button button,TextView status){this.activity=activity;this.button=button;this.status=status;}
    void resume(){foreground=true;if(pending&&!awaitingPermission)install();}
    void pause(){foreground=false;}
    void close(){destroyed=true;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
    void check(){
        if(busy||destroyed)return;if(pending){install();return;}
        busy=true;button.setEnabled(false);button.setText("检查中…");status.setText("正在查询 GitHub 最新版本…");
        worker.execute(()->{
            try{
                UpdateProtocol.Release latest=UpdateProtocol.latest(new JSONArray(readIndex()));
                if(latest==null)throw new IOException("仓库暂未提供可识别的安装包");
                PackageInfo installed=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
                if(UpdateProtocol.compare(latest.version,installed.versionName)<=0){
                    main.post(()->{if(destroyed)return;finish();status.setText("已是最新版");Toast.makeText(activity,"已是最新版",Toast.LENGTH_SHORT).show();});return;
                }
                main.post(()->{if(!destroyed){button.setText("下载中…");status.setText("发现 v"+latest.version+"，正在下载安装包…");}});
                download(latest);
                verify(latest.version);
                main.post(()->{if(destroyed)return;finish();pending=true;status.setText("安装包已下载并验证，等待系统安装确认");if(foreground)install();});
            }catch(Exception e){
                final String message=e.getMessage()==null?"请检查网络后重试":e.getMessage();
                main.post(()->{if(destroyed)return;finish();status.setText("更新未完成："+message);Toast.makeText(activity,"更新未完成，请查看提示",Toast.LENGTH_LONG).show();});
            }
        });
    }
    private void finish(){busy=false;button.setEnabled(true);button.setText("检查更新");}
    private HttpURLConnection connect(String address)throws Exception {
        URL url=new URL(address);
        Set<String> hosts=new HashSet<>(Arrays.asList("api.github.com","raw.githubusercontent.com","github.com","objects.githubusercontent.com","release-assets.githubusercontent.com"));
        for(int redirect=0;redirect<=4;redirect++){
            if(!"https".equals(url.getProtocol())||!hosts.contains(url.getHost())||url.getUserInfo()!=null)throw new IOException("下载地址不受支持");
            HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent","StepSubmit-Updater");
            int code=c.getResponseCode();
            if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("下载重定向无效");url=new URL(url,location);continue;}
            if(code!=200){c.disconnect();throw new IOException(code==403||code==429?"GitHub 访问受限，请稍后再试":"GitHub 返回 HTTP "+code);}
            return c;
        }
        throw new IOException("下载重定向次数过多");
    }
    private String readIndex()throws Exception {
        HttpURLConnection c=connect(UpdateProtocol.INDEX);
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1){if(out.size()+count>2*1024*1024)throw new IOException("版本列表过大");out.write(buffer,0,count);}
            return out.toString("UTF-8");
        }finally{c.disconnect();}
    }
    private void download(UpdateProtocol.Release latest)throws Exception {
        HttpURLConnection c=connect(latest.url);File part=new File(activity.getCacheDir(),"update-download.part");
        try{
            long expected=c.getContentLength();if(expected>64*1024*1024)throw new IOException("安装包超过下载大小限制");
            long total=0;int last=-1;
            try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part)){
                byte[] buffer=new byte[16384];int count;
                while((count=in.read(buffer))!=-1){
                    if(Thread.currentThread().isInterrupted())throw new IOException("下载已停止");
                    total+=count;if(total>64*1024*1024)throw new IOException("安装包超过下载大小限制");out.write(buffer,0,count);
                    int percent=expected>0?(int)(total*100/expected):-1;
                    if(percent>=0&&percent/5!=last/5){last=percent;final int value=percent;main.post(()->{if(!destroyed)status.setText("正在下载 v"+latest.version+"："+Math.min(100,value)+"%");});}
                }
                out.getFD().sync();
            }
            if(total==0||(expected>=0&&total!=expected))throw new IOException("安装包下载不完整");
            File target=UpdateApkProvider.apk(activity);if(target.exists()&&!target.delete())throw new IOException("无法替换本机更新缓存");
            if(!part.renameTo(target))throw new IOException("无法保存更新缓存");
        }finally{c.disconnect();if(part.exists())part.delete();}
    }
    @SuppressWarnings("deprecation")
    private void verify(String expectedVersion)throws Exception {
        PackageManager pm=activity.getPackageManager();PackageInfo current=pm.getPackageInfo(activity.getPackageName(),PackageManager.GET_SIGNATURES);
        PackageInfo update=pm.getPackageArchiveInfo(UpdateApkProvider.apk(activity).getAbsolutePath(),PackageManager.GET_SIGNATURES);
        if(update==null||!current.packageName.equals(update.packageName)||!expectedVersion.equals(update.versionName))throw new IOException("安装包版本或包名不匹配");
        long oldCode=Build.VERSION.SDK_INT>=28?current.getLongVersionCode():current.versionCode;
        long newCode=Build.VERSION.SDK_INT>=28?update.getLongVersionCode():update.versionCode;
        if(newCode<=oldCode)throw new IOException("安装包版本号未高于当前版本");
        if(current.signatures==null||current.signatures.length==0||update.signatures==null||!Arrays.equals(current.signatures,update.signatures))throw new IOException("安装包签名与当前应用不一致，已停止安装");
    }
    private void install(){
        if(!pending||destroyed||!foreground||awaitingPermission)return;
        try{
            if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                awaitingPermission=true;status.setText("请允许此应用安装更新，返回后继续系统安装");
                activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())),INSTALL_PERMISSION);return;
            }
            Uri uri=UpdateApkProvider.uri(activity);
            Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("StepSubmit update",uri));
            activity.startActivity(intent);pending=false;status.setText("已打开系统安装页面，请确认安装");
        }catch(Exception e){awaitingPermission=false;status.setText("无法打开系统安装页面，请点击检查更新重试");}
    }
    void permissionReturned(){
        awaitingPermission=false;
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){pending=false;status.setText("尚未允许安装更新，点击检查更新可重试");}
        else {foreground=true;install();}
    }
}
