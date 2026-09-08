using System;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Reflection;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using System.Windows.Media;
using System.Windows.Media.Imaging;

class Smoke {
 [STAThread] static void Main(string[] args){string output=Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"smoke-result.txt");try{if(args.Length>0&&args[0]=="network"){if(args.Length<3)throw new ArgumentException("Usage: Smoke.exe network <adapter> <server:port>; set N2N_TEST_KEY");Network(args[1],args[2]);File.WriteAllText(output,"PASS: Windows n2n registration, encrypted TCP echo, disconnect");return;}
 var p=new Profile();if(p.Server!=""||p.Community!=""||p.Ip!=""||p.Mask!=""||p.Secret!="")throw new Exception("Prefilled defaults");p.Server="192.0.2.1:7777";p.Community="smoke";p.Ip="10.77.0.30";p.Mask="255.255.255.0";p.Validate("测试-key");p.Encrypt("测试-key");if(p.Decrypt()!="测试-key"||p.Secret.Contains("测试-key"))throw new Exception("DPAPI failure");
 foreach(string mask in new[]{"255.0.255.0","255.255.255.255"}){p.Mask=mask;bool rejected=false;try{p.Validate("key");}catch{rejected=true;}if(!rejected)throw new Exception("Invalid CIDR accepted");}
 var asm=typeof(Engine).Assembly;Window w;using(var stream=asm.GetManifestResourceStream("Main.xaml"))w=(Window)XamlReader.Load(stream);foreach(string field in new[]{"Server","Community","Ip","Mask"})if(((TextBox)w.FindName(field)).Text!="")throw new Exception("UI prefilled");
 using(var stream=asm.GetManifestResourceStream("icon.png")){var bmp=new BitmapImage();bmp.BeginInit();bmp.CacheOption=BitmapCacheOption.OnLoad;bmp.StreamSource=stream;bmp.EndInit();((Image)w.FindName("BrandIcon")).Source=bmp;}
 ((PasswordBox)w.FindName("Key")).Password="mask-check";
 var selector=(ComboBox)w.FindName("Adapter");selector.ItemsSource=new[]{new Tap{Id="test",Name="TAP-Windows Adapter V9"}};selector.SelectedIndex=0;
 var content=(FrameworkElement)w.Content;content.Measure(new Size(880,1050));content.Arrange(new Rect(0,0,880,1050));content.UpdateLayout();var image=new RenderTargetBitmap(880,1050,96,96,PixelFormats.Pbgra32);image.Render(content);var encoder=new PngBitmapEncoder();encoder.Frames.Add(BitmapFrame.Create(image));using(var stream=File.Create(Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"ui.png")))encoder.Save(stream);
 string native=Engine.Extract("edge.exe");
 var psi=new System.Diagnostics.ProcessStartInfo(native,"--help"){UseShellExecute=false,CreateNoWindow=true,RedirectStandardOutput=true};
 using(var proc=System.Diagnostics.Process.Start(psi)){string help=proc.StandardOutput.ReadToEnd();proc.WaitForExit();if(proc.ExitCode!=0||!help.Contains(IntPtr.Size==8?"Windows-x64":"Windows-x86"))throw new Exception("Embedded core architecture/startup failed");}
 if(!File.Exists(Engine.Extract("tap-driver.exe")))throw new Exception("Driver resource missing");
 string result="PASS: relocated EXE resources, native core launch, driver extraction;  empty defaults, CIDR validation, Unicode DPAPI, WPF render\r\n";File.WriteAllText(output,result);Console.WriteLine(result);
 }catch(Exception e){File.WriteAllText(output,"FAIL: "+e);Console.WriteLine(e);Environment.ExitCode=1;}}
 static void Network(string adapter,string server){string key=Environment.GetEnvironmentVariable("N2N_TEST_KEY");if(String.IsNullOrWhiteSpace(key))throw new ArgumentException("Set N2N_TEST_KEY for the test network");var p=new Profile{Server=server,Community="win-smoke",Ip="10.77.0.30",Mask="255.255.255.0",Adapter=adapter,Relay=true};using(var engine=new Engine()){engine.Log=s=>File.AppendAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"network.log"),s+"\r\n");engine.Start(p,key);bool connected=false;for(int i=0;i<30;i++){Thread.Sleep(1000);try{if(engine.Connected().GetAwaiter().GetResult()){connected=true;break;}}catch(Exception ex){File.AppendAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"network.log"),ex.Message+"\r\n");}}if(!connected)throw new Exception("Registration timeout");using(var socket=new TcpClient()){if(!socket.ConnectAsync("10.77.0.10",18800).Wait(15000))throw new Exception("TCP connect timeout");socket.ReceiveTimeout=5000;byte[] payload=Encoding.UTF8.GetBytes("n2n-windows-smoke\n");socket.GetStream().Write(payload,0,payload.Length);string reply=new StreamReader(socket.GetStream()).ReadLine();if(reply!="n2n-windows-smoke")throw new Exception("TCP payload mismatch");}engine.Stop().GetAwaiter().GetResult();if(engine.Running)throw new Exception("Disconnect failed");}}
}
