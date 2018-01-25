package com.mizo0203.lilywhite.domain;

import com.mizo0203.lilywhite.repo.Repository;
import com.mizo0203.lilywhite.util.HttpUtil;

public class UseCase {
  private final Repository mRepository;

  public UseCase() {
    mRepository = new Repository();
  }

  public void destroy() {
    mRepository.destroy();
  }

  public void authorizeOauth(HttpUtil.Callback callback) {
    mRepository.authorizeOauth(callback);
  }
}
