package com.mizo0203.lilywhite.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.repo.line.data.AccessToken;
import com.mizo0203.lilywhite.repo.line.data.ResponseNotifyData;
import com.mizo0203.lilywhite.repo.line.data.ResponseRevokeData;
import com.mizo0203.lilywhite.repo.line.data.ResponseStatusData;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repository {

  private static final Logger LOG = Logger.getLogger(Repository.class.getName());
  private final OfyRepository mOfyRepository;
  private final LineRepository mLineRepository;

  public Repository() {
    mOfyRepository = new OfyRepository();
    mLineRepository = new LineRepository();
  }

  public void destroy() {
    mOfyRepository.destroy();
    mLineRepository.destroy();
  }

  @Nullable
  public String buildAuthorizeOauthRedirectUrlString() {
    String client_id = getKey("client_id");
    String redirect_uri_str = Define.REDIRECT_URI_STR;
    return mLineRepository.buildAuthorizeOauthRedirectUrlString(client_id, redirect_uri_str);
  }

  public void tokenOauth(String code) {
    String client_id = getKey("client_id");
    String client_secret = getKey("client_secret");
    mLineRepository.tokenOauth(code, client_id, client_secret, this::onResponseTokenOauth);
  }

  private void onResponseTokenOauth(HttpURLConnection connection) {
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return;
      }
      String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
      AccessToken access_token = new ObjectMapper().readValue(body, AccessToken.class);
      setKey("access_token", access_token.getAccessToken());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "", e);
    }
  }

  public void notify(@Nonnull String message) {
    String access_token = getKey("access_token");
    mLineRepository.notify(
        access_token, message, null, null, null, null, null, this::onResponseNotify);
  }

  private void onResponseNotify(HttpURLConnection connection) {
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return;
      }
      String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
      ResponseNotifyData responseNotifyData =
          new ObjectMapper().readValue(body, ResponseNotifyData.class);
      LOG.info("responseNotifyData: " + responseNotifyData.getMessage());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "", e);
    }
  }

  public void status() {
    String access_token = getKey("access_token");
    mLineRepository.status(access_token, this::onResponseStatus);
  }

  private void onResponseStatus(HttpURLConnection connection) {
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return;
      }
      String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
      ResponseStatusData responseStatusData =
          new ObjectMapper().readValue(body, ResponseStatusData.class);
      LOG.info("responseStatusData.getStatus(): " + responseStatusData.getStatus());
      LOG.info("responseStatusData.getMessage(): " + responseStatusData.getMessage());
      LOG.info("responseStatusData.getTargetType(): " + responseStatusData.getTargetType());
      LOG.info("responseStatusData.getTarget(): " + responseStatusData.getTarget());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "", e);
    }
  }

  public void revoke() {
    String access_token = getKey("access_token");
    mLineRepository.revoke(access_token, this::onResponseRevoke);
  }

  private void onResponseRevoke(HttpURLConnection connection) {
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return;
      }
      String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
      ResponseRevokeData responseRevokeData =
          new ObjectMapper().readValue(body, ResponseRevokeData.class);
      LOG.info("responseRevokeData.getStatus(): " + responseRevokeData.getStatus());
      LOG.info("responseRevokeData.getMessage(): " + responseRevokeData.getMessage());
      deleteKey("access_token");
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "", e);
    }
  }

  private void setKey(String key, String value) {
    KeyEntity keyEntity = mOfyRepository.loadKeyEntity(key);
    if (keyEntity == null) {
      keyEntity = new KeyEntity();
      keyEntity.key = key;
    }
    keyEntity.value = value;
    mOfyRepository.saveKeyEntity(keyEntity);
  }

  private String getKey(String key) {
    KeyEntity keyEntity = mOfyRepository.loadKeyEntity(key);
    if (keyEntity == null) {
      keyEntity = new KeyEntity();
      keyEntity.key = key;
      keyEntity.value = "";
      mOfyRepository.saveKeyEntity(keyEntity);
    }
    if (keyEntity.value.isEmpty()) {
      LOG.severe(key + " isEmpty");
    }
    return keyEntity.value;
  }

  private void deleteKey(String key) {
    mOfyRepository.deleteKeyEntity(key);
  }
}
