# n2n edge · Windows x64 / x86

Windows 客户端使用 WPF 实现 Material 风格界面，内置从源码编译的 n2n 3.0 edge。与 Android 版使用同一上游版本和图标。首次启动的节点、社区、密钥、IP、子网掩码均为空；保存后会记住用户自己的配置。

## 使用

1. 运行 `n2n-edge.exe`，接受 Windows 管理员权限提示，虚拟网卡配置需要此权限。
2. 没有 TAP 网卡时，点击“安装 TAP 驱动”，完成内置官方 OpenVPN TAP-Windows 驱动安装后刷新网卡。选择一张专用于 n2n 的 TAP 网卡。
3. 填写节点 `主机:UDP端口`、Community、共享密钥、虚拟 IP 和子网掩码。所有设备的 Community、密钥、加密算法和头部加密设置必须一致；IP 必须不同。
4. 点击“连接子网”。“已连接节点”表示 n2n 核心收到真实注册确认，不代表其他设备的密钥也填写正确。

“检测节点”发送真实 REGISTER_SUPER 报文，校验响应类型及 cookie；它使用临时探测社区，只判断节点是否回应，受限社区节点可能拒绝探测。连接失败可复制日志排查。

支持 AES-CBC、Twofish、ChaCha20、Speck-CTR，支持 MTU、MAC、头部加密、强制 UDP 中转和子网组播。没有默认路由或系统代理设置。Windows 3.0 上游不支持 TCP 中转。

密钥用 Windows DPAPI 加密保存到 `%LOCALAPPDATA%\N2nEdge\profile.xml`，通过子进程环境变量传递，不放在命令行。退出时会停止 edge；Windows Job Object 防止 UI 异常退出后留下连接进程。TAP 驱动及已保存的网卡 IP 配置不随应用关闭而卸载。

适用于 Windows 10/11 x64，使用系统 .NET Framework 4.8。应用 EXE 尚未签名；内置的 TAP 9.24.7 安装程序已核验 OpenVPN Inc. 的有效 Authenticode 签名。

## 构建与源代码

在仓库根目录运行 `win\build.ps1 -Architecture x64` 或 `win\build.ps1 -Architecture x86`。脚本的 `-Toolchain` 参数可指定 MinGW-w64 的 bin 目录，需要 GCC、CMake 和 mingw32-make。UI 用系统 .NET Framework 的 C# 编译器构建，无须安装 .NET SDK。

图标为本项目原创矢量设计，源文件 `assets/icon.svg`；`make-icon.py` 使用 Pillow 生成 PNG 和多尺寸 ICO。

n2n 源码位于 `win/vendor/n2n`，固定为 3.0 标签提交 `66f557af97b9c2ad42537516101fd04df2639ef0`。Windows 独立兼容层修正线程句柄赋值优先级、MinGW getopt 头文件冲突，支持 UTF-16 环境中的共享密钥转换为 UTF-8，关闭物理网卡组播发现但保留通过节点发现其他 edge 的能力。

本客户端代码按 GPL-3.0-or-later 提供。n2n 的许可证随源码提供。MinGW getopt 兼容头保留其公有领域声明。

- n2n: https://github.com/ntop/n2n/tree/3.0
- TAP 驱动源码: https://github.com/OpenVPN/tap-windows6
- 内置驱动原始下载: https://build.openvpn.net/downloads/releases/tap-windows-9.24.7-I601-Win10.exe

测试辅助程序不随应用交付，测试配置不会预填进应用。

`Smoke.cs` 的网络检查需要显式传入 `network <adapter> <server:port>`，并设置环境变量 `N2N_TEST_KEY`。对端需使用测试社区 `win-smoke`，在虚拟地址 `10.77.0.10:18800` 提供 TCP 回显服务。

`server` 提供 Debian/Ubuntu 上的 supernode 安装模板。将该目录复制到服务器，检查端口和配置后运行 `bash install-supernode.sh`；脚本需要 sudo、systemd 和 UFW，使用 UDP 7777，并保留临时源码构建目录。模板不包含可直接使用的服务器地址或共享密钥。


## Portable 交付

`dist/x64` 与 `dist/x86` 各只有一个可运行 EXE，核心、图标及官方 TAP 安装程序均在 EXE 内。可单独移动，不依赖原工程目录。配置存储在当前电脑 LocalAppData，不随文件自动移动到另一电脑。首次安装 TAP 是系统驱动操作，需要管理员权限。

0.3.0 的密码框、TAP 选择框及算法选择框使用统一圆角模板，保留密码掩码、选择和键盘操作。32 位兼容层使用正确的 Windows 线程调用约定和 IP Helper 函数声明，并修正管理时间戳的 64 位格式。
