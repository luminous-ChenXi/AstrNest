const fs = require('fs');
const path = require('path');

// 创建 Canvas
function createCanvas(width, height) {
    // 使用简单的 PNG 编码
    const canvas = {
        width,
        height,
        data: Buffer.alloc(width * height * 4)
    };
    return canvas;
}

// 设置像素颜色
function setPixel(canvas, x, y, r, g, b, a = 255) {
    if (x < 0 || x >= canvas.width || y < 0 || y >= canvas.height) return;
    const idx = (y * canvas.width + x) * 4;
    canvas.data[idx] = r;
    canvas.data[idx + 1] = g;
    canvas.data[idx + 2] = b;
    canvas.data[idx + 3] = a;
}

// 绘制圆形
function drawCircle(canvas, cx, cy, radius, r, g, b) {
    for (let y = 0; y < canvas.height; y++) {
        for (let x = 0; x < canvas.width; x++) {
            const dx = x - cx;
            const dy = y - cy;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= radius) {
                // 渐变效果
                const t = dist / radius;
                const pr = Math.round(255 * (1 - t) + 255 * t);  // pink to pink
                const pg = Math.round(107 * (1 - t) + 143 * t);  // pink
                const pb = Math.round(157 * (1 - t) + 171 * t);  // pink to light pink
                setPixel(canvas, x, y, pr, pg, pb);
            }
        }
    }
}

// 简单的 PNG 编码
function encodePNG(canvas) {
    // 使用简单的 BMP 格式代替，然后转换
    // 这里我们直接创建一个简单的 ICO 兼容格式
    const { createCanvas: createNodeCanvas } = require('canvas');
    const nodeCanvas = createNodeCanvas(canvas.width, canvas.height);
    const ctx = nodeCanvas.getContext('2d');

    // 绘制渐变背景
    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
    gradient.addColorStop(0, '#ff6b9d');
    gradient.addColorStop(0.5, '#ff8fab');
    gradient.addColorStop(1, '#4ecdc4');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(canvas.width/2, canvas.height/2, canvas.width/2, 0, Math.PI * 2);
    ctx.fill();

    // 绘制白色星形
    ctx.fillStyle = 'white';
    ctx.beginPath();
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const outerRadius = canvas.width * 0.3;
    const innerRadius = canvas.width * 0.12;
    const numPoints = 8;

    for (let i = 0; i < numPoints * 2; i++) {
        const angle = (Math.PI * i / numPoints) - Math.PI / 2;
        const radius = i % 2 === 0 ? outerRadius : innerRadius;
        const x = centerX + radius * Math.cos(angle);
        const y = centerY + radius * Math.sin(angle);
        if (i === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    }
    ctx.closePath();
    ctx.fill();

    // 高光
    ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.beginPath();
    ctx.ellipse(canvas.width * 0.3, canvas.height * 0.25, canvas.width * 0.12, canvas.height * 0.08, -Math.PI / 4, 0, Math.PI * 2);
    ctx.fill();

    return nodeCanvas.toBuffer('image/png');
}

// 生成图标
async function generateIcons() {
    try {
        const canvas = require('canvas');
    } catch (e) {
        console.log('Installing canvas package...');
        const { execSync } = require('child_process');
        execSync('npm install canvas', { cwd: __dirname, stdio: 'inherit' });
    }

    const { createCanvas } = require('canvas');
    const sizes = [16, 32, 48, 128];

    for (const size of sizes) {
        const canvas = createCanvas(size, size);
        const ctx = canvas.getContext('2d');

        // 绘制渐变背景
        const gradient = ctx.createLinearGradient(0, 0, size, size);
        gradient.addColorStop(0, '#ff6b9d');
        gradient.addColorStop(0.5, '#ff8fab');
        gradient.addColorStop(1, '#4ecdc4');
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(size/2, size/2, size/2, 0, Math.PI * 2);
        ctx.fill();

        // 绘制白色星形
        ctx.fillStyle = 'white';
        ctx.beginPath();
        const centerX = size / 2;
        const centerY = size / 2;
        const outerRadius = size * 0.3;
        const innerRadius = size * 0.12;
        const numPoints = 8;

        for (let i = 0; i < numPoints * 2; i++) {
            const angle = (Math.PI * i / numPoints) - Math.PI / 2;
            const radius = i % 2 === 0 ? outerRadius : innerRadius;
            const x = centerX + radius * Math.cos(angle);
            const y = centerY + radius * Math.sin(angle);
            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        }
        ctx.closePath();
        ctx.fill();

        // 高光
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.beginPath();
        ctx.ellipse(size * 0.3, size * 0.25, size * 0.12, size * 0.08, -Math.PI / 4, 0, Math.PI * 2);
        ctx.fill();

        // 保存
        const buffer = canvas.toBuffer('image/png');
        fs.writeFileSync(path.join(__dirname, 'icons', `icon-${size}.png`), buffer);
        console.log(`Created icon-${size}.png`);
    }

    console.log('\nAll icons generated!');
}

generateIcons().catch(console.error);
