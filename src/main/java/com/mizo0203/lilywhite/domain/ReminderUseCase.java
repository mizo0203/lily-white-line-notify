package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
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
import com.mizo0203.lilywhite.repo.line.data.ResponseStatusData;
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
  private static final String ACTION_DATA_REQUEST_ACCESS_TOKEN_COMPLETION =
      "ACTION_DATA_REQUEST_ACCESS_TOKEN_COMPLETION";
  private static final String ACTION_DATA_CANCEL_REMINDER = "ACTION_DATA_CANCEL_REMINDER";
  private static final String ACTION_DATA_NOT_CANCEL_REMINDER = "ACTION_DATA_NOT_CANCEL_REMINDER";
  private static final String ACTION_DATA_CREATE_REMINDER = "ACTION_DATA_CREATE_REMINDER";

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
  private boolean mDelete;

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

  private static ReminderState getReminderState(Reminder reminder) {
    if (reminder.getReminderMessage() == null) {
      return ReminderState.NO_REMINDER_MESSAGE;
    } else if (reminder.getAccessToken() == null) {
      return ReminderState.NO_ACCESS_TOKEN;
    } else if (!reminder.isReminderEnqueued()) {
      return ReminderState.HAS_ACCESS_TOKEN;
    } else if (!reminder.isCancellationConfirm()) {
      return ReminderState.REMINDER_ENQUEUED;
    } else {
      return ReminderState.REMINDER_CANCELLATION_CONFIRM;
    }
  }

  @Override
  public void close() {
    if (mDelete) {
      mOfyRepository.deleteReminder(mReminder.getId());
    } else {
      mOfyRepository.saveReminder(mReminder);
    }
  }

  /* package */ void onLineTextMessage(MessageEvent event, TextMessageContent message) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
        onResponseReminderMessage(event, message.getText().split("\n")[0]);
        break;
      case NO_ACCESS_TOKEN:
      case HAS_ACCESS_TOKEN:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onResponseReminderMessage(MessageEvent event, String reminderMessage) {
    mReminder.setReminderMessage(reminderMessage);
    mRepository.replyMessage(event.getReplyToken(), createMessageToRequestAccessToken());
  }

  private void onResponseCompleteAccessToken(final PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
        // NOP
        break;
      case NO_ACCESS_TOKEN:
        mRepository.replyMessage(event.getReplyToken(), createMessageToRequestAccessToken());
        break;
      case HAS_ACCESS_TOKEN:
        mRepository.status(mReminder, createStatusCallback(event));
        break;
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private Repository.Callback<ResponseStatusData> createStatusCallback(final PostbackEvent event) {
    return (apiRateLimit, res) -> {
      if (res != null && res.getTarget() != null) {
        mRepository.replyMessage(event.getReplyToken(), createMessageToRequestReminderDate());
      } else {
        mReminder.setAccessToken(null);
        mRepository.replyMessage(event.getReplyToken(), createMessageToRequestAccessToken());
      }
    };
  }

  private Message createMessageToRequestAccessToken() {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。", createButtonsTemplateToAccessToken());
  }

  private Template createButtonsTemplateToAccessToken() {
    return ButtonsTemplate.builder()
        .text(
            "1. 通知を送信するトークルームを選択してください\n"
                + "2. LINE の公式アカウント \"LINE Notify\" をトークルームに招待してください\n"
                + "3. 完了ボタンを押してくささい")
        .actions(createPostbackActionsToRequestAccessToken())
        .build();
  }

  private List<Action> createPostbackActionsToRequestAccessToken() {
    return Arrays.asList(
        new URIAction(
            "トークルームを選択",
            "https://lily-white-line-notify.appspot.com/hello?state=" + mReminder.getId()),
        new PostbackAction("完了", ACTION_DATA_REQUEST_ACCESS_TOKEN_COMPLETION));
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
    } else if (ACTION_DATA_REQUEST_ACCESS_TOKEN_COMPLETION.equals(data)) {
      onResponseCompleteAccessToken(event);
    } else if (ACTION_DATA_CANCEL_REMINDER.equals(data)) {
      onResponseCancelReminder(event);
    } else if (ACTION_DATA_NOT_CANCEL_REMINDER.equals(data)) {
      onResponseNotCancelReminder(event);
    } else if (ACTION_DATA_CREATE_REMINDER.equals(data)) {
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
      case NO_ACCESS_TOKEN:
        // NOP
        break;
      case HAS_ACCESS_TOKEN:
        enqueueReminderTask(date);
        replyReminderConfirmMessage(event.getReplyToken(), date);
        notifyReminderConfirmMessage(date);
        mConfig.setEditingReminderId(null);
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
      case NO_ACCESS_TOKEN:
      case HAS_ACCESS_TOKEN:
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
      case NO_ACCESS_TOKEN:
      case HAS_ACCESS_TOKEN:
      case REMINDER_ENQUEUED:
        // NOP
        break;
      case REMINDER_CANCELLATION_CONFIRM:
        deleteReminder();
        replyCanceledReminderMessage(event.getReplyToken());
        break;
      default:
        // NOP
        break;
    }
  }

  private void onResponseNotCancelReminder(PostbackEvent event) {
    switch (mReminderState) {
      case NO_REMINDER_MESSAGE:
      case NO_ACCESS_TOKEN:
      case HAS_ACCESS_TOKEN:
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
      case NO_ACCESS_TOKEN:
      case HAS_ACCESS_TOKEN:
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
    return new ButtonsTemplate(
        null,
        null,
        "キャンセルしましたー",
        Collections.singletonList(createPostbackActionToCreateReminder()));
  }

  private Action createPostbackActionToCreateReminder() {
    return new PostbackAction("新しいリマインダーを作成", ACTION_DATA_CREATE_REMINDER);
  }

  private void enqueueReminderTask(Date date) {
    mRepository.enqueueReminderTask(mConfig, mReminder, date.getTime());
  }

  private void replyReminderConfirmMessage(String replyToken, Date date) {
    mRepository.replyMessage(
        replyToken,
        createMessageToConfirmReminder("リマインダーをセットしましたー\n" + mTranslator.formatDate(date)));
  }

  private void notifyReminderConfirmMessage(Date date) {
    mRepository.notify(
        mReminder,
        mConfig.getNickname() + "さんが\n" + mTranslator.formatDate(date) + " に\n" + "リマインダーをセットしました");
  }

  private Message createMessageToConfirmReminder(String text) {
    return new TemplateMessage(
        "テンプレートメッセージはiOS版およびAndroid版のLINE 6.7.0以降で対応しています。",
        createButtonsTemplateToConfirmReminder(text));
  }

  private Template createButtonsTemplateToConfirmReminder(String text) {
    return ButtonsTemplate.builder()
        .text(text)
        .actions(
            Arrays.asList(
                createPostbackActionToRequestReminderCancellation(),
                createPostbackActionToCreateReminder()))
        .build();
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
        replyToken, new TextMessage("リマインダーをセットしますよー\nメッセージを入力してくださいー\n例) 春ですよー"));
  }

  /* package */ void deleteReminder() {
    deleteReminderTask();
    mRepository.notify(mReminder, mConfig.getNickname() + "さんがリマインダーをキャンセルしました");
    revokeAccessToken();
    mConfig.setEditingReminderId(null);
    mDelete = true;
  }

  private void revokeAccessToken() {
    String accessToken = mReminder.getAccessToken();
    mRepository.revoke(accessToken);
    mReminder.setAccessToken(null);
  }

  private void deleteReminderTask() {
    String taskName = mReminder.getReminderEnqueuedTaskName();
    mRepository.deleteReminderTask(taskName);
    mReminder.setReminderEnqueuedTaskName(null);
  }
}
