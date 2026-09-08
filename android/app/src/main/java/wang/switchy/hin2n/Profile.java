package wang.switchy.hin2n;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import wang.switchy.hin2n.model.EdgeCmd;

public class Profile {
    public String server="", community="", key="", ip="",
        mask="", mac="", cipher="AES-CBC";
    public int mtu=1400;
    public boolean header=false;

    public static Profile load(Context context) {
        SharedPreferences p=context.getSharedPreferences("profile",0);
        Profile v=new Profile();
        v.server=p.getString("server",v.server); v.community=p.getString("community",v.community);
        v.ip=p.getString("ip",v.ip); v.mask=p.getString("mask",v.mask);
        v.mac=p.getString("mac",""); v.mtu=p.getInt("mtu",1400);
        v.cipher=p.getString("cipher","AES-CBC"); v.header=p.getBoolean("header",false);
        if(v.mac.isEmpty()) { byte[] b=new byte[6]; new SecureRandom().nextBytes(b); b[0]=(byte)((b[0]&0xfc)|2);
            v.mac=String.format(Locale.ROOT,"%02x:%02x:%02x:%02x:%02x:%02x",b[0],b[1],b[2],b[3],b[4],b[5]); }
        String encrypted=p.getString("secret","");
        if(!encrypted.isEmpty()) try {
            String[] bits=encrypted.split(":"); Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,storageKey(),new GCMParameterSpec(128,Base64.decode(bits[0],Base64.NO_WRAP)));
            v.key=new String(c.doFinal(Base64.decode(bits[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
        } catch(Exception ignored) { v.key=""; }
        return v;
    }
    private static SecretKey storageKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(ks.containsAlias("n2n-profile")) return (SecretKey)ks.getKey("n2n-profile",null);
        KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder("n2n-profile",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return g.generateKey();
    }
    public void save(Context context) throws Exception {
        validate(); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,storageKey());
        String encrypted=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+
            Base64.encodeToString(c.doFinal(key.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        boolean ok=context.getSharedPreferences("profile",0).edit().putString("server",server)
            .putString("community",community).putString("ip",ip).putString("mask",mask).putString("mac",mac)
            .putString("cipher",cipher).putBoolean("header",header).putInt("mtu",mtu).putString("secret",encrypted).commit();
        if(!ok) throw new IllegalStateException("无法保存配置");
    }
    public static long ipv4(String value) {
        if(!value.matches("(0|[1-9][0-9]{0,2})(\\.(0|[1-9][0-9]{0,2})){3}")) throw new IllegalArgumentException("请输入有效的 IPv4 地址");
        long result=0;
        for(String s:value.split("\\.")) { int n=Integer.parseInt(s); if(n>255) throw new IllegalArgumentException("IPv4 每段不能超过 255"); result=(result<<8)|n; }
        return result;
    }
    public int prefix() {
        long m=ipv4(mask), inv=(~m)&0xffffffffL;
        if((inv&(inv+1))!=0) throw new IllegalArgumentException("子网掩码必须连续");
        int n=Long.bitCount(m);
        if(n<8 || n>30) throw new IllegalArgumentException("子网掩码需介于 /8 和 /30");
        return n;
    }
    public String network() { return address(ipv4(ip)&ipv4(mask)); }
    public static String address(long n) { return String.format(Locale.ROOT,"%d.%d.%d.%d",n>>>24,(n>>>16)&255,(n>>>8)&255,n&255); }
    public void validate() {
        java.util.ArrayList<String> missing=new java.util.ArrayList<>();
        if(server.trim().isEmpty()) missing.add("节点地址");
        if(community.trim().isEmpty()) missing.add("Community");
        if(key.isEmpty()) missing.add("共享密钥");
        if(ip.trim().isEmpty()) missing.add("虚拟 IP");
        if(mask.trim().isEmpty()) missing.add("子网掩码");
        if(mac.trim().isEmpty()) missing.add("MAC 地址");
        if(cipher.trim().isEmpty()) missing.add("加密算法");
        if(!missing.isEmpty()) throw new IllegalArgumentException("请填写："+String.join("、",missing));
        if(!server.matches("[A-Za-z0-9.-]+:[0-9]{1,5}") || server.length()>47) throw new IllegalArgumentException("节点格式应为 主机:端口，最长 47 字符");
        int port=Integer.parseInt(server.substring(server.lastIndexOf(':')+1));
        if(port<1 || port>65535) throw new IllegalArgumentException("端口应介于 1–65535");
        if(!community.matches("[A-Za-z0-9_.-]{1,19}")) throw new IllegalArgumentException("Community 需为 1–19 位字母、数字、点、下划线或短横线");
        if(key.isEmpty() || key.getBytes(StandardCharsets.UTF_8).length>128 || key.indexOf('\0')>=0) throw new IllegalArgumentException("请填写所有设备共用的密钥，最多 128 字节");
        if(ip.trim().isEmpty()) throw new IllegalArgumentException("请填写虚拟 IP");
        try { ipv4(ip); } catch(IllegalArgumentException e) { throw new IllegalArgumentException("虚拟 IP 格式无效，请填写四段 IPv4 地址"); }
        if(mask.trim().isEmpty()) throw new IllegalArgumentException("请填写子网掩码，例如 255.255.255.0");
        try { ipv4(mask); } catch(IllegalArgumentException e) { throw new IllegalArgumentException("子网掩码格式无效，例如 255.255.255.0"); }
        prefix(); long addr=ipv4(ip), host=addr&(~ipv4(mask)&0xffffffffL);
        if(host==0 || host==(~ipv4(mask)&0xffffffffL) || (addr>>>24)==0 || (addr>>>24)==127 || (addr>>>24)>=224)
            throw new IllegalArgumentException("请使用有效主机 IP，不能是网络、广播或回环地址");
        if(!mac.matches("[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}") || (Integer.parseInt(mac.substring(0,2),16)&1)!=0)
            throw new IllegalArgumentException("请输入有效的单播 MAC 地址");
        if(mtu<576 || mtu>1500) throw new IllegalArgumentException("MTU 应介于 576–1500");
        if(!java.util.Arrays.asList("AES-CBC","Twofish","Speck-CTR","ChaCha20").contains(cipher)) throw new IllegalArgumentException("不支持的加密算法");
    }
    public EdgeCmd command(int fd,String log) {
        EdgeCmd c=new EdgeCmd(); c.edgeType=3; c.ipMode=0; c.ipAddr=ip; c.ipNetmask=mask;
        c.supernodes=new String[]{server,""}; c.community=community; c.encKey=key; c.encKeyFile="";
        c.devDesc="Android"; c.macAddr=mac; c.mtu=mtu; c.localIP=""; c.holePunchInterval=25;
        c.reResoveSupernodeIP=true; c.localPort=0; c.allowRouting=false; c.dropMuticast=false;
        c.httpTunnel=false; c.traceLevel=2; c.vpnFd=fd; c.gatewayIp=""; c.dnsServer="";
        c.logPath=log; c.encryptionMode=cipher; c.headerEnc=header; return c;
    }
}
