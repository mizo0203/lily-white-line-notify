package com.mizo0203.lilywhite.repo;

import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.servlet.LineBotCallbackRequestParser;
import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.repo.line.data.ResponseApiRateLimit;
import com.mizo0203.lilywhite.repo.line.data.ResponseStatusData;
import com.mizo0203.lilywhite.repo.objectify.entity.Channel;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;
import com.mizo0203.lilywhite.repo.objectify.entity.Reminder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class Repository {

  private static final Logger LOG = Logger.getLogger(Repository.class.getName());
  private final OfyRepository mOfyRepository;
  private final LineRepository mLineRepository;
  private final PushQueueRepository mPushQueueRepository;
  private final Channel mChannel;
  private final LineMessagingClient mLineMessagingClient;

  public Repository(long channelId) {
    mOfyRepository = new OfyRepository();
    mLineRepository = new LineRepository();
    mPushQueueRepository = new PushQueueRepository();
    mChannel = Objects.requireNonNull(mOfyRepository.loadChannel(channelId));
    mLineMessagingClient = LineMessagingClient.builder(mChannel.getToken()).build();
  }

  public void destroy() {
    mLineRepository.destroy();
    mPushQueueRepository.destroy();
  }

  @Nullable
  public String buildAuthorizeOauthRedirectUrlString(String state) {
    String client_id = getKey("client_id");
    String redirect_uri_str = Define.REDIRECT_URI_STR;
    return mLineRepository.buildAuthorizeOauthRedirectUrlString(client_id, redirect_uri_str, state);
  }

  public void tokenOauth(String code, long editingReminderId) {
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
            Reminder reminder = mOfyRepository.loadReminder(editingReminderId);
            reminder.setAccessToken(accessToken.getAccessToken());
            mOfyRepository.saveReminder(reminder);
          }
        });
  }

  public void notify(Reminder reminder, @Nonnull String message) {
    String access_token = reminder.getAccessToken();
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

  public void status(Reminder reminder, @Nullable final Callback<ResponseStatusData> callback) {
    String access_token = reminder.getAccessToken();
    mLineRepository.status(
        access_token,
        (apiRateLimit, responseStatusData) -> {
          if (responseStatusData != null) {
            LOG.info("responseStatusData.getStatus(): " + responseStatusData.getStatus());
            LOG.info("responseStatusData.getMessage(): " + responseStatusData.getMessage());
            LOG.info("responseStatusData.getTargetType(): " + responseStatusData.getTargetType());
            LOG.info("responseStatusData.getTarget(): " + responseStatusData.getTarget());
          }
          if (callback != null) {
            callback.response(apiRateLimit, responseStatusData);
          }
        });
  }

  public void revoke(@Nullable String accessToken) {
    if (accessToken == null || accessToken.isEmpty()) {
      return;
    }
    mLineRepository.revoke(
        accessToken,
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

  public void enqueueReminderTask(LineTalkRoomConfig config, Reminder reminder, long etaMillis) {
    String taskName =
        mPushQueueRepository.enqueueReminderTask(
            config.getSourceId(), etaMillis, reminder.getReminderMessage());
    LOG.info("enqueueReminderTask taskName: " + taskName);
    reminder.setReminderEnqueuedTaskName(taskName);
  }

  public void deleteReminderTask(@Nullable String taskName) {
    if (taskName == null || taskName.isEmpty()) {
      return;
    }
    mPushQueueRepository.deleteReminderTask(taskName);
  }

  /**
   * 応答メッセージを送る
   *
   * @param replyToken Webhook で受信する応答トークン
   * @param messages 送信するメッセージ (最大件数：5)
   */
  public void replyMessage(String replyToken, Message... messages) {
    mLineMessagingClient.replyMessage(new ReplyMessage(replyToken, Arrays.asList(messages)));
  }

  public LineBotCallbackRequestParser getLineBotCallbackRequestParser() {
    return new LineBotCallbackRequestParser(
        new LineSignatureValidator(mChannel.getSecret().getBytes()));
  }

  public interface Callback<T> {

    void response(@Nullable ResponseApiRateLimit apiRateLimit, @Nullable T res);
  }
}
