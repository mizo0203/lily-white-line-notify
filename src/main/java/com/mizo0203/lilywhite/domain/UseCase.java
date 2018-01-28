package com.mizo0203.lilywhite.domain;

import com.mizo0203.lilywhite.repo.Repository;

import javax.annotation.Nullable;
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
    callback.onBuildRedirectUrlString(mRepository.buildAuthorizeOauthRedirectUrlString());
  }

  public void tokenOauth(String code) {
    mRepository.tokenOauth(code);
  }

  public interface AuthorizeOauthCallback {
    void onBuildRedirectUrlString(@Nullable String redirectUrlString) throws IOException;
  }
}
