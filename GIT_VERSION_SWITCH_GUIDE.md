# Git 版本回退与切换完全指南

## 🎯 核心概念

### 三种版本标识方式

1. **Tag（标签）** - 推荐用于正式版本
   - 例如：`v1.0-stable`, `v1.1`, `v2.0`
   - 语义清晰，易于记忆

2. **Commit Hash（提交哈希）** - 精确到每次提交
   - 例如：`46e8042`, `2e879f7`
   - 唯一标识，最精确

3. **Branch（分支）** - 用于并行开发
   - 例如：`master`, `feature-overlay-and-gesture`
   - 可以持续更新

---

## 🚀 快速开始

### 使用图形化工具（推荐新手）

双击运行项目中的：
```
switch_version.bat
```

提供交互式菜单，无需记忆命令！

---

## 📖 命令行操作详解

### 1️⃣ 查看所有可用版本

```bash
# 查看所有标签
git tag

# 查看标签详情
git tag -n

# 查看所有提交历史（最近20条）
git log --oneline -20

# 查看所有分支
git branch -a
```

### 2️⃣ 切换到特定版本

#### 通过标签切换
```bash
# 切换到 v1.0-stable 版本
git checkout v1.0-stable

# 此时会提示 "detached HEAD"，这是正常的
```

#### 通过 Commit Hash 切换
```bash
# 先查看 commit hash
git log --oneline

# 切换到指定 commit
git checkout 46e8042
```

#### 通过分支切换
```bash
# 切换到功能分支
git checkout feature-overlay-and-gesture

# 切换回主分支
git checkout master
```

### 3️⃣ 基于旧版本创建新分支

如果您想基于某个旧版本进行修改：

```bash
# 基于 v1.0-stable 创建修复分支
git checkout -b hotfix/issue-name v1.0-stable

# 基于某个 commit 创建实验分支
git checkout -b experiment/feature 46e8042
```

### 4️⃣ 返回最新版本

```bash
# 切换回 master 分支
git checkout master

# 拉取最新代码
git pull origin master
```

---

## 💡 实际应用场景

### 场景 1: 新版本有 Bug，需要回退

```bash
# 1. 查看当前版本
git log --oneline -5

# 2. 切换到上一个稳定版本
git checkout v1.0-stable

# 3. 或者基于稳定版本创建修复分支
git checkout -b hotfix/critical-bug v1.0-stable

# 4. 修复后合并回 master
git checkout master
git merge hotfix/critical-bug
```

### 场景 2: 对比两个版本的差异

```bash
# 比较 v1.0 和当前版本的差异
git diff v1.0-stable HEAD

# 比较两个 commit 的差异
git diff 46e8042 2e879f7

# 查看某个文件的版本差异
git diff v1.0-stable HEAD -- app/build.gradle.kts
```

### 场景 3: 导出某个版本的完整代码

```bash
# 方法 1: 切换到该版本后复制整个文件夹
git checkout v1.0-stable
# 然后复制 C:\Users\feng\Desktop\播放器 文件夹

# 方法 2: 导出为 ZIP（不切换当前工作区）
git archive v1.0-stable --format=zip --output=../live-bg-player-v1.0.zip

# 方法 3: 克隆特定标签到新目录
git clone --branch v1.0-stable https://github.com/Girlex/live-bg-player.git live-bg-player-v1.0
```

### 场景 4: 查看某个版本的文件内容（不切换）

```bash
# 查看 v1.0-stable 版本的某个文件
git show v1.0-stable:app/build.gradle.kts

# 查看某个 commit 的文件
git show 46e8042:app/src/main/java/com/lingma/livebgplayer/ui/config/ConfigActivity.kt
```

---

## ⚠️ 注意事项

### Detached HEAD 状态

当您通过 tag 或 commit hash 切换时，会进入 "detached HEAD" 状态：

```
Note: switching to 'v1.0-stable'.

You are in 'detached HEAD' state.
```

**这意味着**：
- ✅ 可以查看和编译代码
- ✅ 可以运行测试
- ❌ **不要直接修改代码**（修改会丢失）

**如果需要修改**：
```bash
# 创建新分支
git checkout -b my-new-branch
```

### 未提交的更改

切换版本前，确保没有未提交的更改：

```bash
# 检查状态
git status

# 如果有未提交更改，选择：
# 1. 提交更改
git add .
git commit -m "保存当前工作"

# 2. 暂存更改
git stash

# 3. 丢弃更改（谨慎！）
git reset --hard
```

---

## 🔄 完整工作流程示例

### 日常开发流程

```bash
# 1. 开始新功能开发
git checkout master
git pull origin master
git checkout -b feature/new-feature

# 2. 开发过程中随时提交
git add .
git commit -m "完成部分功能"

# 3. 发现 Bug，需要回退测试
git stash  # 暂存当前工作
git checkout v1.0-stable  # 切换到稳定版测试
# ... 测试完成后
git checkout feature/new-feature  # 回到功能分支
git stash pop  # 恢复之前的工作

# 4. 功能完成，合并到 master
git checkout master
git merge feature/new-feature
git tag -a v1.1 -m "Release v1.1"
git push origin master --tags
```

### 紧急修复流程

```bash
# 1. 基于稳定版本创建修复分支
git checkout -b hotfix/critical v1.0-stable

# 2. 修复 Bug
# ... 修改代码 ...
git add .
git commit -m "Fix: 修复严重Bug"

# 3. 合并到 master 和 develop
git checkout master
git merge hotfix/critical
git tag -a v1.0.1 -m "Hotfix v1.0.1"
git push origin master --tags

# 4. 删除修复分支
git branch -d hotfix/critical
```

---

## 📊 版本历史可视化

### 查看图形化历史

```bash
# 安装 gitk（Git 自带）
gitk

# 或使用命令行图形界面
git log --graph --oneline --all
```

### 在线查看

访问 GitHub 仓库：
https://github.com/Girlex/live-bg-player

- Commits 标签页：查看所有提交
- Tags 标签页：查看所有版本标签
- Branches 标签页：查看所有分支

---

## 🛠️ 高级技巧

### 1. 恢复误删的文件

```bash
# 从某个版本恢复文件
git checkout v1.0-stable -- path/to/file.txt
```

### 2.  cherry-pick 特定提交

```bash
# 将某个 commit 应用到当前分支
git cherry-pick 46e8042
```

### 3. 重置到某个版本（谨慎使用）

```bash
# 软重置（保留更改）
git reset --soft v1.0-stable

# 硬重置（丢弃所有更改）
git reset --hard v1.0-stable
```

### 4. 查看某个版本的完整快照

```bash
# 导出为 tar.gz
git archive v1.0-stable --format=tar.gz --output=../v1.0.tar.gz
```

---

## 📞 常见问题

### Q: 如何知道当前在哪个版本？
```bash
git describe --tags
# 或
git log -1 --oneline
```

### Q: 切换版本后代码不见了？
检查是否在正确的分支：
```bash
git branch
git status
```

### Q: 如何撤销版本切换？
```bash
git checkout master
```

### Q: 可以同时保留多个版本吗？
可以！使用不同的文件夹：
```bash
# 克隆到不同目录
git clone https://github.com/Girlex/live-bg-player.git live-bg-player-v1.0
cd live-bg-player-v1.0
git checkout v1.0-stable
```

---

## 🎓 总结

| 操作 | 命令 | 用途 |
|------|------|------|
| 查看历史 | `git log --oneline` | 浏览所有版本 |
| 切换版本 | `git checkout <tag/hash>` | 查看旧版本 |
| 创建分支 | `git checkout -b <name>` | 基于某版本开发 |
| 返回最新 | `git checkout master` | 回到主线 |
| 导出代码 | `git archive <tag>` | 打包特定版本 |

**记住**：Git 让您可以随时回到任何历史版本，大胆尝试，不用担心丢失代码！
