package com.mizo0203.lilywhite.domain;

import com.mizo0203.lilywhite.repo.AuthorizeOauthCallback;
import com.mizo0203.lilywhite.repo.Repository;

import java.io.IOException;

public class UseCase implements AutoCloseable {
  private final Repository mRepository;

  public UseCase() {
    mRepository = new Repository();
  }

  @Override
  public void close() {
    mRepository.destroy();
  }

  public void authorizeOauth(AuthorizeOauthCallback callback) throws IOException {
    mRepository.authorizeOauth(callback);
  }
}
