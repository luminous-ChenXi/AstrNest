// 图标生成脚本
// 使用 Canvas API 生成不同尺寸的图标

const fs = require('fs');
const { createCanvas } = require('canvas');

const sizes = [16, 32, 48, 128];

function generateIcon(size) {
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext('2d');

  // 绘制圆角矩形背景
  const radius = size * 0.1875; // 24/128 = 0.1875
  ctx.beginPath();
  ctx.moveTo(radius, 0);
  ctx.lineTo(size - radius, 0);
  ctx.quadraticCurveTo(size, 0, size, radius);
  ctx.lineTo(size, size - radius);
  ctx.quadraticCurveTo(size, size, size - radius, size);
  ctx.lineTo(radius, size);
  ctx.quadraticCurveTo(0, size, 0, size - radius);
  ctx.lineTo(0, radius);
  ctx.quadraticCurveTo(0, 0, radius, 0);
  ctx.closePath();

  // 渐变填充
  const gradient = ctx.createLinearGradient(0, 0, size, size);
  gradient.addColorStop(0, '#667eea');
  gradient.addColorStop(1, '#764ba2');
  ctx.fillStyle = gradient;
  ctx.fill();

  // 绘制六边形图标
  const centerX = size / 2;
  const centerY = size / 2;
  const hexRadius = size * 0.3125; // 40/128 = 0.3125

  ctx.beginPath();
  for (let i = 0; i < 6; i++) {
    const angle = (Math.PI / 3) * i - Math.PI / 2;
    const x = centerX + hexRadius * Math.cos(angle);
    const y = centerY + hexRadius * Math.sin(angle);
    if (i === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  }
  ctx.closePath();
  ctx.strokeStyle = 'white';
  ctx.lineWidth = Math.max(1, size * 0.03125); // 4/128 = 0.03125
  ctx.stroke();

  // 绘制中心圆点
  ctx.beginPath();
  ctx.arc(centerX, centerY, size * 0.09375, 0, Math.PI * 2); // 12/128 = 0.09375
  ctx.fillStyle = 'white';
  ctx.fill();

  // 绘制交叉线
  ctx.beginPath();
  const lineOffset = size * 0.15625; // 20/128 = 0.15625
  ctx.moveTo(centerX - lineOffset, centerY - lineOffset);
  ctx.lineTo(centerX + lineOffset, centerY + lineOffset);
  ctx.moveTo(centerX + lineOffset, centerY - lineOffset);
  ctx.lineTo(centerX - lineOffset, centerY + lineOffset);
  ctx.strokeStyle = 'white';
  ctx.lineWidth = Math.max(1, size * 0.0234375); // 3/128 = 0.0234375
  ctx.lineCap = 'round';
  ctx.stroke();

  return canvas.toBuffer('image/png');
}

// 生成所有尺寸的图标
sizes.forEach(size => {
  const buffer = generateIcon(size);
  fs.writeFileSync(`icon-${size}.png`, buffer);
  console.log(`Generated icon-${size}.png`);
});

console.log('All icons generated successfully!');
