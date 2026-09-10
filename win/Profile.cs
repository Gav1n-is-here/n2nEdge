using System;
using System.Text;
using System.Text.RegularExpressions;
using System.Security.Cryptography;
using System.Linq;

public class Profile {
 public string Server="",Community="",Secret="",Ip="",Mask="",Mac="",Adapter="",Cipher="AES-CBC";
 public int Mtu=1400; public bool Header=false,Relay=false,Multicast=false;
 public string ConnectionModeLabel(){return Relay?"强制服务器中转":"P2P 优先";}
 public static uint Address(string s) { if(!Regex.IsMatch(s,@"^(0|[1-9]\d{0,2})(\.(0|[1-9]\d{0,2})){3}$"))throw new Exception("请输入有效 IPv4 地址");uint v=0;foreach(string b in s.Split('.')){uint n=uint.Parse(b);if(n>255)throw new Exception("IPv4 每段不能超过 255");v=(v<<8)|n;}return v; }
 public int Prefix(){uint m=Address(Mask),v=~m;if((v&(v+1))!=0)throw new Exception("子网掩码必须连续");int n=0;while(m!=0){n+=(int)(m&1);m>>=1;}if(n<8||n>30)throw new Exception("子网掩码需介于 /8 和 /30");return n;}
 public void Validate(string key){
  var missing=new System.Collections.Generic.List<string>();
  if(string.IsNullOrWhiteSpace(Server))missing.Add("节点地址");
  if(string.IsNullOrWhiteSpace(Community))missing.Add("Community");
  if(string.IsNullOrEmpty(key))missing.Add("共享密钥");
  if(string.IsNullOrWhiteSpace(Ip))missing.Add("虚拟 IP");
  if(string.IsNullOrWhiteSpace(Mask))missing.Add("子网掩码");
  if(string.IsNullOrWhiteSpace(Cipher))missing.Add("加密算法");
  if(missing.Count>0)throw new Exception("请填写："+string.Join("、",missing));
  Endpoint(Server); if(Server.Length>47)throw new Exception("节点地址最长 47 字符");
  if(!Regex.IsMatch(Community,@"^[A-Za-z0-9_.-]{1,19}$"))throw new Exception("社区需为 1–19 位字母、数字、点、下划线或短横线");
  if(string.IsNullOrEmpty(key)||Encoding.UTF8.GetByteCount(key)>128||key.Contains("\0"))throw new Exception("请输入共享密钥，最多 128 字节");
  if(string.IsNullOrWhiteSpace(Ip))throw new Exception("请填写虚拟 IP");
  try{Address(Ip);}catch{throw new Exception("虚拟 IP 格式无效，请填写四段 IPv4 地址");}
  if(string.IsNullOrWhiteSpace(Mask))throw new Exception("请填写子网掩码，例如 255.255.255.0");
  try{Address(Mask);}catch{throw new Exception("子网掩码格式无效，例如 255.255.255.0");}
  Prefix();uint a=Address(Ip),h=a&~Address(Mask);if(h==0||h==~Address(Mask)||(a>>24)==0||(a>>24)==127||(a>>24)>=224)throw new Exception("虚拟 IP 必须是有效主机地址");
  if(Mac!=""&&(!Regex.IsMatch(Mac,@"^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$")||(Convert.ToInt32(Mac.Substring(0,2),16)&1)!=0))throw new Exception("MAC 必须是单播地址");
  if(Mtu<576||Mtu>1500)throw new Exception("MTU 应介于 576–1500");if(!new[]{"AES-CBC","Twofish","ChaCha20","Speck-CTR"}.Contains(Cipher))throw new Exception("不支持的加密算法");
 }
 public static string[] Endpoint(string s){if(string.IsNullOrWhiteSpace(s))throw new Exception("请填写节点地址");if(!Regex.IsMatch(s,@"^[A-Za-z0-9.-]+:\d{1,5}$"))throw new Exception("节点格式应为 主机:端口");string[] b=s.Split(':');int p=int.Parse(b[1]);if(p<1||p>65535)throw new Exception("端口应为 1–65535");return b;}
 public void Encrypt(string key){Secret=Convert.ToBase64String(ProtectedData.Protect(Encoding.UTF8.GetBytes(key),null,DataProtectionScope.CurrentUser));}
 public string Decrypt(){return Secret==""?"":Encoding.UTF8.GetString(ProtectedData.Unprotect(Convert.FromBase64String(Secret),null,DataProtectionScope.CurrentUser));}
}
