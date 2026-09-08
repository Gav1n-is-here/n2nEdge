using System;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Diagnostics;
using System.Net.NetworkInformation;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using System.Xml.Serialization;

public class Tap {public string Id{get;set;} public string Name{get;set;}}
public class MainApp {
 Window w;Profile profile=new Profile();Engine edge;bool stopping,polling,closing;DispatcherTimer timer;DateTime started;
 static string Config=Path.Combine(Engine.Root,"profile.xml");
 T Get<T>(string n) where T:class{return w.FindName(n) as T;}TextBox Box(string n){return Get<TextBox>(n);}
 void Log(string s){if(w.Dispatcher.HasShutdownStarted)return;w.Dispatcher.BeginInvoke(new Action(()=>{var b=Box("Logs");b.AppendText(DateTime.Now.ToString("HH:mm:ss")+"  "+s+Environment.NewLine);if(b.Text.Length>24000)b.Text=b.Text.Substring(b.Text.Length-18000);b.ScrollToEnd();}));}
 void Status(string s,string detail){Get<TextBlock>("Status").Text=s;Get<TextBlock>("Detail").Text=detail;}
 void Busy(bool active){Get<StackPanel>("Settings").IsEnabled=!active;Get<Button>("Save").IsEnabled=!active;Get<Button>("Connect").Content=active?"断开连接":"连接子网";}
 [STAThread] public static void Main(string[] args){try{Directory.CreateDirectory(Engine.Root);var app=new Application();new MainApp().Run(app);}catch(Exception e){MessageBox.Show(e.Message,"n2n edge",MessageBoxButton.OK,MessageBoxImage.Error);}}
 void Run(Application app){using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream("Main.xaml"))w=(Window)XamlReader.Load(s);
 using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream("icon.png")){var bitmap=new BitmapImage();bitmap.BeginInit();bitmap.CacheOption=BitmapCacheOption.OnLoad;bitmap.StreamSource=s;bitmap.EndInit();w.Icon=bitmap;Get<Image>("BrandIcon").Source=bitmap;}
 try{if(File.Exists(Config))using(var s=File.OpenRead(Config))profile=(Profile)new XmlSerializer(typeof(Profile)).Deserialize(s);}catch(Exception e){Log("配置读取失败："+e.Message);}
 Box("Server").Text=profile.Server;Box("Community").Text=profile.Community;Box("Ip").Text=profile.Ip;Box("Mask").Text=profile.Mask;Box("Mac").Text=profile.Mac;Box("Mtu").Text=profile.Mtu.ToString();Get<ComboBox>("Cipher").ItemsSource=new[]{"AES-CBC","Twofish","ChaCha20","Speck-CTR"};Get<ComboBox>("Cipher").SelectedItem=profile.Cipher;Get<CheckBox>("Header").IsChecked=profile.Header;Get<CheckBox>("Relay").IsChecked=profile.Relay;Get<CheckBox>("Multicast").IsChecked=profile.Multicast;
 try{Get<PasswordBox>("Key").Password=profile.Decrypt();}catch{Log("保存的密钥无法解密，请重新填写。");}Refresh();Get<TextBlock>("Architecture").Text="WINDOWS · "+(IntPtr.Size==8?"x64":"x86");
 Get<Button>("Refresh").Click+=(s,e)=>Refresh();Get<Button>("Save").Click+=(s,e)=>{try{Read();Save();Status("配置已保存","密钥已由 Windows 当前账户加密保存");}catch(Exception ex){Status("请检查配置",ex.Message);Log(ex.Message);}};
 Get<Button>("Connect").Click+=async(s,e)=>{if(edge!=null)await Stop();else Start();};
 Get<Button>("Probe").Click+=async(s,e)=>{var b=Get<Button>("Probe");b.IsEnabled=false;try{Log(await Engine.Probe(Box("Server").Text.Trim()));}catch(Exception ex){Log("节点检测失败："+ex.Message);}finally{b.IsEnabled=true;}};
 Get<Button>("Copy").Click+=(s,e)=>{try{Clipboard.SetText(Box("Logs").Text);}catch(Exception ex){Log(ex.Message);}};
 Get<Button>("Driver").Click+=async(s,e)=>{try{var p=Process.Start(new ProcessStartInfo(Engine.Extract("tap-driver.exe")){UseShellExecute=true});await Task.Run(()=>p.WaitForExit());Refresh();Log("驱动安装程序已结束。");}catch(Exception ex){Log("驱动安装失败："+ex.Message);}};
 timer=new DispatcherTimer{Interval=TimeSpan.FromSeconds(2)};timer.Tick+=async(s,e)=>await Poll();timer.Start();
 w.Closing+=async(s,e)=>{if(edge!=null&&!closing){e.Cancel=true;closing=true;await Stop();w.Close();}else timer.Stop();};
 Log("n2n 3.0 / Windows x64；请选用专用 TAP 网卡。检测节点检查公网注册，不能验证共享密钥。");app.Run(w);
 }
 void Refresh(){string selected=Get<ComboBox>("Adapter").SelectedItem is Tap?((Tap)Get<ComboBox>("Adapter").SelectedItem).Id:profile.Adapter;var list=NetworkInterface.GetAllNetworkInterfaces().Where(n=>n.Description.IndexOf("TAP",StringComparison.OrdinalIgnoreCase)>=0).Select(n=>new Tap{Id=n.Id,Name=n.Name+" · "+n.Description}).ToList();Get<ComboBox>("Adapter").ItemsSource=list;Get<ComboBox>("Adapter").SelectedItem=list.FirstOrDefault(t=>t.Id==selected);if(list.Count==0)Log("未找到 TAP 网卡，请安装 TAP 驱动后刷新。");}
 void Read(){
 var missing=new System.Collections.Generic.List<string>();
 string[] fields={"Server","Community","Ip","Mask","Mtu"};string[] labels={"节点地址","Community","虚拟 IP","子网掩码","MTU"};
 for(int i=0;i<fields.Length;i++)if(string.IsNullOrWhiteSpace(Box(fields[i]).Text))missing.Add(labels[i]);
 if(Get<PasswordBox>("Key").Password.Length==0)missing.Add("共享密钥");
 if(Get<ComboBox>("Cipher").SelectedItem==null)missing.Add("加密算法");
 if(missing.Count>0)throw new Exception("请填写："+string.Join("、",missing));
 profile.Server=Box("Server").Text.Trim();profile.Community=Box("Community").Text.Trim();profile.Ip=Box("Ip").Text.Trim();profile.Mask=Box("Mask").Text.Trim();profile.Mac=Box("Mac").Text.Trim();int mtu;if(!int.TryParse(Box("Mtu").Text,out mtu))throw new Exception("MTU 必须是整数");profile.Mtu=mtu;profile.Cipher=(string)Get<ComboBox>("Cipher").SelectedItem;profile.Header=Get<CheckBox>("Header").IsChecked==true;profile.Relay=Get<CheckBox>("Relay").IsChecked==true;profile.Multicast=Get<CheckBox>("Multicast").IsChecked==true;profile.Adapter=Get<ComboBox>("Adapter").SelectedItem is Tap?((Tap)Get<ComboBox>("Adapter").SelectedItem).Id:"";profile.Validate(Get<PasswordBox>("Key").Password);profile.Encrypt(Get<PasswordBox>("Key").Password);}
 void Save(){string temp=Config+".new";using(var s=File.Create(temp))new XmlSerializer(typeof(Profile)).Serialize(s,profile);if(File.Exists(Config))File.Replace(temp,Config,null);else File.Move(temp,Config);}
 void Start(){try{Read();Save();edge=new Engine();edge.Log=Log;edge.Start(profile,Get<PasswordBox>("Key").Password);started=DateTime.UtcNow;Busy(true);Status("正在连接","等待节点确认 · "+profile.Server);}catch(Exception e){if(edge!=null)edge.Dispose();edge=null;Busy(false);Status("连接失败",e.Message);Log(e.Message);}}
 async Task Poll(){if(edge==null||stopping||polling)return;polling=true;var current=edge;try{if(!current.Running){current.Dispose();edge=null;Busy(false);Status("已断开","连接进程退出，请查看日志");return;}bool connected=await current.Connected();if(edge!=current||stopping)return;if(connected)Status("已连接节点",profile.Ip+"/"+profile.Prefix()+" · "+profile.Community+" · "+profile.Cipher);else Status("正在连接",(DateTime.UtcNow-started).TotalSeconds>20?"节点暂未回应，正在重试":"等待节点确认 · "+profile.Server);}catch{if(edge==current&&!stopping&&(DateTime.UtcNow-started).TotalSeconds>20)Status("正在连接","暂未收到本地核心状态，请查看日志");}finally{polling=false;}}
 async Task Stop(){if(edge==null||stopping)return;stopping=true;Get<Button>("Connect").IsEnabled=false;try{await edge.Stop();Log("连接已停止");}catch(Exception e){Log(e.Message);}finally{edge=null;stopping=false;Busy(false);Get<Button>("Connect").IsEnabled=true;Status("未连接","连接已断开");}}
}
