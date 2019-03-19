package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.Template;
import com.mizo0203.lilywhite.repo.OfyRepository;
import com.mizo0203.lilywhite.repo.ReminderState;
import com.mizo0203.lilywhite.repo.Repository;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;
import com.mizo0203.lilywhite.repo.objectify.entity.Reminder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ReminderUseCase implements AutoCloseable {

  private static final String ACTION_DATA_REQUEST_REMINDER_CANCELLATION =
      "ACTION_DATA_REQUEST_REMINDER_CANCELLATION";
  private static final String ACTION_DATA_CANCEL_REMINDER = "ACTION_DATA_CANCEL_REMINDER";
  private static final String ACTION_DATA_NOT_CANCEL_REMINDER = "ACTION_DATA_NOT_CANCEL_REMINDER";
  private static final String ACTION_DATA_REQUEST_RESET = "ACTION_DATA_REQUEST_RESET";

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(ReminderUseCase.class.getName());

  private static final String ACTION_DATA_REQUEST_REMINDER_DATE_SET =
      "ACTION_DATA_REQUEST_REMINDER_DATE_SET";
  private final Repository mRepository;
  private final OfyRepository mOfyRepository;
  private final LineTalkRoomConfig mConfig;
  private final Reminder mReminder;
  private final ReminderState mReminderState;
  private final Translator mTranslator;

  /* package */ ReminderUseCase(Repository repository, LineTalkRoomConfig config) {
    mRepository = repository;
    mOfyRepository = new OfyRepository();
    mConfig = config;
    if (config.getEditingReminderId() == null) {
      mReminder = mOfyRepository.factoryReminder();
      config.setEditingReminderId(mReminder.getId());
    } else {
      mReminder = mOfyRepository.loadReminder(config.getEditingReminderId());
    }
    mReminderState = getReminderState(mReminder);
    mTranslator = new Translator();
  }

  private static ReminderState getReminderState(Reminder config) {
    if (config.getReminderMessage() == null) {
      return ReminderState.NO_REMINDER_MESSAGE;
    } else if (!config.isReminderEnqueued()) {
      return ReminderState.HAS_REMINDER_MESSAGE;
    } else if (!config.isCancellationConfirm()) {
      return ReminderState.REMINDER_ENQUEUED;
    } else {
      return ReminderState.REMINDER_CANCELLATION_CONFIRM;
    }
  }

  @Override
  public void close() {
    mOfyRepository.saveReminder(mReminder);
  }

  /* package */ void onLineTextMessage(MessageEvent event, TextMessageContent message) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
        onResponseReminderMessage(event, message.getText().split("\n")[0]);
        break;
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onResponseReminderMessage(MessageEvent event, String reminderMessage) {
    mReminder.setReminderMessage(reminderMessage);
    mRepository.replyMessage(event.getReplyToken(), createMessageToRequestReminderDate());
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

  /* package */ void onLinePostBackNoParam(PostbackEvent event) {

    String data = event.getPostbackContent().getData();
    if (ACTION_DATA_REQUEST_REMINDER_CANCELLATION.equals(data)) {
      onResponseReminderCancellation(event);
    } else if (ACTION_DATA_CANCEL_REMINDER.equals(data)) {
      onResponseCancelReminder(event);
    } else if (ACTION_DATA_NOT_CANCEL_REMINDER.equals(data)) {
      onResponseNotCancelReminder(event);
    } else if (ACTION_DATA_REQUEST_RESET.equals(data)) {
      onResponseReset(event);
    }
  }

  /* package */ void onLinePostBackDateParam(PostbackEvent event, Date date) {
    String data = event.getPostbackContent().getData();
    if (ACTION_DATA_REQUEST_REMINDER_DATE_SET.equals(data)) {
      onResponseReminderDate(event, date);
    }
  }

  private void onResponseReminderDate(PostbackEvent event, Date date) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
        // NOP
        break;
      case HAS_REMINDER_MESSAGE:
        enqueueReminderTask(date);
        replyReminderConfirmMessage(event.getReplyToken(), date);
        break;
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onResponseReminderCancellation(PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
        // NOP
        break;
      case REMINDER_ENQUEUED:
        setCancellationConfirm(true);
        replyReminderCancellationConfirmMessage(event.getReplyToken());
        break;
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onResponseCancelReminder(PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
        // NOP
        break;
      case REMINDER_CANCELLATION_CONFIRM:
        replyCanceledReminderMessage(event.getReplyToken());
        mRepository.clearEvent(mConfig, mReminder);
        break;
      default:
        // NOP
        break;
    }
  }

  private void onResponseNotCancelReminder(PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
        // NOP
        break;
      case REMINDER_CANCELLATION_CONFIRM:
        setCancellationConfirm(false);
        replyNotCanceledReminderMessage(event.getReplyToken());
        break;
      default:
        // NOP
        break;
    }
  }

  private void onResponseReset(PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
        replyMessageToRequestReminderMessage(mConfig.getSourceId(), event.getReplyToken());
        break;
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private Message createTemplateMessageToReset() {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。", createTemplateToReset());
  }

  private Template createTemplateToReset() {
    return new ButtonsTemplate(null, null, "キャンセルしましたー", createPostBackActionsToRequestReset());
  }

  private List<Action> createPostBackActionsToRequestReset() {
    return Collections.singletonList(new PostbackAction("リセット", ACTION_DATA_REQUEST_RESET));
  }

  private void enqueueReminderTask(Date date) {
    mRepository.enqueueReminderTask(mConfig, mReminder, date.getTime());
  }

  private void replyReminderConfirmMessage(String replyToken, Date date) {
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

  private void replyReminderCancellationConfirmMessage(String replyToken) {
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

  private void replyCanceledReminderMessage(String replyToken) {
    mRepository.replyMessage(replyToken, createTemplateMessageToReset());
  }

  private void replyNotCanceledReminderMessage(String replyToken) {
    mRepository.replyMessage(replyToken, new TextMessage("そのままですー"));
  }

  private void setCancellationConfirm(boolean cancellationConfirm) {
    mReminder.setCancellationConfirm(cancellationConfirm);
  }

  private void replyMessageToRequestReminderMessage(String senderId, String replyToken) {
    mRepository.replyMessage(
        replyToken,
        new TextMessage("https://lily-white-line-notify.appspot.com/hello?state=" + senderId),
        new TextMessage("リマインダーをセットしますよー\nメッセージを入力してくださいー\n例) 春ですよー"));
  }
}
