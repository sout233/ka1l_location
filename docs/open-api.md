# 无账号版本与开放内容接口

本分支移除了应用账号、邮箱验证码、个人资料、会员套餐、Stripe/微信支付、每日模拟次数检查与扣减。模拟启动不再访问账号服务器，也不需要伪造登录或会员状态。覆盖位置、路线、导航、独立定位、Wi-Fi、基站、相机和 NFC 模拟入口。

ROOT 模式开机后的 100 秒稳定性检查保留在 `SimulationReadiness` 中。设备权限、运行模式、ROOT/Xposed 模块与硬件要求仍然适用。升级启动时会清理旧的 `auth_prefs`，不再保存账号令牌。

## 默认离线使用

直接构建即可，`OPEN_CONTENT_BASE_URL` 默认为空。公告、FAQ 从以下开源文件读取，不发起内容网络请求：

- `app/src/main/assets/open-api/notices.json`
- `app/src/main/assets/open-api/faqs.json`

公告默认是空数组，FAQ 包含中英双语的使用说明。维护这些文件后重新构建即可更新内置内容。

## 可选：自行托管开放接口

无需部署 RuoYi、数据库、登录系统或支付服务。将 `app/src/main/assets/open-api/` 中的文件部署到 Nginx、Caddy、GitHub Pages 等 HTTPS 静态服务即可。也可以用任意开源后端实现相同的 GET 接口。

例如部署在 `https://example.org/kail/`，然后构建：

```sh
./gradlew :app:assembleDebug -POPEN_CONTENT_BASE_URL=https://example.org/kail
```

也可以在本地 `~/.gradle/gradle.properties` 中设置：

```properties
OPEN_CONTENT_BASE_URL=https://example.org/kail
```

| 请求 | 响应 | 必填字段 | 可选字段 |
| --- | --- | --- | --- |
| `GET /kail/notices.json` | 公告对象数组 | `id`（唯一整数）、`title`、`content`（字符串） | `type`（整数，默认 0）、`createTime`（字符串，默认空） |
| `GET /kail/faqs.json` | FAQ 对象数组 | `id`（唯一整数）、`title`、`answerMd`（字符串） | `sort`（整数，默认 0，按升序展示） |

响应应为 HTTP 200、UTF-8 编码的 JSON 数组，建议 `Content-Type: application/json; charset=utf-8`。无需 Authorization、tenant-id、Cookie、API Key 或账户。空数组 `[]` 表示暂无内容，不使用 `{ "code": 0, "data": [...] }` 包装。

公告示例：

```json
[
  {
    "id": 1,
    "title": "维护通知",
    "type": 0,
    "content": "<p>新的公告内容。</p>",
    "createTime": "2026-11-02"
  }
]
```

FAQ 示例：

```json
[
  {
    "id": 1,
    "title": "需要登录吗？",
    "answerMd": "无需登录，模拟功能没有每日次数限制。",
    "sort": 1
  }
]
```

应用先获取并校验远端内容，成功后缓存；网络错误、HTTP 非成功状态、JSON 格式或必填字段错误时，依次使用最近一次有效缓存、APK 内置内容。有效空数组会覆盖旧内容。缓存写入失败不会影响本次展示。公告内容更新后按现有内容哈希规则重新提示，用户已关闭的相同公告不会反复弹出。内容接口故障不影响模拟功能。

## 更新与第三方服务的范围

更新检查已移除原 RuoYi/Yudao 接口，直接使用 GitHub Releases 公共 API，保留原有 GitHub 下载回退逻辑。当前仍指向原项目仓库；发布自己的无账号版本时，应修改 `UpdateChecker.kt` 的 `OWNER`、`REPO`，以及 `AppDrawer.kt` 的 Xposed 模块下载地址，避免安装回原项目发布的版本。

本次替换的是应用账号、付费与业务内容后端。以下依赖仍保留，并非全部改成免密钥服务：

- 百度地图 SDK、地点搜索与路线规划：仍需百度 API Key，见 [baiduApiKey.md](baiduApiKey.md)。不能直接替换 URL 接入 OpenStreetMap，后续迁移需要更换地图 UI、搜索/路线模型及 BD09/GCJ02/WGS84 坐标转换。
- WiGLE、OpenCellID 查询：保留已有的提供商接口和开发者 API Key 配置。
- Firebase 崩溃报告、GitHub 更新下载：仍按项目原有配置使用。

## 验证

在具备项目要求的 Android SDK 36、NDK/CMake 与依赖下载权限的环境中运行：

```sh
./gradlew :app:testDebugUnitTest :app:compileDebugKotlin
```

`OpenContentTest` 覆盖离线模式、远端成功、缓存保存、断网回退、缓存损坏、缓存读写失败、无效响应不污染缓存、有效空数组、必填字段、重复 ID、FAQ 排序、公告默认字段以及全部内容源失效。
