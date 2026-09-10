package wang.switchy.hin2n.service;

import android.app.*;
import android.content.Intent;
import android.net.VpnService;
import android.os.*;
import android.system.OsConstants;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import wang.switchy.hin2n.MainActivity;
import wang.switchy.hin2n.Profile;
import wang.switchy.hin2n.R;
import wang.switchy.hin2n.model.EdgeCmd;
import wang.switchy.hin2n.model.EdgeStatus;

public class N2NService extends VpnService {
    public static volatile String status="未连接", detail="连接后即可访问同一虚拟子网内的设备";
    public static volatile boolean active=false;
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private ParcelFileDescriptor tun;
    private Profile profile;
    private boolean engineStarted=false;
    private volatile boolean closing=false;
    private int latestStartId;
    private static final String CHANNEL="n2n-connection";
    static { System.loadLibrary("edge_jni"); }
    public native boolean startEdge(EdgeCmd cmd);
    public native void stopEdge();
    public native EdgeStatus getEdgeStatus();

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager n=getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel(CHANNEL,"子网连接",NotificationManager.IMPORTANCE_LOW));
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop=new Intent(this,N2NService.class).setAction("STOP");
        PendingIntent disconnect=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_network)
            .setContentTitle("n2n edge · "+status).setContentText(detail).setContentIntent(open)
            .setOngoing(true).addAction(new Notification.Action.Builder(null,"断开",disconnect).build()).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        latestStartId=startId;
        if(intent==null || "STOP".equals(intent.getAction())) { disconnect(); return START_NOT_STICKY; }
        if(active || closing) return START_NOT_STICKY;
        active=true; status="正在连接"; detail="正在建立加密子网连接";
        startForeground(1,notification());
        worker.execute(()->{
            try {
                if(closing) return;
                profile=Profile.load(this); profile.validate();
                int fd=EstablishVpnService(profile.ip,profile.prefix());
                if(fd<0) throw new IllegalStateException("VPN 授权失效，请重新连接");
                // Duplicate descriptor is owned by native edge after a successful start.
                boolean started=false;
                try { started=startEdge(profile.command(fd,getFilesDir()+"/edge.log")); }
                finally { if(!started) ParcelFileDescriptor.adoptFd(fd).close(); }
                if(!started) throw new IllegalStateException("n2n 初始化失败");
                engineStarted=true;
            } catch(Exception e) { fail(e.getMessage()==null?"连接失败":e.getMessage()); }
        });
        return START_NOT_STICKY;
    }
    public int EstablishVpnService(String ip,int prefix) {
        try {
            tun=new Builder().setSession("N2N · "+profile.community).setMtu(profile.mtu)
                .addAddress(ip,prefix).addRoute(profile.network(),prefix)
                .allowFamily(OsConstants.AF_INET6).setBlocking(true).establish();
            if(tun==null) return -1;
            return ParcelFileDescriptor.dup(tun.getFileDescriptor()).detachFd();
        } catch(Exception e) { return -1; }
    }
    public void reportEdgeStatus(EdgeStatus edge) {
        main.post(()->{
            if(closing || !active || edge==null) return;
            switch(edge.runningStatus) {
                case CONNECTED:
                    status="已连接节点"; detail=profile.ip+" · "+profile.community+" · 模式："+profile.connectionModeLabel(); break;
                case CONNECTING:
                    status="正在连接"; detail="等待节点响应 · "+profile.server; break;
                case SUPERNODE_DISCONNECT:
                    status="正在重连"; detail="节点暂时不可达，正在重试"; break;
                case FAILED:
                    fail("连接失败，请检查节点地址和网络"); return;
                case DISCONNECT:
                    disconnect(); return;
            }
            getSystemService(NotificationManager.class).notify(1,notification());
        });
    }
    private void fail(String message) {
        status="连接失败"; detail=message;
        disconnectInternal(true);
    }
    private void disconnect() { disconnectInternal(false); }
    private synchronized void disconnectInternal(boolean failed) {
        if(closing) return;
        closing=true;
        if(!failed) { status="正在断开"; detail="正在关闭子网连接"; }
        worker.execute(()->{
            if(engineStarted) { stopEdge(); engineStarted=false; }
            try { if(tun!=null) tun.close(); } catch(Exception ignored) {} finally { tun=null; }
            main.post(()->{
                active=false;
                if(!failed) { status="未连接"; detail="连接后即可访问同一虚拟子网内的设备"; }
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelfResult(latestStartId);
            });
        });
    }
    @Override public void onRevoke() { disconnect(); }
    @Override public void onDestroy() { disconnect(); super.onDestroy(); }
}
