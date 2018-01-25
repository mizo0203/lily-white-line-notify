package com.mizo0203.lilywhite.repo;

import com.mizo0203.lilywhite.util.HttpUtil;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/* package */ class LineRepository {

  private static final Logger LOG = Logger.getLogger(LineRepository.class.getName());
  private static final String LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR =
      "https: //notify-bot.line.me/oauth/authorize";

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
  }

  public void authorizeOauth(
      String client_id, String redirect_uri_str, HttpUtil.Callback callback) {
    try {
      URL url = new URL(LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR);
      Map<String, String> reqProp = new HashMap<>();
      reqProp.put("response_type", "code");
      reqProp.put("client_id", client_id);
      reqProp.put("redirect_uri", redirect_uri_str);
      reqProp.put("scope", "notify");
      reqProp.put("state", "state"); // FIXME: ユーザのセッションIDから生成されるハッシュ値などを指定
      reqProp.put("response_mode", "form_post");
      HttpUtil.get(url, reqProp, null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
