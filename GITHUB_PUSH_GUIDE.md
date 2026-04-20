# GitHub 推送完整指南

## 📋 前置准备

### ✅ 已完成
- [x] Git 仓库已初始化
- [x] 代码已提交（commit）
- [x] 稳定版本标签已创建（v1.0-stable）
- [x] 功能分支已创建（feature-overlay-and-gesture）
- [x] 远程仓库地址已配置

### ⚠️ 需要您完成
1. **在 GitHub 创建仓库**
2. **获取 Personal Access Token**
3. **执行推送**

---

## 🚀 完整操作步骤

### 步骤 1: 在 GitHub 创建仓库

1. 访问：https://github.com/new
2. 填写信息：
   ```
   Repository name: live-bg-player
   Description: 零卡顿游戏直播背景视频播放器 Android App
   Public/Private: 根据您的选择
   ```
3. **重要**：不要勾选 "Initialize this repository with a README"
4. 点击 "Create repository"

### 步骤 2: 获取 Personal Access Token (PAT)

1. 访问：https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 配置：
   - Note: `live-bg-player`
   - Expiration: 90 days（或根据需要）
   - Scopes: 勾选 **repo**（全选）
4. 点击 "Generate token"
5. **复制 Token**（格式类似：`ghp_xxxxxxxxxxxxxxxxxxxx`）
   - ⚠️ Token 只显示一次，请妥善保存！

### 步骤 3: 推送代码到 GitHub

#### 方法 A: 使用批处理脚本（推荐）

双击运行项目根目录下的：
```
push_to_github.bat
```

当提示输入用户名和密码时：
- Username: `girlex`
- Password: **粘贴刚才复制的 Token**（不会显示字符，正常现象）

#### 方法 B: 手动命令行推送

```bash
# 推送主分支和标签
git push -u origin master --tags

# 如果需要推送功能分支
git push origin feature-overlay-and-gesture
```

同样需要输入：
- Username: `girlex`
- Password: **您的 Token**

---

## ✅ 验证推送成功

推送成功后，访问：
```
https://github.com/girlex/live-bg-player
```

您应该能看到：
- ✅ 所有源代码文件
- ✅ v1.0-stable 标签
- ✅ 完整的提交历史

---

## 🔄 后续开发工作流

### 日常开发流程

```bash
# 1. 切换到功能分支
git checkout feature-overlay-and-gesture

# 2. 开发新功能...

# 3. 提交更改
git add .
git commit -m "添加画面控件叠加功能"

# 4. 推送到 GitHub
git push origin feature-overlay-and-gesture

# 5. 完成后合并到主分支
git checkout master
git merge feature-overlay-and-gesture
git tag -a v1.1-new-feature -m "新增功能版本"
git push origin master --tags
```

### 回退到稳定版本

如果新功能有问题，可以快速回退：

```bash
# 回退到 v1.0-stable
git checkout v1.0-stable

# 或者创建新的修复分支
git checkout -b hotfix/issue-name v1.0-stable
```

---

## 🛠️ 常见问题

### Q1: 推送时提示 "Repository not found"
**解决**：GitHub 仓库还未创建，请先在 GitHub 上创建空仓库

### Q2: 推送时提示 "Authentication failed"
**解决**：
- 确认使用的是 Personal Access Token，不是账户密码
- 检查 Token 是否过期
- 确认 Token 有 repo 权限

### Q3: 推送时网络连接超时
**解决**：
- 检查网络连接
- 尝试使用代理
- 或使用 GitHub Desktop 客户端

### Q4: 如何查看已推送的内容？
```bash
# 查看所有分支
git branch -a

# 查看所有标签
git tag

# 查看提交历史
git log --oneline
```

---

## 📞 需要帮助？

如果遇到问题，可以：
1. 检查 Git 状态：`git status`
2. 查看远程配置：`git remote -v`
3. 测试连接：`git ls-remote origin`

---

**祝推送顺利！** 🎉
