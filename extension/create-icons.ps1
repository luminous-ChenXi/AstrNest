Add-Type -AssemblyName System.Drawing

$sizes = @(16, 32, 48, 128)

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # 圆角矩形
    $rect = New-Object System.Drawing.Rectangle(2, 2, $size-4, $size-4)
    $radius = [int]($size * 0.2)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($rect.X, $rect.Y, $radius*2, $radius*2, 180, 90)
    $path.AddArc($rect.Right-$radius*2, $rect.Y, $radius*2, $radius*2, 270, 90)
    $path.AddArc($rect.Right-$radius*2, $rect.Bottom-$radius*2, $radius*2, $radius*2, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom-$radius*2, $radius*2, $radius*2, 90, 90)
    $path.CloseFigure()

    # 粉色到薄荷绿渐变
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, [System.Drawing.Color]::FromArgb(255, 255, 107, 157), [System.Drawing.Color]::FromArgb(255, 78, 205, 196), 45)
    $g.FillPath($brush, $path)

    # 白色图片图标 - 简单的山形
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [Math]::Max(1, $size * 0.08))
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

    # 画山形线条
    $g.DrawLine($pen, $size*0.25, $size*0.65, $size*0.35, $size*0.5)
    $g.DrawLine($pen, $size*0.35, $size*0.5, $size*0.5, $size*0.6)
    $g.DrawLine($pen, $size*0.5, $size*0.6, $size*0.8, $size*0.4)

    # 画太阳
    $sunBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $sunSize = [int]($size * 0.12)
    $g.FillEllipse($sunBrush, $size*0.6, $size*0.25, $sunSize, $sunSize)

    $g.Dispose()
    $bmp.Save("icons/icon-$size.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    Write-Host "Created icon-$size.png"
}

Write-Host "All icons created successfully!"
