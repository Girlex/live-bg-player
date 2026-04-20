# 版本号管理规范

## 📋 当前版本

- **versionCode**: 1
- **versionName**: "1.0"
- **Git Tag**: v1.0-stable

---

## 🔄 版本号规则

### Semantic Versioning (语义化版本)

格式：`MAJOR.MINOR.PATCH`

| 部分 | 说明 | 何时递增 |
|------|------|---------|
| MAJOR | 主版本号 | 不兼容的 API 修改、重大功能更新 |
| MINOR | 次版本号 | 向下兼容的功能性新增 |
| PATCH | 修订号 | 向下兼容的问题修正 |

### versionCode 规则

- 每次发布必须递增
- 建议格式：`MAJOR * 10000 + MINOR * 100 + PATCH`
- 例如：
  - v1.0.0 → versionCode = 10000
  - v1.1.0 → versionCode = 10100
  - v1.1.1 → versionCode = 10101
  - v2.0.0 → versionCode = 20000

---

## 🚀 版本更新流程

### 1. 开发新功能

```bash
# 切换到功能分支
git checkout feature-overlay-and-gesture

# 开发完成后合并到 master
git checkout master
git merge feature-overlay-and-gesture
```

### 2. 更新版本号

编辑 `app/build.gradle.kts`：

```kotlin
defaultConfig {
    versionCode = 2  // 递增
    versionName = "1.1"  // 根据改动调整
}
```

### 3. 创建 Git 标签

```bash
git tag -a v1.1 -m "Release v1.1 - 新增画面控件叠加和手势切换功能"
```

### 4. 推送到 GitHub

```bash
git push origin master --tags
```

### 5. 创建 GitHub Release

访问：https://github.com/Girlex/live-bg-player/releases/new
- Tag: v1.1
- Title: Release v1.1
- Description: 更新内容列表
- 上传 APK 文件

---

## 📝 版本历史记录

### v1.0 (当前稳定版)
- ✅ 零卡顿视频循环播放
- ✅ 全屏沉浸式体验
- ✅ 自动识别横竖屏
- ✅ MD3 风格配置页面
- ✅ zoom 模式无黑边播放

### 计划中的版本

#### v1.1 (下一版本)
- [ ] 画面控件叠加（时钟、计时器、弹幕、Logo）
- [ ] V 字手势切换视频
- [ ] 多视频播放列表管理

#### v2.0 (未来规划)
- [ ] 云端视频库支持
- [ ] 自定义手势扩展
- [ ] 更多控件类型

---

## 💡 最佳实践

1. **每次发布前**：
   - 更新 versionCode 和 versionName
   - 运行完整测试
   - 生成 Release APK

2. **Git 标签命名**：
   - 使用 `v` 前缀：`v1.0`, `v1.1`, `v2.0`
   - 添加详细描述

3. **Release Notes**：
   - 列出所有新功能
   - 标注修复的 Bug
   - 注明已知问题

4. **APK 命名规范**：
   ```
   live-bg-player-v{versionName}-{buildType}.apk
   例如：
   live-bg-player-v1.0-release.apk
   live-bg-player-v1.1-debug.apk
   ```

---

## 🔧 自动化脚本（可选）

可以创建脚本来自动递增版本号，但手动控制更安全可靠。
