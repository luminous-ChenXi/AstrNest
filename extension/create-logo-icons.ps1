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

    # 创建渐变画笔
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 255, 107, 157),
        [System.Drawing.Color]::FromArgb(255, 78, 205, 196),
        45.0
    )

    # 填充圆形背景
    $g.FillEllipse($brush, $rect)

    $g.Dispose()
    $bmp.Save("icons\icon-$size.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    Write-Host "Created icon-$size.png"
}

Write-Host "All logo icons created successfully!"
