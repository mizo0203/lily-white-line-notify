package com.mizo0203.lilywhite.domain;

import com.mizo0203.lilywhite.repo.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class UseCase implements AutoCloseable {
  private final Repository mRepository;
  private final Translator mTranslator;

  public UseCase() {
    mRepository = new Repository();
    mTranslator = new Translator();
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

  public void notify(@Nonnull String message) {
    mRepository.notify(message);
  }

  public void status() {
    mRepository.status();
  }

  public void revoke() {
    mRepository.revoke();
  }

  public void setReminderMessage(String sourceId, String reminderMessage) {
    mRepository.setReminderMessage(sourceId, reminderMessage);
  }

  public void enqueueReminderTask(String source_id, String date, String time) {
    mRepository.enqueueReminderTask(source_id, mTranslator.parseFormDatetime(date, time).getTime());
  }

  public interface AuthorizeOauthCallback {
    void onBuildRedirectUrlString(@Nullable String redirectUrlString) throws IOException;
  }
}
