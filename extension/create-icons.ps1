# 生成扩展图标脚本
# 使用前端项目的 SVG Logo

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Xml

# 读取SVG文件
$svgContent = Get-Content "icons\logo.svg" -Raw

# 创建不同尺寸的图标
$sizes = @(16, 32, 48, 128)

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # 绘制渐变背景圆
    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)

    # 创建渐变画笔 (粉色到薄荷绿)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 255, 107, 157),  # #ff6b9d
        [System.Drawing.Color]::FromArgb(255, 78, 205, 196),   # #4ecdc4
        45.0
    )

    # 填充圆形背景
    $g.FillEllipse($brush, $rect)

    # 绘制白色Logo路径（简化的星星/闪光形状）
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    # 根据尺寸调整绘制
    $center = $size / 2
    $radius = $size * 0.35

    # 创建一个简单的星形/闪光形状
    $points = @()
    $numPoints = 8
    for ($i = 0; $i -lt $numPoints * 2; $i++) {
        $angle = [Math]::PI * $i / $numPoints - [Math]::PI / 2
        $r = if ($i % 2 -eq 0) { $radius } else { $radius * 0.4 }
        $x = $center + $r * [Math]::Cos($angle)
        $y = $center + $r * [Math]::Sin($angle)
        $points += New-Object System.Drawing.PointF($x, $y)
    }

    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPolygon($whiteBrush, $points)

    $g.Dispose()
    $bmp.Save("icons\icon-$size.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    Write-Host "Created icon-$size.png"
}

Write-Host "All icons created successfully!"
