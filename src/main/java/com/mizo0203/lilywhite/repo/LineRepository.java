package com.mizo0203.lilywhite.repo;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/* package */ class LineRepository {

  private static final Logger LOG = Logger.getLogger(LineRepository.class.getName());
  private static final String LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR =
      "https://notify-bot.line.me/oauth/authorize";

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
  }

  public void authorizeOauth(
      String client_id, String redirect_uri_str, AuthorizeOauthCallback callback)
      throws IOException {
    try {
      callback.onBuildRedirectUrlString(
          new URIBuilder(LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR)
              .setParameter("response_type", "code")
              .setParameter("client_id", client_id)
              .setParameter("redirect_uri", redirect_uri_str)
              .setParameter("scope", "notify")
              .setParameter("state", "state") // FIXME: ユーザのセッションIDから生成されるハッシュ値などを指定
              .setParameter("response_mode", "form_post")
              .build()
              .toString());
    } catch (URISyntaxException e) {
      e.printStackTrace();
      callback.onBuildRedirectUrlString(null);
    }
  }
}
