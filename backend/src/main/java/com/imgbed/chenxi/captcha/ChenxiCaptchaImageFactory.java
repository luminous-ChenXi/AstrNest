package com.imgbed.chenxi.captcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;

class ChenxiCaptchaImageFactory {

  private static final int WIDTH = 168;
  private static final int HEIGHT = 56;
  private static final int CHAR_COUNT = 5;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private static final Font[] FONTS = new Font[]{
      new Font("Inter", Font.BOLD, 32),
      new Font("Nunito", Font.BOLD, 34),
      new Font("JetBrains Mono", Font.BOLD, 30),
      new Font("Poppins", Font.BOLD, 33)
  };

  CaptchaImagePayload createImage() {
    String code = randomCode();
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    paintBackground(graphics);
    drawNoise(graphics);
    drawCharacters(graphics, code);

    graphics.dispose();
    return new CaptchaImagePayload(encode(image), code, WIDTH, HEIGHT);
  }

  private void paintBackground(Graphics2D graphics) {
    Color start = palette();
    Color end = palette();
    graphics.setPaint(new GradientPaint(0, 0, start, WIDTH, HEIGHT, end));
    graphics.fillRoundRect(0, 0, WIDTH, HEIGHT, 20, 20);

    for (int i = 0; i < 6; i++) {
      graphics.setColor(new Color(255, 255, 255, 24 + RANDOM.nextInt(40)));
      int radius = 18 + RANDOM.nextInt(28);
      int x = RANDOM.nextInt(WIDTH - radius);
      int y = RANDOM.nextInt(HEIGHT - radius);
      graphics.fillOval(x, y, radius, radius);
    }
  }

  private void drawNoise(Graphics2D graphics) {
    graphics.setStroke(new BasicStroke(1.2f));
    for (int i = 0; i < 4; i++) {
      graphics.setColor(new Color(15 + RANDOM.nextInt(200), 20 + RANDOM.nextInt(200), 40 + RANDOM.nextInt(180), 120));
      int x1 = RANDOM.nextInt(WIDTH / 2);
      int y1 = RANDOM.nextInt(HEIGHT);
      int x2 = x1 + RANDOM.nextInt(WIDTH / 2);
      int y2 = RANDOM.nextInt(HEIGHT);
      graphics.drawLine(x1, y1, x2, y2);
    }

    for (int i = 0; i < 15; i++) {
      graphics.setColor(new Color(255, 255, 255, 120));
      int x = RANDOM.nextInt(WIDTH);
      int y = RANDOM.nextInt(HEIGHT);
      graphics.drawRect(x, y, 1, 1);
    }
  }

  private void drawCharacters(Graphics2D graphics, String code) {
    int x = 20 + RANDOM.nextInt(8);
    for (char ch : code.toCharArray()) {
      graphics.setFont(FONTS[RANDOM.nextInt(FONTS.length)]);
      graphics.setColor(randomGlyphColor());
      int angle = RANDOM.nextInt(30) - 15;
      double rad = Math.toRadians(angle);
      int y = 35 + RANDOM.nextInt(10);
      graphics.rotate(rad, x, y);
      graphics.drawString(String.valueOf(ch), x, y);
      graphics.rotate(-rad, x, y);
      x += 26 + RANDOM.nextInt(6);
    }
  }

  private Color palette() {
    Color[] colors = new Color[]{
        new Color(37, 99, 235),
        new Color(236, 72, 153),
        new Color(14, 165, 233),
        new Color(249, 115, 22),
        new Color(45, 212, 191)
    };
    return colors[RANDOM.nextInt(colors.length)];
  }

  private Color randomGlyphColor() {
    int base = 120 + RANDOM.nextInt(120);
    return new Color(base, 255 - RANDOM.nextInt(80), 180 + RANDOM.nextInt(60));
  }

  private String randomCode() {
    StringBuilder builder = new StringBuilder(CHAR_COUNT);
    for (int i = 0; i < CHAR_COUNT; i++) {
      builder.append(CHAR_POOL[RANDOM.nextInt(CHAR_POOL.length)]);
    }
    return builder.toString();
  }

  private String encode(BufferedImage image) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", output);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    } catch (IOException ex) {
      throw new IllegalStateException("验证码生成失败", ex);
    }
  }

  record CaptchaImagePayload(String imageBase64, String code, int width, int height) {
  }
}
