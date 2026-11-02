# Android CI 与 Release

`.github/workflows/android.yml` 在 main 推送、针对 main 的 PR、新建 `v*` 标签及手动触发时运行。Java 21、Android SDK 36、NDK 29.0.13846066、CMake 3.22.1 在 runner 上自动准备。

编译前，CI 将主程序原生模块中的签名校验绑定到本次实际使用的证书，保留校验并避免沿用原项目证书。

主程序和独立 Xposed 工程分别执行 `:app:testDebugUnitTest :app:assembleRelease`。成功后对 release APK 进行 zipalign、签名与 apksigner 验证，并上传 `KailLocation`、`KailLocationXposed` 两组 Actions artifacts，内含 APK 和 SHA-256 校验文件。测试报告单独上传。

发布可选以下任一方式：

1. 在 Actions → Android CI and Release → Run workflow 中选择 main，勾选 `publish_release`，输入全新的标签，例如 `v1.7.2-open.1`。
2. 将新的 `v*` 标签推送到仓库。

只有两个工程都测试、编译和签名成功后，才会创建 GitHub 预发布 Release。手动发布仅允许 main 分支。现有 Release 不会被自动覆盖。普通 main/PR 构建仅上传 artifacts。

当前使用 CI 测试密钥，密码采用 Android 测试约定，**不是保密的生产发布签名**。各应用的密钥分别存入 Actions cache，缓存保留时后续构建复用；缓存失效后可能重新生成密钥，导致不能覆盖安装旧包。正式长期分发应改用持久保管的专用 keystore 与 GitHub Actions secrets。CI APK 不能直接覆盖不同签名的原作者版本，请先备份数据。

测试与签名检查不代表真机验证。地图 Key、ROOT/Xposed 权限等设备条件见 [开放接口说明](open-api.md)。
