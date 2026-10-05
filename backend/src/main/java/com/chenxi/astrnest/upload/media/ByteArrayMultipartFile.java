package com.chenxi.astrnest.upload.media;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * 内存字节实现的 MultipartFile：EXIF 剥离后以剥离结果替换原上传体，
 * 对下游（存储 handler/媒体校验）完全透明。
 */
public class ByteArrayMultipartFile implements MultipartFile {

  private final String name;
  private final String originalFilename;
  private final String contentType;
  private final byte[] bytes;

  public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
    this.name = name;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.bytes = bytes;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalFilename() {
    return originalFilename;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return bytes.length == 0;
  }

  @Override
  public long getSize() {
    return bytes.length;
  }

  @Override
  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(bytes);
  }

  @Override
  public void transferTo(File destination) throws IOException {
    try (FileOutputStream outputStream = new FileOutputStream(destination)) {
      outputStream.write(bytes);
    }
  }
}
