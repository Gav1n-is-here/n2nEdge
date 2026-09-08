using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Web.Script.Serialization;

public class Engine : IDisposable {
 public static readonly string Root=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"N2nEdge");
 Process process;IntPtr job;int port;string password,key;public Action<string> Log=delegate{};
 public bool Running {get{return process!=null&&!process.HasExited;}}
 public static string Extract(string name){string folder=Path.Combine(Root,"bin-0.3.0-"+(IntPtr.Size==8?"x64":"x86"));Directory.CreateDirectory(folder);string path=Path.Combine(folder,name);byte[] data;using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream(name)){if(s==null)throw new Exception("缺少内置资源 "+name);using(var m=new MemoryStream()){s.CopyTo(m);data=m.ToArray();}}if(!File.Exists(path)||!File.ReadAllBytes(path).SequenceEqual(data))File.WriteAllBytes(path,data);return path;}
 static string Q(string s){return "\""+s.Replace("\"","\\\"")+"\"";}
 public void Start(Profile p,string secret){if(process!=null)throw new Exception("连接尚未停止");p.Validate(secret);if(p.Adapter=="")throw new Exception("请选择 TAP 虚拟网卡");key=secret;using(var u=new UdpClient(new IPEndPoint(IPAddress.Loopback,0)))port=((IPEndPoint)u.Client.LocalEndPoint).Port;password=Guid.NewGuid().ToString("N");
 int cipher=new Dictionary<string,int>{{"AES-CBC",3},{"Twofish",2},{"ChaCha20",4},{"Speck-CTR",5}}[p.Cipher];string args="-l "+Q(p.Server)+" -c "+Q(p.Community)+" -a "+Q(p.Ip+"/"+p.Prefix())+" -d "+Q(p.Adapter)+" -A"+cipher+" -M "+p.Mtu+" -t "+port+" --management-password "+password;
 if(p.Mac!="")args+=" -m "+Q(p.Mac);if(p.Header)args+=" -H";if(p.Relay)args+=" -S1";if(p.Multicast)args+=" -E";
 var info=new ProcessStartInfo(Extract("edge.exe"),args){UseShellExecute=false,CreateNoWindow=true,RedirectStandardOutput=true,RedirectStandardError=true,StandardOutputEncoding=Encoding.Default,StandardErrorEncoding=Encoding.Default};info.EnvironmentVariables["N2N_KEY"]=secret;
 process=new Process{StartInfo=info};process.OutputDataReceived+=(s,e)=>{if(e.Data!=null)Log(e.Data.Replace(key,"[密钥]"));};process.ErrorDataReceived+=(s,e)=>{if(e.Data!=null)Log(e.Data.Replace(key,"[密钥]"));};
 try{job=NewJob();process.Start();if(!AssignProcessToJobObject(job,process.Handle))throw new Exception("无法建立连接进程退出保护");process.BeginOutputReadLine();process.BeginErrorReadLine();}catch{Dispose();throw;}
 }
 public async Task<bool> Connected(){if(!Running)return false;var times=await Query("r time timestamps");if(!times.Any(r=>r.ContainsKey("last_super")&&Convert.ToInt64(r["last_super"])>0))return false;var rows=await Query("r status supernodes");return rows.Any(r=>r.ContainsKey("current")&&Convert.ToInt32(r["current"])==1&&true);}
 async Task<List<Dictionary<string,object>>> Query(string command){int currentPort=port;return await Task.Run(()=>{var rows=new List<Dictionary<string,object>>();using(var u=new UdpClient()){u.Connect(IPAddress.Loopback,currentPort);u.Client.ReceiveTimeout=900;byte[] request=Encoding.ASCII.GetBytes(command+"\n");u.Send(request,request.Length);for(int i=0;i<128;i++){IPEndPoint ep=null;var data=u.Receive(ref ep);var row=new JavaScriptSerializer().Deserialize<Dictionary<string,object>>(Encoding.UTF8.GetString(data));rows.Add(row);if(row.ContainsKey("_type")&&(string)row["_type"]=="end")break;}}return rows;});}
 public async Task Stop(){if(process==null)return;try{if(Running){try{await Query("w stop:1:"+password+" stop");}catch{}await Task.Run(()=>process.WaitForExit(3000));}}finally{Dispose();}}
 public void Dispose(){if(process!=null){try{if(!process.HasExited)process.Kill();}catch{}process.Dispose();process=null;}if(job!=IntPtr.Zero){CloseHandle(job);job=IntPtr.Zero;}}
 public static async Task<string> Probe(string server){string[] b=Profile.Endpoint(server);return await Task.Run(()=>{var ip=Dns.GetHostAddresses(b[0]).FirstOrDefault(v=>v.AddressFamily==AddressFamily.InterNetwork);if(ip==null)throw new Exception("没有可用的 IPv4 地址");byte[] p=new byte[79];p[0]=3;p[1]=2;p[3]=5;Encoding.ASCII.GetBytes("probe-windows").CopyTo(p,4);byte[] cookie=new byte[4];using(var rng=RandomNumberGenerator.Create())rng.GetBytes(cookie);cookie.CopyTo(p,24);p[28]=2;p[33]=99;p[34]=10;p[35]=78;p[37]=22;p[38]=24;p[56]=1;p[58]=16;using(var u=new UdpClient()){u.Connect(ip,int.Parse(b[1]));u.Client.ReceiveTimeout=4000;var watch=Stopwatch.StartNew();u.Send(p,p.Length);IPEndPoint ep=null;byte[] r=u.Receive(ref ep);if(r.Length<28||r[0]!=3||(r[3]&31)!=7||!r.Skip(24).Take(4).SequenceEqual(cookie))throw new Exception("收到非预期注册响应");return "节点可达：REGISTER_SUPER_ACK · "+watch.ElapsedMilliseconds+" ms · "+r.Length+" 字节（未验证社区和密钥）";}});}
 [StructLayout(LayoutKind.Sequential)] struct BASIC{public long PerProcess,PerJob;public uint Flags;public UIntPtr Min,Max;public uint Active;public UIntPtr Affinity;public uint Priority,Scheduling;}
 [StructLayout(LayoutKind.Sequential)] struct IO{public ulong R1,R2,R3,R4,R5,R6;}
 [StructLayout(LayoutKind.Sequential)] struct EXTENDED{public BASIC Basic;public IO Io;public UIntPtr P1,P2,P3,P4;}
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] static extern IntPtr CreateJobObject(IntPtr a,string n);
 [DllImport("kernel32.dll")] static extern bool SetInformationJobObject(IntPtr j,int c,ref EXTENDED info,int size);
 [DllImport("kernel32.dll")] static extern bool AssignProcessToJobObject(IntPtr j,IntPtr p);
 [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
 static IntPtr NewJob(){IntPtr j=CreateJobObject(IntPtr.Zero,null);var info=new EXTENDED();info.Basic.Flags=0x2000;if(j==IntPtr.Zero||!SetInformationJobObject(j,9,ref info,Marshal.SizeOf(info)))throw new Exception("无法创建退出保护");return j;}
}
