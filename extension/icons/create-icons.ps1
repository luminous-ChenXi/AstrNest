# 创建简单的图标文件
# 使用 .NET 绘制图标

Add-Type -AssemblyName System.Drawing

function Create-Icon($size, $outputPath) {
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    
    # 绘制圆角矩形背景（渐变效果用纯色代替）
    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(102, 126, 234))
    $graphics.FillRectangle($brush, $rect)
    
    # 绘制中心圆点
    $centerX = $size / 2
    $centerY = $size / 2
    $circleRadius = $size * 0.15
    $circleRect = New-Object System.Drawing.Rectangle(
        [int]($centerX - $circleRadius),
        [int]($centerY - $circleRadius),
        [int]($circleRadius * 2),
        [int]($circleRadius * 2)
    )
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $graphics.FillEllipse($whiteBrush, $circleRect)
    
    # 保存
    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # 清理
    $graphics.Dispose()
    $bitmap.Dispose()
    $brush.Dispose()
    $whiteBrush.Dispose()
    
    Write-Host "Created: $outputPath (${size}x${size})"
}

# 创建各个尺寸的图标
Create-Icon 16 "$PSScriptRoot\icon-16.png"
Create-Icon 32 "$PSScriptRoot\icon-32.png"
Create-Icon 48 "$PSScriptRoot\icon-48.png"
Create-Icon 128 "$PSScriptRoot\icon-128.png"

Write-Host "All icons created successfully!"
