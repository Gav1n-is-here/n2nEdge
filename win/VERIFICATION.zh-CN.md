# 0.3.0 验证记录

- Windows x64 和 x86 编译完成，PE Machine 分别为 0x8664 和 0x14c；内置原生核心架构分别匹配。
- 两个 EXE 分别复制到独立验证目录，验证内置资源释放、核心 --help 启动、TAP 安装程序释放、空默认配置、DPAPI Unicode 密钥往返和 WPF 圆角控件渲染。输出位于 verification/relocated-x64 与 verification/relocated-x86。
- Android 四种 ABI 及 universal APK 均编译通过、签名校验通过；包内 libedge_jni.so 的 ELF 架构逐个核对。具体结果见 verification/architectures.json。
- 本轮没有重做跨设备网络测试。先前 Windows x64 0.2.0 与 Linux n2n 3.0 的注册、AES-CBC TCP 收发和断开已通过，测试借助临时 SSH 通道；公网直连在当时开发网络上存在超时，不能用该测试保证任意网络下的公网连通性。
- Windows x86 和 Android x86 为新增构建，本轮未在对应实体设备上完成 VPN 数据互通验证。
- Windows EXE 未做发行者签名；内置 OpenVPN TAP 安装程序签名有效。
