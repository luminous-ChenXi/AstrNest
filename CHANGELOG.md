# AstrNest 更新日志

## [Unreleased] - 2026-02-27

### 更新主题：前端暗色主题适配与移动端优化

---

### 一、主题切换器优化

**修改文件：**
- `frontend/src/components/common/ThemeSwitcher.vue`
- `frontend/src/composables/useTheme.js`

**变更内容：**
1. **手机端隐藏主题切换器** - 添加 `hidden md:flex` 类，在移动端（<768px）自动隐藏
2. **默认自动跟随系统** - 添加 `initialValue: 'auto'`，默认根据用户设备系统主题自动切换明暗模式

---

### 二、用户页面暗色主题适配

**修改文件：**
- `frontend/src/views/user/UserImagesView.vue`
- `frontend/src/views/user/UserProfileView.vue`
- `frontend/src/views/user/UserSecurityView.vue`
- `frontend/src/views/user/UserApiManagerView.vue`

**变更内容：**
将所有硬编码颜色替换为 CSS 变量，支持明暗主题切换：

| 原硬编码 | 替换为 |
|---------|--------|
| `text-white/80` | `text-body-secondary` |
| `text-white/70` | `text-body-muted` |
| `text-white/60` | `text-body-soft` |
| `text-white/50`, `text-white/40` | `text-body-faint` |
| `text-white` | `text-body-primary` |
| `bg-white/5` | `bg-surface-overlay` |
| `bg-black/30`, `bg-black/20` | `bg-surface-strong` |
| `border-white/10`, `border-white/15` | `border-body` |

**涉及页面：**
- 仪表盘（UserHomeView.vue）
- 媒体管理（UserImagesView.vue）
- 资料信息（UserProfileView.vue）
- 安全设置（UserSecurityView.vue）
- API 接口管理（UserApiManagerView.vue）

---

### 三、移动端布局优化

**修改文件：**
- `frontend/src/views/user/UserHomeView.vue`
- `frontend/src/layouts/UserLayout.vue`

**变更内容：**
1. **统计卡片** - 手机端改为 3 列紧凑布局，字体和间距适配
2. **配额卡片** - 圆角和间距响应式调整
3. **上传区域** - 添加上传图标，按钮和间距更紧凑
4. **最近上传** - 图片高度手机端 `h-36`，桌面端 `h-48`
5. **整体间距** - 所有区域 padding、margin 响应式调整

---

### 四、下拉菜单主题适配

**修改文件：**
- `frontend/src/assets/styles/chenxi-interactions.css`
- `frontend/src/layouts/UserLayout.vue`

**变更内容：**
1. **背景色** - 从 `rgba(17, 25, 40, 0.95)` 改为 `var(--panel-overlay)`
2. **边框** - 从 `rgba(255, 255, 255, 0.1)` 改为 `var(--border-soft)`
3. **文字颜色** - 使用 `var(--text-body-secondary)` 等变量
4. **悬停效果** - 使用 `var(--color-bg-strong)` 和 `var(--color-text-primary)`

---

### 五、修复汇总

| 问题 | 修复方式 |
|-----|---------|
| 暗色主题字体看不见 | 所有硬编码颜色替换为 CSS 变量 |
| 手机端主题切换器突兀 | 添加 `hidden md:flex` 隐藏 |
| 用户页面无主题适配 | 5 个用户页面全部使用变量类 |
| 下拉菜单无主题适配 | 样式文件使用 CSS 变量 |

---

### 测试建议

1. 在系统设置中切换明暗主题，验证所有页面正常显示
2. 使用手机浏览器访问，验证主题切换器已隐藏
3. 验证用户头像下拉菜单在明暗主题下的显示效果
4. 检查所有按钮、输入框、卡片的边框和背景色

---

## [2026-02-24] - 移动端适配与主题系统优化

### 功能更新概览

本次更新主要围绕**移动端适配性优化**和**主题系统一致性**两大主题，解决了手机端组件显示异常、暗色主题字体丢失等问题，同时全面规范化CSS变量使用，提升代码可维护性。

### 详细更新内容

#### 1. 移动端适配性改进

##### 1.1 公开图片组件显示修复
- **问题描述**：手机端主页公开图片组件丢失，网格布局在移动端显示异常
- **优化后**：使用响应式网格 `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`，移动端单列显示
- **实现位置**：`frontend/src/components/public/PublicGalleryGrid.vue`

##### 1.2 导航栏移动端适配
- **问题描述**：移动端导航菜单背景色硬编码，暗色主题下显示异常
- **优化后**：统一使用 `var(--glass-bg)` 和 `var(--border-soft)`，自动适配主题
- **实现位置**：`frontend/src/views/PublicLandingView.vue`

##### 1.3 用户布局标签页优化
- **问题描述**：暗色主题下用户中心标签页文字对比度不足，难以辨识
- **优化后**：使用 `var(--color-text-primary)` 和 `var(--color-text-secondary)` 确保可读性
- **实现位置**：`frontend/src/layouts/UserLayout.vue`

#### 2. 主题系统规范化

##### 2.1 硬编码颜色值清理
- **问题描述**：大量使用 `#F9A8C8`、`#E87A9F`、`rgba(255,255,255,x)` 等硬编码颜色
- **优化后**：统一替换为CSS变量：
  - 主色调 → `var(--color-brand-primary)`
  - 强调色 → `var(--color-brand-accent)`
  - 背景色 → `var(--glass-bg)` / `var(--chip-bg)`
  - 边框色 → `var(--glass-border)` / `var(--chip-border)`
- **影响范围**：10+ 组件文件，50+ 处样式定义

##### 2.2 认证卡片主题适配
- **问题描述**：认证卡片使用硬编码白色文字，暗色主题下不可见
- **优化后**：使用 `text-body-primary`、`text-body-muted` 等语义化类名
- **实现位置**：`frontend/src/components/chenxi/ChenxiAuthCard.vue`

##### 2.3 主题切换器优化
- **问题描述**：主题切换按钮使用硬编码背景色，需要单独定义暗色主题样式
- **优化后**：使用 `var(--color-bg-strong)`、`var(--border-soft)`，简化代码结构
- **实现位置**：`frontend/src/components/common/ThemeSwitcher.vue`

#### 3. 响应式布局增强

##### 3.1 痛点卡片网格适配
- **优化前**：固定3列网格，移动端显示拥挤
- **优化后**：移动端单列 `@media (max-width: 768px) { grid-template-columns: 1fr; }`
- **实现位置**：`frontend/src/views/PublicLandingView.vue`

##### 3.2 统计区域适配
- **优化内容**：统计数字颜色统一使用 `var(--color-brand-primary)`
- **优化内容**：统计卡片背景使用 `var(--chip-bg)`，确保主题一致性

### 前端变更文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `frontend/src/views/PublicLandingView.vue` | 修改 | 修复导航栏、痛点卡片、信任区域、联系区域等样式，移除硬编码颜色 |
| `frontend/src/components/public/PublicGalleryGrid.vue` | 修改 | 优化响应式网格布局，修复分页面板样式 |
| `frontend/src/layouts/UserLayout.vue` | 修改 | 修复标签页暗色主题样式 |
| `frontend/src/views/user/UserHomeView.vue` | 修改 | 修复配额卡片暗色主题样式 |
| `frontend/src/components/chenxi/ChenxiAuthCard.vue` | 修改 | 替换硬编码白色文字为CSS变量 |
| `frontend/src/components/common/ThemeSwitcher.vue` | 修改 | 使用CSS变量简化主题适配逻辑 |

### 代码规范改进

#### CSS变量使用规范
```css
/* 优化前 - 硬编码颜色 */
.pain-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(249, 168, 200, 0.2);
}

/* 优化后 - CSS变量 */
.pain-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}
```

#### 主题适配简化
```css
/* 优化前 - 需要单独定义暗色主题 */
.landing-header {
  background: rgba(255, 255, 255, 0.7);
}
.dark .landing-header {
  background: rgba(5, 6, 12, 0.7);
}

/* 优化后 - 变量自动适配 */
.landing-header {
  background: var(--glass-bg);
}
```

### 用户体验改进

#### 移动端浏览体验
- 公开图片网格自适应屏幕宽度
- 导航菜单在暗色主题下清晰可见
- 所有卡片组件正确显示背景和边框

#### 暗色主题一致性
- 所有文字使用语义化颜色变量，确保对比度
- 背景色使用玻璃态效果，提升视觉层次
- 品牌色统一使用主色调变量，保持视觉一致性
