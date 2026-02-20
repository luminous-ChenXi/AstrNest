const fs = require('fs');
const path = require('path');

// 创建渐变图标 - 粉色到薄荷绿到浅蓝
function createGradientIcon(size) {
  const padding = Math.floor(size * 0.15);
  const innerSize = size - padding * 2;

  // 使用粉色/薄荷绿/浅蓝渐变主题
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <!-- 粉色到薄荷绿渐变 -->
    <linearGradient id="gradient1" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#ff6b9d;stop-opacity:1" />
      <stop offset="50%" style="stop-color:#ff8fab;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#4ecdc4;stop-opacity:1" />
    </linearGradient>
    <!-- 薄荷绿到浅蓝渐变 -->
    <linearGradient id="gradient2" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#4ecdc4;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#87ceeb;stop-opacity:1" />
    </linearGradient>
    <!-- 阴影滤镜 -->
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="rgba(0,0,0,0.15)"/>
    </filter>
  </defs>

  <!-- 背景圆角矩形 -->
  <rect x="${padding}" y="${padding}" width="${innerSize}" height="${innerSize}"
        rx="${Math.floor(innerSize * 0.2)}" ry="${Math.floor(innerSize * 0.2)}"
        fill="url(#gradient1)" filter="url(#shadow)"/>

  <!-- 图片图标 - 使用白色 -->
  <g fill="none" stroke="white" stroke-width="${Math.max(1.5, size * 0.04)}" stroke-linecap="round" stroke-linejoin="round">
    <!-- 图片外框 -->
    <rect x="${padding + innerSize * 0.2}" y="${padding + innerSize * 0.25}"
          width="${innerSize * 0.6}" height="${innerSize * 0.5}"
          rx="${innerSize * 0.05}" ry="${innerSize * 0.05}"/>
    <!-- 山脉 -->
    <polyline points="${padding + innerSize * 0.2},${padding + innerSize * 0.65} ${padding + innerSize * 0.35},${padding + innerSize * 0.5} ${padding + innerSize * 0.5},${padding + innerSize * 0.6} ${padding + innerSize * 0.8},${padding + innerSize * 0.4}"/>
    <!-- 太阳/月亮 -->
    <circle cx="${padding + innerSize * 0.65}" cy="${padding + innerSize * 0.4}" r="${innerSize * 0.08}"/>
  </g>

  <!-- 右上角装饰小圆点 -->
  <circle cx="${size - padding - innerSize * 0.15}" cy="${padding + innerSize * 0.15}"
          r="${innerSize * 0.08}" fill="white" opacity="0.6"/>
</svg>`;

  return svg;
}

// 创建单色图标（用于工具栏）
function createMonochromeIcon(size) {
  const padding = Math.floor(size * 0.1);
  const innerSize = size - padding * 2;

  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="toolbarGradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#ff6b9d;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#4ecdc4;stop-opacity:1" />
    </linearGradient>
  </defs>

  <!-- 图片图标 -->
  <g fill="none" stroke="url(#toolbarGradient)" stroke-width="${Math.max(1.5, size * 0.06)}" stroke-linecap="round" stroke-linejoin="round">
    <rect x="${padding + innerSize * 0.1}" y="${padding + innerSize * 0.15}"
          width="${innerSize * 0.8}" height="${innerSize * 0.7}"
          rx="${innerSize * 0.08}" ry="${innerSize * 0.08}"/>
    <polyline points="${padding + innerSize * 0.1},${padding + innerSize * 0.7} ${padding + innerSize * 0.3},${padding + innerSize * 0.5} ${padding + innerSize * 0.5},${padding + innerSize * 0.6} ${padding + innerSize * 0.9},${padding + innerSize * 0.35}"/>
    <circle cx="${padding + innerSize * 0.7}" cy="${padding + innerSize * 0.35}" r="${innerSize * 0.1}"/>
  </g>
</svg>`;

  return svg;
}

// 确保 icons 目录存在
const iconsDir = path.join(__dirname, 'icons');
if (!fs.existsSync(iconsDir)) {
  fs.mkdirSync(iconsDir, { recursive: true });
}

// 生成各种尺寸的图标
const sizes = [16, 32, 48, 128];
const sizes2x = [32, 64, 96, 256];

console.log('正在生成渐变主题图标...');

sizes.forEach((size, index) => {
  // 主图标
  const iconSvg = createGradientIcon(size);
  fs.writeFileSync(path.join(iconsDir, `icon-${size}.svg`), iconSvg);
  console.log(`✓ 生成 icon-${size}.svg`);

  // 2x 版本
  const size2x = sizes2x[index];
  const icon2xSvg = createGradientIcon(size2x);
  fs.writeFileSync(path.join(iconsDir, `icon-${size}@2x.svg`), icon2xSvg);
  console.log(`✓ 生成 icon-${size}@2x.svg`);
});

// 生成工具栏专用图标
const toolbarSizes = [16, 32];
toolbarSizes.forEach(size => {
  const toolbarSvg = createMonochromeIcon(size);
  fs.writeFileSync(path.join(iconsDir, `toolbar-${size}.svg`), toolbarSvg);
  console.log(`✓ 生成 toolbar-${size}.svg`);
});

console.log('\n图标生成完成！');
console.log('注意：Chrome 扩展需要 PNG 格式的图标。');
console.log('请使用在线工具将 SVG 转换为 PNG，或使用浏览器截图功能。');
console.log('推荐尺寸: 16x16, 32x32, 48x48, 128x128');
