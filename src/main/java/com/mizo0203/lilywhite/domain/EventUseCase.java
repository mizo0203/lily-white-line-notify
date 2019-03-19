package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.Source;
import com.mizo0203.lilywhite.repo.OfyRepository;
import com.mizo0203.lilywhite.repo.State;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

public class EventUseCase implements AutoCloseable {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(EventUseCase.class.getName());

  private final UseCase mUseCase;
  private final Event mEvent;
  private final Source mSource;
  private final Instant mTimestamp;
  private final OfyRepository mOfyRepository;
  private final LineTalkRoomConfig mConfig;
  private final State mState;

  /* package */ EventUseCase(UseCase useCase, Event event) {
    mUseCase = useCase;
    mEvent = event;
    mSource = mEvent.getSource();
    mTimestamp = mEvent.getTimestamp();
    mOfyRepository = new OfyRepository();
    mConfig = mOfyRepository.loadLineTalkRoomConfig(mEvent.getSource().getSenderId());
    mState = getState(mConfig);
  }

  private static State getState(LineTalkRoomConfig config) {
    if (config.getNickname() == null) {
      return State.NO_NICKNAME;
    } else if (config.getReminderMessage() == null) {
      return State.NO_REMINDER_MESSAGE;
    } else if (!config.isReminderEnqueued()) {
      return State.HAS_REMINDER_MESSAGE;
    } else if (!config.isCancellationConfirm()) {
      return State.REMINDER_ENQUEUED;
    } else {
      return State.REMINDER_CANCELLATION_CONFIRM;
    }
  }

  @Override
  public void close() {
    mOfyRepository.saveLineTalkRoomConfig(mConfig);
  }

  public void onEvent() {
    LOG.info("Sender Id: " + mSource.getSenderId());
    LOG.info("User Id:   " + mSource.getUserId());
    LOG.info("Timestamp: " + mTimestamp.toString());
    LOG.info("Event:     " + mEvent.getClass().getSimpleName());
    if (mEvent instanceof FollowEvent) {
      LOG.info("FollowEvent");
      onLineFollow((FollowEvent) mEvent);
    } else if (mEvent instanceof MessageEvent) {
      LOG.info("MessageEvent");
      onLineMessage((MessageEvent) mEvent);
    } else if (mEvent instanceof PostbackEvent) {
      LOG.info("PostbackEvent");
      onLinePostBack((PostbackEvent) mEvent);
    }
  }

  private void onLineFollow(FollowEvent event) {
    LOG.info("replyToken: " + event.getReplyToken());
    mUseCase.initSource(mSource.getSenderId());
    mUseCase.replyMessageToRequestNickname(event.getReplyToken());
  }

  private void onLineMessage(MessageEvent event) {
    if (event.getMessage() instanceof TextMessageContent) {
      onLineTextMessage(event, (TextMessageContent) event.getMessage());
    }
  }

  private void onLineTextMessage(MessageEvent event, TextMessageContent message) {
    LOG.info("text: " + message.getText());
    if (message.getText() == null) {
      return;
    }
    switch (mState) {
      case NO_NICKNAME:
        onResponseNickname(event, message.getText().split("\n")[0]);
        break;
      case NO_REMINDER_MESSAGE:
        String reminderMessage = message.getText().split("\n")[0];
        mUseCase.setReminderMessage(mSource.getSenderId(), reminderMessage);
        mUseCase.replyMessageToRequestReminderDate(event.getReplyToken());
        break;
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onLinePostBack(PostbackEvent event) {
    @Nullable Map<String, String> params = event.getPostbackContent().getParams();
    if (params == null || params.isEmpty()) {
      onLinePostBackNoParam(event);
    } else if (params.containsKey("date")) {
      Date date = mUseCase.parseDate(params.get("date"));
      onLinePostBackDateParam(event, date);
    } else if (params.containsKey("time")) {
      Date date = mUseCase.parseTime(params.get("time"));
      onLinePostBackDateParam(event, date);
    } else if (params.containsKey("datetime")) {
      Date date = mUseCase.parseDatetime(params.get("datetime"));
      onLinePostBackDateParam(event, date);
    }
  }

  private void onLinePostBackNoParam(PostbackEvent event) {
    String data = event.getPostbackContent().getData();
    if (UseCase.ACTION_DATA_REQUEST_REMINDER_CANCELLATION.equals(data)) {
      onresponseReminderCancellation(event);
    } else if (UseCase.ACTION_DATA_CANCEL_REMINDER.equals(data)) {
      onresponseCancelReminder(event);
    } else if (UseCase.ACTION_DATA_NOT_CANCEL_REMINDER.equals(data)) {
      onresponseNotCancelReminder(event);
    } else if (UseCase.ACTION_DATA_REQUEST_RESET.equals(data)) {
      onResponseReset(event);
    }
  }

  private void onLinePostBackDateParam(PostbackEvent event, Date date) {
    String data = event.getPostbackContent().getData();
    if (UseCase.ACTION_DATA_REQUEST_REMINDER_DATE_SET.equals(data)) {
      onresponseReminderDate(event, date);
    }
  }

  private void onResponseNickname(MessageEvent event, String nickname) {
    mConfig.setNickname(nickname);
    mUseCase.replyMessageToRequestReminderMessage(mSource.getSenderId(), event.getReplyToken());
  }

  private void onresponseReminderDate(PostbackEvent event, Date date) {
    switch (mState) {
      case NO_NICKNAME:
      case NO_REMINDER_MESSAGE:
        // NOP
        break;
      case HAS_REMINDER_MESSAGE:
        mUseCase.enqueueReminderTask(mSource.getSenderId(), date);
        mUseCase.replyReminderConfirmMessage(event.getReplyToken(), date);
        break;
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onresponseReminderCancellation(PostbackEvent event) {
    switch (mState) {
      case NO_NICKNAME:
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
        // NOP
        break;
      case REMINDER_ENQUEUED:
        mUseCase.setCancellationConfirm(mSource.getSenderId(), true);
        mUseCase.replyReminderCancellationConfirmMessage(event.getReplyToken());
        break;
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }

  private void onresponseCancelReminder(PostbackEvent event) {
    switch (mState) {
      case NO_NICKNAME:
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
        // NOP
        break;
      case REMINDER_CANCELLATION_CONFIRM:
        mUseCase.replyCanceledReminderMessage(event.getReplyToken());
        mUseCase.initSource(mSource.getSenderId());
        break;
      default:
        // NOP
        break;
    }
  }

  private void onresponseNotCancelReminder(PostbackEvent event) {
    switch (mState) {
      case NO_NICKNAME:
      case NO_REMINDER_MESSAGE:
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
        // NOP
        break;
      case REMINDER_CANCELLATION_CONFIRM:
        mUseCase.setCancellationConfirm(mSource.getSenderId(), false);
        mUseCase.replyNotCanceledReminderMessage(event.getReplyToken());
        break;
      default:
        // NOP
        break;
    }
  }

  private void onResponseReset(PostbackEvent event) {
    switch (mState) {
      case NO_NICKNAME:
        // NOP
        break;
      case NO_REMINDER_MESSAGE:
        mUseCase.replyMessageToRequestReminderMessage(mSource.getSenderId(), event.getReplyToken());
        break;
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
      default:
        // NOP
        break;
    }
  }
}
