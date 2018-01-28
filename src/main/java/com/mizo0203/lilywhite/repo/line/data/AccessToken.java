package com.mizo0203.lilywhite.repo.line.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessToken {
  private int status;
  private String message;

  @JsonProperty("access_token")
  private String accessToken;

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getAccessToken() {
    return accessToken;
  }
}
