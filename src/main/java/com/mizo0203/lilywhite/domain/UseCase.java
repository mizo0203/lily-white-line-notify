package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.Template;
import com.mizo0203.lilywhite.repo.Repository;
import com.mizo0203.lilywhite.repo.State;
import com.mizo0203.lilywhite.repo.objectify.entity.Channel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UseCase implements AutoCloseable {
  public static final String ACTION_DATA_REQUEST_REMINDER_DATE_SET =
      "ACTION_DATA_REQUEST_REMINDER_DATE_SET";
  public static final String ACTION_DATA_REQUEST_REMINDER_CANCELLATION =
      "ACTION_DATA_REQUEST_REMINDER_CANCELLATION";
  public static final String ACTION_DATA_CANCEL_REMINDER = "ACTION_DATA_CANCEL_REMINDER";
  public static final String ACTION_DATA_NOT_CANCEL_REMINDER = "ACTION_DATA_NOT_CANCEL_REMINDER";
  public static final String ACTION_DATA_REQUEST_RESET = "ACTION_DATA_REQUEST_RESET";
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

  public void authorizeOauth(String state, AuthorizeOauthCallback callback) throws IOException {
    callback.onBuildRedirectUrlString(mRepository.buildAuthorizeOauthRedirectUrlString(state));
  }

  public void tokenOauth(String code, String sourceId) {
    mRepository.tokenOauth(code, sourceId);
  }

  /** リマインダーメッセージを送信する */
  public void notify(String source_id, @Nonnull String message) {
    mRepository.notify(source_id, message);
  }

  public void status(String source_id) {
    mRepository.status(source_id);
  }

  public void revoke(String source_id) {
    mRepository.revoke(source_id);
  }

  public void setReminderMessage(String sourceId, String reminderMessage) {
    mRepository.setReminderMessage(sourceId, reminderMessage);
  }

  public Channel loadChannel(long id) {
    return mRepository.loadChannel(id);
  }

  public Date parseDate(String date) {
    return mTranslator.parseDate(date);
  }

  public Date parseTime(String time) {
    return mTranslator.parseTime(time);
  }

  public Date parseDatetime(String datetime) {
    return mTranslator.parseDatetime(datetime);
  }

  public void initSource(String source_id) {
    mRepository.clearEvent(source_id);
  }

  private Message createTemplateMessageToReset(String text) {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。", createTemplateToReset(text));
  }

  private Template createTemplateToReset(String text) {
    return new ButtonsTemplate(null, null, text, createPostBackActionsToRequestReset());
  }

  private List<Action> createPostBackActionsToRequestReset() {
    return Collections.singletonList(new PostbackAction("リセット", ACTION_DATA_REQUEST_RESET));
  }

  public void replyMessageToRequestReminderMessage(String senderId, String replyToken) {
    mRepository.replyMessage(
        replyToken,
        new TextMessage("https://lily-white-line-notify.appspot.com/hello?state=" + senderId),
        new TextMessage("リマインダーをセットしますよー\nメッセージを入力してくださいー\n例) 春ですよー"));
  }

  public void replyMessageToRequestReminderDate(String replyToken) {
    mRepository.replyMessage(replyToken, createMessageToRequestReminderDate());
  }

  private Message createMessageToRequestReminderDate() {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。",
        createButtonsTemplateToRequestReminderDate());
  }

  private Template createButtonsTemplateToRequestReminderDate() {
    return ButtonsTemplate.builder()
        .text("リマインダー日時をセットしますよー")
        .actions(createDateTimePickerActionsToRequestReminderDate())
        .build();
  }

  private List<Action> createDateTimePickerActionsToRequestReminderDate() {
    return Collections.singletonList(createDateTimePickerActionToRequestReminderDate());
  }

  private Action createDateTimePickerActionToRequestReminderDate() {
    return new DatetimePickerAction(
        "セット", ACTION_DATA_REQUEST_REMINDER_DATE_SET, Define.Mode.DATE_TIME.toString());
  }

  public void enqueueReminderTask(String source_id, Date date) {
    mRepository.enqueueReminderTask(source_id, date.getTime());
  }

  public State getState(String source_id) {
    return mRepository.getState(source_id);
  }

  public void replyReminderConfirmMessage(String replyToken, Date date) {
    mRepository.replyMessage(
        replyToken,
        createMessageToConfirmReminder("リマインダーをセットしましたー\n" + mTranslator.formatDate(date)));
  }

  private Message createMessageToConfirmReminder(String text) {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。",
        createButtonsTemplateToConfirmReminder(text));
  }

  private Template createButtonsTemplateToConfirmReminder(String text) {
    return ButtonsTemplate.builder()
        .text(text)
        .actions(createPostbackActionsToRequestReminderCancellation())
        .build();
  }

  private List<Action> createPostbackActionsToRequestReminderCancellation() {
    return Collections.singletonList(createPostbackActionToRequestReminderCancellation());
  }

  private Action createPostbackActionToRequestReminderCancellation() {
    return new PostbackAction("キャンセル", ACTION_DATA_REQUEST_REMINDER_CANCELLATION);
  }

  public void replyReminderCancellationConfirmMessage(String replyToken) {
    mRepository.replyMessage(replyToken, createMessageToConfirmCancellationReminder());
  }

  private Message createMessageToConfirmCancellationReminder() {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。",
        createConfirmTemplateToConfirmCancellationReminder());
  }

  private Template createConfirmTemplateToConfirmCancellationReminder() {
    return new ConfirmTemplate(
        "本当にキャンセルしますかー？", createPostbackActionsToConfirmCancellationReminder());
  }

  private List<Action> createPostbackActionsToConfirmCancellationReminder() {
    return Arrays.asList(
        createPostbackActionToCancelReminder(), createPostbackActionToNotCancelReminder());
  }

  private Action createPostbackActionToCancelReminder() {
    return new PostbackAction("はい", ACTION_DATA_CANCEL_REMINDER);
  }

  private Action createPostbackActionToNotCancelReminder() {
    return new PostbackAction("いいえ", ACTION_DATA_NOT_CANCEL_REMINDER);
  }

  public void replyCanceledReminderMessage(String replyToken) {
    mRepository.replyMessage(replyToken, createTemplateMessageToReset("キャンセルしましたー"));
  }

  public void replyNotCanceledReminderMessage(String replyToken) {
    mRepository.replyMessage(replyToken, new TextMessage("そのままですー"));
  }

  public void setCancellationConfirm(String sourceId, boolean cancellationConfirm) {
    mRepository.setCancellationConfirm(sourceId, cancellationConfirm);
  }

  public interface AuthorizeOauthCallback {
    void onBuildRedirectUrlString(@Nullable String redirectUrlString) throws IOException;
  }
}
