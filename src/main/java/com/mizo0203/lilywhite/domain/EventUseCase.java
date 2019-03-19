package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.event.*;
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
    if (mEvent instanceof AccountLinkEvent) {
      LOG.info("AccountLinkEvent");
    } else if (mEvent instanceof BeaconEvent) {
      LOG.info("BeaconEvent");
    } else if (mEvent instanceof FollowEvent) {
      LOG.info("FollowEvent");
      onLineFollow((FollowEvent) mEvent);
    } else if (mEvent instanceof JoinEvent) {
      LOG.info("JoinEvent");
    } else if (mEvent instanceof LeaveEvent) {
      LOG.info("LeaveEvent");
    } else if (mEvent instanceof MessageEvent) {
      LOG.info("MessageEvent");
      onLineMessage((MessageEvent) mEvent);
    } else if (mEvent instanceof PostbackEvent) {
      LOG.info("PostbackEvent");
      onLinePostBack((PostbackEvent) mEvent);
    } else if (mEvent instanceof ThingsEvent) {
      LOG.info("ThingsEvent");
    } else if (mEvent instanceof UnfollowEvent) {
      LOG.info("UnfollowEvent");
    } else if (mEvent instanceof UnknownEvent) {
      LOG.info("UnknownEvent");
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
        {
          String nickname = message.getText().split("\n")[0];
          mConfig.setNickname(nickname);
          mUseCase.replyMessageToRequestReminderMessage(
              mSource.getSenderId(), event.getReplyToken());
          break;
        }
      case NO_REMINDER_MESSAGE:
        {
          String reminderMessage = message.getText().split("\n")[0];
          mUseCase.setReminderMessage(mSource.getSenderId(), reminderMessage);
          mUseCase.replyMessageToRequestReminderDate(event.getReplyToken());
          break;
        }
      case HAS_REMINDER_MESSAGE:
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
        {
          // NOP
          break;
        }
      default:
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
    switch (mState) {
      case NO_NICKNAME:
        {
          // NOP
          break;
        }
      case NO_REMINDER_MESSAGE:
        {
          if (UseCase.ACTION_DATA_REQUEST_RESET.equals(event.getPostbackContent().getData())) {
            mUseCase.replyMessageToRequestReminderMessage(
                mSource.getSenderId(), event.getReplyToken());
          }
          break;
        }
      case HAS_REMINDER_MESSAGE:
        {
          // NOP
          break;
        }
      case REMINDER_ENQUEUED:
        {
          if (UseCase.ACTION_DATA_REQUEST_REMINDER_CANCELLATION.equals(
              event.getPostbackContent().getData())) {
            mUseCase.setCancellationConfirm(mSource.getSenderId(), true);
            mUseCase.replyReminderCancellationConfirmMessage(event.getReplyToken());
          }
          break;
        }
      case REMINDER_CANCELLATION_CONFIRM:
        {
          if (UseCase.ACTION_DATA_CANCEL_REMINDER.equals(event.getPostbackContent().getData())) {
            mUseCase.replyCanceledReminderMessage(event.getReplyToken());
            mUseCase.initSource(mSource.getSenderId());
          } else if (UseCase.ACTION_DATA_NOT_CANCEL_REMINDER.equals(
              event.getPostbackContent().getData())) {
            mUseCase.setCancellationConfirm(mSource.getSenderId(), false);
            mUseCase.replyNotCanceledReminderMessage(event.getReplyToken());
          }
          break;
        }
      default:
        break;
    }
  }

  private void onLinePostBackDateParam(PostbackEvent event, Date date) {
    switch (mState) {
      case NO_NICKNAME:
      case NO_REMINDER_MESSAGE:
        {
          // NOP
          break;
        }
      case HAS_REMINDER_MESSAGE:
        {
          if (UseCase.ACTION_DATA_REQUEST_REMINDER_DATE_SET.equals(
              event.getPostbackContent().getData())) {
            mUseCase.enqueueReminderTask(mSource.getSenderId(), date);
            mUseCase.replyReminderConfirmMessage(event.getReplyToken(), date);
          }
          break;
        }
      case REMINDER_ENQUEUED:
      case REMINDER_CANCELLATION_CONFIRM:
        {
          // NOP
          break;
        }
      default:
        break;
    }
  }
}
