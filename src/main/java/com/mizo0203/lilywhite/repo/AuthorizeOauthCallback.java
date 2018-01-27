package com.mizo0203.lilywhite.repo;

import javax.annotation.Nullable;
import java.io.IOException;

public interface AuthorizeOauthCallback {
  void onBuildRedirectUrlString(@Nullable String redirectUrlString) throws IOException;
}
