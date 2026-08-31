# ScopeShift

ScopeShift 是一个基于 libxposed Modern API 102 的应用级地区环境模块。它只在 LSPosed 作用域内工作，为选定应用提供一致的 SIM、运营商、语言、时区和位置参数。

## 功能

- 通过“地区 + 运营商”生成完整环境配置
- 覆盖 SIM/网络国家码、运营商名称和 MCC-MNC
- 覆盖应用进程读取到的语言、IANA 时区、实时 UTC 偏移和 GPS 坐标
- 内置 32 个常用地区及对应运营商
- 使用 Remote Preferences 同步配置，保存后自动热重载已注入进程
- 可清除当前配置，使作用域应用恢复读取真实系统环境
- 作用域完全由 LSPosed 管理器控制

## 兼容性

- Android 11 至 Android 16
- 支持 libxposed Modern API 102 的 LSPosed 实现
- 已在 Android 16、LSPosed API 102 环境完成真机验证

## 安装与使用

1. 从项目 Releases 下载已签名 APK 并安装。
2. 在 LSPosed 中启用 ScopeShift，并勾选需要生效的应用。
3. 打开 ScopeShift，选择地区和运营商后保存。
4. 首次启用模块或切换作用域后，强制停止并重新打开目标应用。

配置页会显示当前作用域应用数量。需要停止覆盖时，点击“恢复系统默认”清除配置。目标应用可能缓存已经读取的地区数据；遇到未立即变化的情况，请先彻底结束其进程后再测试。

## 作用范围

ScopeShift 修改的是目标应用通过 Android Java 框架读取到的环境信息。IP 地址、账号注册地区、服务端记录、设备硬件标识，以及应用通过原生代码或自有网络接口收集的信息不在覆盖范围内。不同应用的地区判定策略不同，因此不承诺对所有应用产生相同结果。

## 隐私

ScopeShift 不申请 Android 权限，不包含网络访问，也不收集或上传数据。用户选择的地区与运营商保存在 LSPosed 的 Remote Preferences 中。完整说明见 [PRIVACY.md](PRIVACY.md)。

## 构建

要求：JDK 21、Android SDK 37。项目使用 Gradle Wrapper 和固定版本的 libxposed 依赖。

可安装的调试包：

```powershell
.\gradlew.bat :app:assembleDebug
```

产物位于 `app/build/outputs/apk/debug/ScopeShift-<版本号>-debug.apk`。

优化后的 Release 包：

```powershell
.\gradlew.bat :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/ScopeShift-<版本号>-release.apk`。

## 开源许可

ScopeShift 以 [MIT License](LICENSE) 开源。第三方依赖及其许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

使用者应自行确认所在地区、目标服务条款及适用法律，并对模块的使用方式负责。
