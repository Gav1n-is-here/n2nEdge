# n2nEdge

应用名：**n2n edge**。Android 与 Windows 使用统一图标和 Material 风格，首次启动不预填节点、社区、密钥或虚拟 IP。

项目按 Android 和 Windows 分为两个平台目录，各自包含源码、依赖源码、构建脚本和当前可下载产物。

```text
n2nEdge/
  android/                 Android 工程
    app/                   应用及 JNI
    vendor/                n2n、TUN/TAP 桥接源码
    dist/                  四种 ABI 的 APK 和通用 APK
  win/                     Windows 工程
    vendor/                n2n 源码
    toolchains/            本机 MinGW-w64 工具链（不提交 Git）
    compat/                Windows 编译兼容修正
    assets/                共用图标、内置 TAP 安装程序
    server/                Linux supernode 部署脚本和配置模板
    dist/x64/n2n-edge.exe   64 位单文件版
    dist/x86/n2n-edge.exe   32 位单文件版
```

## 下载与运行

- [Windows x64](win/dist/x64/n2n-edge.exe)
- [Windows x86](win/dist/x86/n2n-edge.exe)
- [Android 通用 APK](android/dist/n2n-edge-0.3.1-universal.apk)
- [Android ARM64](android/dist/n2n-edge-0.3.1-arm64-v8a.apk)
- [Android ARMv7](android/dist/n2n-edge-0.3.1-armeabi-v7a.apk)
- [Android x86_64](android/dist/n2n-edge-0.3.1-x86_64.apk)
- [Android x86](android/dist/n2n-edge-0.3.1-x86.apk)

Windows 只需复制对应目录下的 **一个 EXE**，不需要附带核心 EXE、DLL 或安装包。它会从自身资源释放所需文件到当前用户的 LocalAppData。系统需具备 .NET Framework 4.8；Windows 10/11 的常见更新版本已具备。首次在没有 TAP 的电脑上连接，需要从应用内安装系统 TAP 驱动并授予管理员权限；这一步无法靠移动 EXE 绕过。

保存的配置位于当前电脑的 `%LOCALAPPDATA%\N2nEdge`，不会随 EXE 自动搬走。移动 EXE 到同一电脑的另一目录仍能读取配置；复制到另一电脑首次为未配置状态。密钥通过 DPAPI 绑定 Windows 用户，不应直接拷贝加密配置跨电脑使用。

## 编译

- Android：`android\build.ps1`，在 `android/app/build/outputs/apk/debug` 生成四种 ABI 和通用 APK。SDK 路径填在 `android/local.properties`，详细环境见 [Android 说明](android/README.md)。
- Windows：`win\build.ps1 -Architecture x64` 或 `-Architecture x86`。`-Toolchain` 指定相应 MinGW-w64 工具链的 bin 目录；两种架构使用独立构建目录。

本版已检查全部 APK 的签名及 ELF 架构、两个 Windows EXE 的 PE 架构；x64/x86 都完成异目录复制后的界面渲染、内置核心启动和驱动资源释放验证。实际网络测试范围见 [验证说明](win/VERIFICATION.zh-CN.md)。

Windows 默认工具链路径为 `win/toolchains/winlibs/mingw64/bin`（x64）和 `win/toolchains/winlibs-i686/mingw32/bin`（x86）。工具链需自行下载，也可以通过 `-Toolchain` 指向已有目录。构建缓存、本机 SDK 配置、运行日志和诊断抓包不提交 Git。

## 许可证

本项目应用代码按 [GPL-3.0-or-later](LICENSE) 提供；第三方源码保留各自许可证，详见各平台的 `vendor` 目录及 [Windows 依赖说明](win/README.zh-CN.md)。
