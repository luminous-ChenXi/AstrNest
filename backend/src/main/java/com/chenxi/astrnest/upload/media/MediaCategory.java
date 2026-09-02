package com.chenxi.astrnest.upload.media;

public enum MediaCategory {
  IMAGE("picture"),
  VIDEO("video");

  private final String storageSegment;

  MediaCategory(String storageSegment) {
    this.storageSegment = storageSegment;
  }

  public String storageSegment() {
    return storageSegment;
  }
}
