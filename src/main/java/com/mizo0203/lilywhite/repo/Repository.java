package com.mizo0203.lilywhite.repo;

import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

public class Repository {

  private static final Logger LOG = Logger.getLogger(Repository.class.getName());
  private final OfyRepository mOfyRepository;
  private final LineRepository mLineRepository;
  private final PushQueueRepository mPushQueueRepository;

  public Repository() {
    mOfyRepository = new OfyRepository();
    mLineRepository = new LineRepository();
    mPushQueueRepository = new PushQueueRepository();
  }

  public void destroy() {
    mOfyRepository.destroy();
    mLineRepository.destroy();
    mPushQueueRepository.destroy();
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
    mLineRepository.tokenOauth(
        code,
        client_id,
        client_secret,
        (apiRateLimit, accessToken) -> {
          if (accessToken != null) {
            LOG.info("accessToken.getStatus(): " + accessToken.getStatus());
            LOG.info("accessToken.getMessage(): " + accessToken.getMessage());
            LOG.info("accessToken.getAccessToken(): " + accessToken.getAccessToken());
            setKey("access_token", accessToken.getAccessToken());
          }
        });
  }

  public void notify(@Nonnull String message) {
    String access_token = getKey("access_token");
    mLineRepository.notify(
        access_token,
        message,
        null,
        null,
        null,
        null,
        null,
        (apiRateLimit, responseNotifyData) -> {
          if (responseNotifyData != null) {
            LOG.info("responseNotifyData.getStatus(): " + responseNotifyData.getStatus());
            LOG.info("responseNotifyData.getMessage(): " + responseNotifyData.getMessage());
          }
        });
  }

  public void status() {
    String access_token = getKey("access_token");
    mLineRepository.status(
        access_token,
        (apiRateLimit, responseStatusData) -> {
          if (responseStatusData != null) {
            LOG.info("responseStatusData.getStatus(): " + responseStatusData.getStatus());
            LOG.info("responseStatusData.getMessage(): " + responseStatusData.getMessage());
            LOG.info("responseStatusData.getTargetType(): " + responseStatusData.getTargetType());
            LOG.info("responseStatusData.getTarget(): " + responseStatusData.getTarget());
          }
        });
  }

  public void revoke() {
    String access_token = getKey("access_token");
    mLineRepository.revoke(
        access_token,
        (apiRateLimit, responseRevokeData) -> {
          if (responseRevokeData != null) {
            LOG.info("responseRevokeData.getStatus(): " + responseRevokeData.getStatus());
            LOG.info("responseRevokeData.getMessage(): " + responseRevokeData.getMessage());
            deleteKey("access_token");
          }
        });
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

  public void setReminderMessage(String sourceId, String reminderMessage) {
    LineTalkRoomConfig config = getOrCreateLineTalkRoomConfig(sourceId);
    config.setReminderMessage(reminderMessage);
    mOfyRepository.saveLineTalkRoomConfig(config);
  }

  public void enqueueReminderTask(String sourceId, long etaMillis) {
    LineTalkRoomConfig config = getOrCreateLineTalkRoomConfig(sourceId);
    String taskName =
        mPushQueueRepository.enqueueReminderTask(
            config.getSourceId(), etaMillis, config.getReminderMessage());
    LOG.info("enqueueReminderTask taskName: " + taskName);
    config.setReminderEnqueuedTaskName(taskName);
    mOfyRepository.saveLineTalkRoomConfig(config);
  }

  private void deleteReminderTask(LineTalkRoomConfig config) {
    String taskName = config.getReminderEnqueuedTaskName();
    if (taskName == null || taskName.isEmpty()) {
      return;
    }
    mPushQueueRepository.deleteReminderTask(taskName);
    config.setReminderEnqueuedTaskName(null);
  }

  private LineTalkRoomConfig getOrCreateLineTalkRoomConfig(String sourceId) {
    LineTalkRoomConfig config = mOfyRepository.loadLineTalkRoomConfig(sourceId);
    if (config == null) {
      config = new LineTalkRoomConfig(sourceId);
    }
    return config;
  }
}
