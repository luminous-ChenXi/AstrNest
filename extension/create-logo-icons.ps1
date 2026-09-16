# Generate extension icons script - using frontend Logo SVG
# Run: Right click -> Run with PowerShell

Add-Type -AssemblyName System.Drawing

# Define colors
$pink = [System.Drawing.Color]::FromArgb(255, 255, 107, 157)
$pinkLight = [System.Drawing.Color]::FromArgb(255, 255, 143, 171)
$mint = [System.Drawing.Color]::FromArgb(255, 78, 205, 196)

# Create different size icons
$sizes = @(16, 32, 48, 128)

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # Draw gradient background circle
    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)

    # Create gradient brush (pink to mint)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        $pink,
        $mint,
        45.0
    )

    # Add middle color stop
    $colorBlend = New-Object System.Drawing.Drawing2D.ColorBlend(3)
    $colorBlend.Colors = @($pink, $pinkLight, $mint)
    $colorBlend.Positions = @(0.0, 0.5, 1.0)
    $brush.InterpolationColors = $colorBlend

    # Fill circle
    $g.FillEllipse($brush, $rect)

    # Draw white logo - simplified star/sparkle pattern
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $center = $size / 2
    $outerRadius = $size * 0.3
    $innerRadius = $size * 0.12

    # Create 8-point star
    $points = @()
    $numPoints = 8
    for ($i = 0; $i -lt $numPoints * 2; $i++) {
        $angle = [Math]::PI * $i / $numPoints - [Math]::PI / 2
        $r = if ($i % 2 -eq 0) { $outerRadius } else { $innerRadius }
        $x = $center + $r * [Math]::Cos($angle)
        $y = $center + $r * [Math]::Sin($angle)
        $points += New-Object System.Drawing.PointF($x, $y)
    }

    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPolygon($whiteBrush, $points)

    # Add highlight effect
    $highlightBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(80, 255, 255, 255))
    $highlightRect = New-Object System.Drawing.RectangleF($size * 0.2, $size * 0.15, $size * 0.25, $size * 0.2)
    $g.FillEllipse($highlightBrush, $highlightRect)

    $g.Dispose()
    $bmp.Save("icons\icon-$size.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    Write-Host "Created icon-$size.png (${size}x${size})"
}

Write-Host ""
Write-Host "All icons generated!"
Write-Host "Tip: Click 'Reload' button on Chrome extensions page to refresh icon"
