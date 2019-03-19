package com.mizo0203.lilywhite.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.repo.line.data.*;
import com.mizo0203.lilywhite.util.HttpUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
  private static final String LINE_NOTIFY_API_STATUS_URL_STR =
      "https://notify-api.line.me/api/status";
  private static final String LINE_NOTIFY_API_REVOKE_URL_STR =
      "https://notify-api.line.me/api/revoke";

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
  }

  @Nullable
  public String buildAuthorizeOauthRedirectUrlString(
      String client_id, String redirect_uri_str, String state) {
    try {
      return new URIBuilder(LINE_NOTIFY_API_AUTHORIZE_OAUTH_URL_STR)
          .setParameter("response_type", "code")
          .setParameter("client_id", client_id)
          .setParameter("redirect_uri", redirect_uri_str)
          .setParameter("scope", "notify")
          .setParameter("state", state) // FIXME: ユーザのセッションIDから生成されるハッシュ値などを指定
          .setParameter("response_mode", "form_post")
          .build()
          .toString();
    } catch (URISyntaxException e) {
      LOG.log(Level.SEVERE, "", e);
      return null;
    }
  }

  public void tokenOauth(
      String code,
      String client_id,
      String client_secret,
      @Nonnull Callback<AccessToken> callback) {
    Map<String, String> reqProp = new HashMap<>();
    reqProp.put("Content-Type", "application/x-www-form-urlencoded");

    Map<String, String> params = new HashMap<>();
    params.put("grant_type", "authorization_code");
    params.put("code", code);
    params.put("redirect_uri", Define.REDIRECT_URI_STR);
    params.put("client_id", client_id);
    params.put("client_secret", client_secret);

    try {
      HttpUtil.post(
          new URL(LINE_NOTIFY_API_TOKEN_OAUTH_URL_STR),
          reqProp,
          params,
          connection -> {
            try {
              if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                callback.response(null, null);
                return;
              }
              String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
              AccessToken accessToken = new ObjectMapper().readValue(body, AccessToken.class);
              callback.response(null, accessToken);
            } catch (IOException e) {
              LOG.log(Level.SEVERE, "", e);
              callback.response(null, null);
            }
          });
    } catch (MalformedURLException e) {
      LOG.log(Level.SEVERE, "", e);
      callback.response(null, null);
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
      @Nonnull Callback<ResponseNotifyData> callback) {
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
      HttpUtil.post(
          new URL(LINE_NOTIFY_API_NOTIFY_URL_STR),
          reqProp,
          params,
          connection -> {
            ResponseApiRateLimit apiRateLimit = parseResponseApiRateLimit(connection);
            try {
              if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                callback.response(apiRateLimit, null);
                return;
              }
              String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
              ResponseNotifyData responseNotifyData =
                  new ObjectMapper().readValue(body, ResponseNotifyData.class);
              LOG.info("responseNotifyData.getStatus(): " + responseNotifyData.getStatus());
              LOG.info("responseNotifyData.getMessage(): " + responseNotifyData.getMessage());
              callback.response(apiRateLimit, responseNotifyData);
            } catch (IOException e) {
              LOG.log(Level.SEVERE, "", e);
              callback.response(apiRateLimit, null);
            }
          });
    } catch (MalformedURLException e) {
      LOG.log(Level.SEVERE, "", e);
      callback.response(null, null);
    }
  }

  public void status(@Nonnull String access_token, @Nonnull Callback<ResponseStatusData> callback) {
    Map<String, String> reqProp = new HashMap<>();
    reqProp.put("Authorization", "Bearer " + access_token);

    try {
      HttpUtil.get(
          new URL(LINE_NOTIFY_API_STATUS_URL_STR),
          reqProp,
          connection -> {
            ResponseApiRateLimit apiRateLimit = parseResponseApiRateLimit(connection);
            try {
              if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                callback.response(apiRateLimit, null);
                return;
              }
              String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
              ResponseStatusData responseStatusData =
                  new ObjectMapper().readValue(body, ResponseStatusData.class);
              LOG.info("responseStatusData.getStatus(): " + responseStatusData.getStatus());
              LOG.info("responseStatusData.getMessage(): " + responseStatusData.getMessage());
              LOG.info("responseStatusData.getTargetType(): " + responseStatusData.getTargetType());
              LOG.info("responseStatusData.getTarget(): " + responseStatusData.getTarget());
              callback.response(apiRateLimit, responseStatusData);
            } catch (IOException e) {
              LOG.log(Level.SEVERE, "", e);
              callback.response(apiRateLimit, null);
            }
          });
    } catch (MalformedURLException e) {
      LOG.log(Level.SEVERE, "", e);
      callback.response(null, null);
    }
  }

  public void revoke(@Nonnull String access_token, @Nonnull Callback<ResponseRevokeData> callback) {
    Map<String, String> reqProp = new HashMap<>();
    reqProp.put("Content-Type", "application/x-www-form-urlencoded");
    reqProp.put("Authorization", "Bearer " + access_token);

    try {
      HttpUtil.post(
          new URL(LINE_NOTIFY_API_REVOKE_URL_STR),
          reqProp,
          new HashMap<>(),
          connection -> {
            try {
              if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                callback.response(null, null);
                return;
              }
              String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
              ResponseRevokeData responseRevokeData =
                  new ObjectMapper().readValue(body, ResponseRevokeData.class);
              LOG.info("responseRevokeData.getStatus(): " + responseRevokeData.getStatus());
              LOG.info("responseRevokeData.getMessage(): " + responseRevokeData.getMessage());
              callback.response(null, responseRevokeData);
            } catch (IOException e) {
              LOG.log(Level.SEVERE, "", e);
              callback.response(null, null);
            }
          });
    } catch (MalformedURLException e) {
      LOG.log(Level.SEVERE, "", e);
      callback.response(null, null);
    }
  }

  private ResponseApiRateLimit parseResponseApiRateLimit(URLConnection connection) {
    try {
      int limit = Integer.parseInt(connection.getHeaderField("X-RateLimit-Limit"));
      int remaining = Integer.parseInt(connection.getHeaderField("X-RateLimit-Remaining"));
      int imageLimit = Integer.parseInt(connection.getHeaderField("X-RateLimit-ImageLimit"));
      int imageRemaining =
          Integer.parseInt(connection.getHeaderField("X-RateLimit-ImageRemaining"));
      Date reset = new Date(Long.parseLong(connection.getHeaderField("X-RateLimit-Reset")));
      LOG.info("X-RateLimit-Limit:          " + limit);
      LOG.info("X-RateLimit-Remaining:      " + remaining);
      LOG.info("X-RateLimit-ImageLimit:     " + imageLimit);
      LOG.info("X-RateLimit-ImageRemaining: " + imageRemaining);
      LOG.info("X-RateLimit-Reset:          " + reset);
      return new ResponseApiRateLimit(limit, remaining, imageLimit, imageRemaining, reset);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return null;
    }
  }

  public interface Callback<T> {

    void response(@Nullable ResponseApiRateLimit apiRateLimit, @Nullable T res);
  }
}
