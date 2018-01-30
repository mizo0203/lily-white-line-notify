package com.mizo0203.lilywhite.repo.line.data;

import javax.annotation.Nullable;

public class ResponseStatusData {
  private int status;
  private String message;
  private String targetType;
  private String target;

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public TARGET_TYPE getTargetType() {
    return TARGET_TYPE.valueOf(targetType);
  }

  @Nullable
  public String getTarget() {
    return target;
  }

  public enum TARGET_TYPE {
    USER, //
    GROUP, //
  }
}
