# 2026-09-10 连接模式切换验证

- Windows x64/x86、Android 四种 ABI 和 universal APK 构建通过；更新两端 `dist` 和 SHA-256 清单。
- Windows WPF 离屏渲染通过，新模式选择框与算法选择框使用相同样式；配置序列化验证两种模式保存及旧配置默认值。
- 本机原生 UDP 检查通过：P2P 使用已知对端，强制中转覆盖已知对端地址；强制中转单播和广播发往模拟服务器，即使等待服务器也不广播至对端；直连 REGISTER_ACK 仅在 P2P 模式下建立对端记录。测试使用本机临时 UDP 套接字，不连接远程服务器或 TAP。
- 两端修改后的 `edge_utils.c` SHA-256 一致。Android 的 Java/JNI/native 配置链已编译通过，尚未完成 Android 设备上的界面和 VPN 运行验证；本轮未做跨设备 NAT 穿透及真实 supernode 中转测试。

# 0.3.0 验证记录

- Windows x64 和 x86 编译完成，PE Machine 分别为 0x8664 和 0x14c；内置原生核心架构分别匹配。
- 两个 EXE 分别复制到独立验证目录，验证内置资源释放、核心 --help 启动、TAP 安装程序释放、空默认配置、DPAPI Unicode 密钥往返和 WPF 圆角控件渲染。输出位于 verification/relocated-x64 与 verification/relocated-x86。
- Android 四种 ABI 及 universal APK 均编译通过、签名校验通过；包内 libedge_jni.so 的 ELF 架构逐个核对。具体结果见 verification/architectures.json。
- 本轮没有重做跨设备网络测试。先前 Windows x64 0.2.0 与 Linux n2n 3.0 的注册、AES-CBC TCP 收发和断开已通过，测试借助临时 SSH 通道；公网直连在当时开发网络上存在超时，不能用该测试保证任意网络下的公网连通性。
- Windows x86 和 Android x86 为新增构建，本轮未在对应实体设备上完成 VPN 数据互通验证。
- Windows EXE 未做发行者签名；内置 OpenVPN TAP 安装程序签名有效。
