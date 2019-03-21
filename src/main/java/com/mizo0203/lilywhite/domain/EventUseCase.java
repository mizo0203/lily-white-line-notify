package com.mizo0203.lilywhite.domain;

import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.TextMessage;
import com.mizo0203.lilywhite.repo.OfyRepository;
import com.mizo0203.lilywhite.repo.Repository;
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
  private final Repository mRepository;
  private boolean mDelete = false;

  /* package */ EventUseCase(UseCase useCase, Repository repository, Event event) {
    mUseCase = useCase;
    mRepository = repository;
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
    } else {
      return State.HAS_NICKNAME;
    }
  }

  @Override
  public void close() {
    if (mDelete) {
      mOfyRepository.deleteLineTalkRoomConfig(mConfig.getSourceId());
    } else {
      mOfyRepository.saveLineTalkRoomConfig(mConfig);
    }
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
    } else if (mEvent instanceof UnfollowEvent) {
      LOG.info("PostbackEvent");
      onLineUnfollow();
    }
  }

  private void onLineFollow(FollowEvent event) {
    LOG.info("replyToken: " + event.getReplyToken());
    mRepository.clearEvent(mConfig, null);
    mRepository.replyMessage(event.getReplyToken(), new TextMessage("あなたのニックネームを入力してください\n例) みぞ"));
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
      case HAS_NICKNAME:
        try (ReminderUseCase reminderUseCase = new ReminderUseCase(mRepository, mConfig)) {
          reminderUseCase.onLineTextMessage(event, message);
        }
        break;
      default:
        // NOP
        break;
    }
  }

  private void onLinePostBack(PostbackEvent event) {
    if (State.NO_NICKNAME.equals(mState)) {
      return;
    }
    try (ReminderUseCase reminderUseCase = new ReminderUseCase(mRepository, mConfig)) {
      @Nullable Map<String, String> params = event.getPostbackContent().getParams();
      if (params == null || params.isEmpty()) {
        reminderUseCase.onLinePostBackNoParam(event);
      } else if (params.containsKey("date")) {
        Date date = mUseCase.parseDate(params.get("date"));
        reminderUseCase.onLinePostBackDateParam(event, date);
      } else if (params.containsKey("time")) {
        Date date = mUseCase.parseTime(params.get("time"));
        reminderUseCase.onLinePostBackDateParam(event, date);
      } else if (params.containsKey("datetime")) {
        Date date = mUseCase.parseDatetime(params.get("datetime"));
        reminderUseCase.onLinePostBackDateParam(event, date);
      }
    }
  }

  private void onLineUnfollow() {
    if (mConfig.getEditingReminderId() != null) {
      try (ReminderUseCase reminderUseCase = new ReminderUseCase(mRepository, mConfig)) {
        reminderUseCase.onLineUnfollow();
      }
    }
    mDelete = true;
  }

  private void onResponseNickname(MessageEvent event, String nickname) {
    mConfig.setNickname(nickname);
    replyMessageToRequestReminderMessage(event.getReplyToken());
  }

  private void replyMessageToRequestReminderMessage(String replyToken) {
    mRepository.replyMessage(
        replyToken, new TextMessage("リマインダーをセットしますよー\nメッセージを入力してくださいー\n例) 春ですよー"));
  }
}
