# n2n edge · Android

独立 Android Studio / Gradle 工程。最低 Android 8，使用 Android VPNService 免 root 接入 n2n v3 IPv4 子网。Material 3 明暗主题、统一应用图标，首次安装所有连接身份和地址字段为空。

`build.ps1` 自动构建 ARM64、ARMv7、x86_64、x86，以及包含四种架构的通用 APK。编译输出在 `app/build/outputs/apk/debug`，仓库内当前交付文件在 `dist`。需要 SDK 36、NDK 28.2.13676358、CMake 3.22.1，以及兼容 Gradle 的 JDK（默认使用 Android Studio 自带 JBR，也可设置 `JAVA_HOME`）。本机 SDK 路径写入不提交 Git 的 `local.properties`，例如 `sdk.dir=C:/Users/your-user/AppData/Local/Android/Sdk`。

`vendor` 包含所有原生依赖及许可证，不依赖其他平台目录或旧工程。密钥通过 Android Keystore 加密保存；应用只路由所填虚拟 IPv4 子网。升级安装会保留用户已经保存的配置。

网络配置中的「连接模式」沿用 Material 3 下拉框，可选「P2P 优先」（默认，直连失败时中转）或「强制服务器中转」（禁用直连，使用 UDP 中转）。先断开再切换并重新连接，模式会保存。旧配置默认 P2P 优先；状态栏显示配置模式，并非实时路径检测。Java 配置经 JNI 传入 n2n v3 的 `allow_p2p`，核心在强制中转时拒绝直连收包并始终经 supernode 发包。

应用代码按 GPL-3.0-or-later 提供；第三方源码保留各自许可证。APK 为开发签名。

`SmokeRunner` 是需显式运行的仪器测试，不包含在应用 APK 中。网络测试必须通过 instrumentation 参数 `-e key` 传入测试密钥，`-e community` 指定测试社区，`-e server` 指定测试节点；默认社区为 `n2n-smoke`，默认节点为模拟器宿主机 `10.0.2.2:17777`。测试会修改应用保存的配置，请在专用测试设备上运行。
