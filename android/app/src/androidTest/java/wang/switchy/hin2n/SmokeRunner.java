package wang.switchy.hin2n;

import android.app.Instrumentation;
import android.content.*;
import android.net.VpnService;
import android.os.Bundle;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import wang.switchy.hin2n.service.N2NService;

/** Explicitly invoked integration checks; never shipped in the application APK. */
public class SmokeRunner extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle b){args=b;start();}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void invalid(Profile p){try{p.validate();throw new AssertionError("Invalid config accepted");}catch(IllegalArgumentException expected){}}
    @Override public void onStart(){
        Bundle result=new Bundle();Context context=getTargetContext();
        try{
            if(args.getString("restore","false").equals("true")){
                context.getSharedPreferences("profile",0).edit().clear().commit();
                result.putString("result","PASS: test configuration cleared");finish(-1,result);return;
            }
            if(args.getString("network","false").equals("true"))
                check(args.containsKey("key") && !args.getString("key").isEmpty(),"Provide the test network key using -e key");
            Profile p=Profile.load(context);p.key=args.getString("key","validation-only-key");p.community=args.getString("community","n2n-smoke");
            p.ip="10.77.0.21";p.mask="255.255.255.0";p.server=args.getString("server","10.0.2.2:17777");p.validate();
            check(p.network().equals("10.77.0.0") && p.prefix()==24,"CIDR calculation");
            p.mask="";
            try { p.validate(); throw new AssertionError("Empty mask accepted"); }
            catch(IllegalArgumentException e) { check(e.getMessage().contains("子网掩码"),"Empty mask has wrong error message"); }
            p.mask="255.0.255.0";invalid(p);p.mask="255.255.255.0";
            p.ip="10.77.0.0";invalid(p);p.ip="10.77.0.255";invalid(p);p.ip="10.77.0.21";
            String endpoint=p.server;p.server="host:65536";invalid(p);p.server=endpoint;
            p.save(context);check(Profile.load(context).key.equals(p.key),"Keystore round trip");
            check(!context.getSharedPreferences("profile",0).getString("secret","").contains(p.key),"Plaintext key stored");
            result.putString("validation","PASS: CIDR, host address, port, encrypted settings");
            if(args.getString("probe","false").equals("true")){
                java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(79);
                b.put((byte)3).put((byte)2).putShort((short)5);
                byte[] name=new byte[20];System.arraycopy("probe-mobile".getBytes(StandardCharsets.US_ASCII),0,name,0,12);b.put(name);
                b.putInt(123).put(new byte[]{2,0,0,0,0,99}).put(new byte[]{10,78,0,22}).put((byte)24);
                b.put(new byte[16]).putShort((short)1).putShort((short)16).put(new byte[16]).putInt(0);
                try(DatagramSocket s=new DatagramSocket()){
                    s.setSoTimeout(5000);s.send(new DatagramPacket(b.array(),79,InetAddress.getByName("10.0.2.2"),17777));
                    DatagramPacket reply=new DatagramPacket(new byte[2048],2048);s.receive(reply);
                    result.putString("udpProbe","PASS: "+reply.getLength()+" bytes from "+reply.getAddress());
                }
            }
            if(args.getString("network","false").equals("true")){
                check(VpnService.prepare(context)==null,"Approve VPN in the test emulator before network checks");
                startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                context.startForegroundService(new Intent(context,N2NService.class).setAction("START"));
                long deadline=System.currentTimeMillis()+45000;
                while(System.currentTimeMillis()<deadline && !N2NService.status.equals("已连接节点"))Thread.sleep(200);
                check(N2NService.status.equals("已连接节点"),"Registration timeout: "+N2NService.status);
                result.putString("registration","PASS: received supernode registration acknowledgment");
                try(Socket socket=new Socket()){
                    socket.connect(new InetSocketAddress("10.77.0.10",18800),15000);socket.setSoTimeout(15000);
                    socket.getOutputStream().write("n2n-android-smoke\n".getBytes(StandardCharsets.UTF_8));
                    String reply=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)).readLine();
                    check("n2n-android-smoke".equals(reply),"TCP payload mismatch");
                }
                result.putString("tcp","PASS: encrypted Android to Linux TCP echo via n2n");
                context.startService(new Intent(context,N2NService.class).setAction("STOP"));
                deadline=System.currentTimeMillis()+10000;
                while(N2NService.active && System.currentTimeMillis()<deadline)Thread.sleep(100);
                check(!N2NService.active,"Disconnect timeout");
                result.putString("disconnect","PASS");
            }
            result.putString("result","PASS");finish(-1,result);
        }catch(Throwable e){result.putString("result","FAIL: "+e);finish(0,result);}
    }
}
