package com.mizo0203.lilywhite.repo;

import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.util.HttpUtil;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/* package */ class LineRepository {

  private static final Logger LOG = Logger.getLogger(LineRepository.class.getName());
  private static final String LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR =
      "https://notify-bot.line.me/oauth/authorize";
  private static final String LINE_NOTIFY_API_TOKEN_OAUTH_URL_STR =
      "https://notify-bot.line.me/oauth/token";
  private static final String LINE_NOTIFY_API_NOTIFY_URL_STR =
      "https://notify-api.line.me/api/notify";

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
  }

  @Nullable
  public String buildAuthorizeOauthRedirectUrlString(String client_id, String redirect_uri_str) {
    try {
      return new URIBuilder(LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR)
          .setParameter("response_type", "code")
          .setParameter("client_id", client_id)
          .setParameter("redirect_uri", redirect_uri_str)
          .setParameter("scope", "notify")
          .setParameter("state", "state") // FIXME: ユーザのセッションIDから生成されるハッシュ値などを指定
          .setParameter("response_mode", "form_post")
          .build()
          .toString();
    } catch (URISyntaxException e) {
      LOG.log(Level.SEVERE, "", e);
      return null;
    }
  }

  public void tokenOauth(
      String code, String client_id, String client_secret, HttpUtil.Callback callback) {
    Map<String, String> reqProp = new HashMap<>();
    reqProp.put("Content-Type", "application/x-www-form-urlencoded");

    Map<String, String> params = new HashMap<>();
    params.put("grant_type", "authorization_code");
    params.put("code", code);
    params.put("redirect_uri", Define.REDIRECT_URI_STR);
    params.put("client_id", client_id);
    params.put("client_secret", client_secret);

    try {
      HttpUtil.post(new URL(LINE_NOTIFY_API_TOKEN_OAUTH_URL_STR), reqProp, params, callback);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public void notify(
      @Nonnull String access_token,
      @Nonnull String message,
      @Nullable String imageThumbnail,
      @Nullable String imageFullsize,
      @Nullable String imageFile,
      @Nullable Number stickerPackageId,
      @Nullable Number stickerId,
      @Nullable HttpUtil.Callback callback) {
    Map<String, String> reqProp = new HashMap<>();
    reqProp.put("Content-Type", "application/x-www-form-urlencoded");
    reqProp.put("Authorization", "Bearer " + access_token);

    Map<String, String> params = new HashMap<>();
    params.put("message", message);
    params.put("imageThumbnail", imageThumbnail);
    params.put("imageFullsize", imageFullsize);
    params.put("imageFile", imageFile);
    params.put("stickerPackageId", stickerPackageId != null ? stickerPackageId.toString() : null);
    params.put("stickerId", stickerId != null ? stickerId.toString() : null);

    try {
      HttpUtil.post(new URL(LINE_NOTIFY_API_NOTIFY_URL_STR), reqProp, params, callback);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }
}
