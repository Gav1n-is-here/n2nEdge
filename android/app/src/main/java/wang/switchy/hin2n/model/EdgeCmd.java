package wang.switchy.hin2n.model;

import java.util.Random;
import java.util.Vector;

public class EdgeCmd {
    public int edgeType;    // 0: v1, 1: v2, 2: v2s 3: v3
    public int ipMode;
    public String ipAddr;
    public String ipNetmask;
    public String[] supernodes;
    public String community;
    public String devDesc;
    public String encKey;
    public String encKeyFile;
    public String macAddr;
    public int mtu;
    public String localIP;
    public int holePunchInterval;
    public boolean reResoveSupernodeIP;
    public int localPort;
    public boolean allowRouting;
    public boolean dropMuticast;
    public boolean httpTunnel;
    public int traceLevel;
    public int vpnFd;
    public String gatewayIp;
    public String dnsServer;
    public String logPath;
    public String encryptionMode;
    public boolean headerEnc;
    public boolean forceRelay;

    public EdgeCmd() {}
}
