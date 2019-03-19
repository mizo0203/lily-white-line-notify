package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.servlet.LineBotCallbackRequestParser;
import com.mizo0203.lilywhite.repo.Repository;
import com.mizo0203.lilywhite.repo.objectify.entity.Reminder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;

public class UseCase implements AutoCloseable {
  private final Repository mRepository;
  private final Translator mTranslator;

  public UseCase(long channelId) {
    mRepository = new Repository(channelId);
    mTranslator = new Translator();
  }

  @Override
  public void close() {
    mRepository.destroy();
  }

  public void authorizeOauth(String state, AuthorizeOauthCallback callback) throws IOException {
    callback.onBuildRedirectUrlString(mRepository.buildAuthorizeOauthRedirectUrlString(state));
  }

  public void tokenOauth(String code, long editingReminderId) {
    mRepository.tokenOauth(code, editingReminderId);
  }

  /** リマインダーメッセージを送信する */
  public void notify(Reminder reminder, @Nonnull String message) {
    mRepository.notify(reminder, message);
  }

  public void status(Reminder reminder) {
    mRepository.status(reminder, null);
  }

  @SuppressWarnings("unused")
  public void revoke(Reminder reminder) {
    mRepository.revoke(reminder);
  }

  /* package */ Date parseDate(String date) {
    return mTranslator.parseDate(date);
  }

  /* package */ Date parseTime(String time) {
    return mTranslator.parseTime(time);
  }

  /* package */ Date parseDatetime(String datetime) {
    return mTranslator.parseDatetime(datetime);
  }

  public EventUseCase createEventUseCase(Event event) {
    return new EventUseCase(this, mRepository, event);
  }

  public LineBotCallbackRequestParser getLineBotCallbackRequestParser() {
    return mRepository.getLineBotCallbackRequestParser();
  }

  public interface AuthorizeOauthCallback {
    void onBuildRedirectUrlString(@Nullable String redirectUrlString) throws IOException;
  }
}
