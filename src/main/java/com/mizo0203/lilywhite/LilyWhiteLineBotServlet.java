package com.mizo0203.lilywhite;

import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.servlet.LineBotCallbackException;
import com.linecorp.bot.servlet.LineBotCallbackRequestParser;
import com.mizo0203.lilywhite.domain.UseCase;
import com.mizo0203.lilywhite.repo.State;
import com.mizo0203.lilywhite.repo.objectify.entity.Channel;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

public class LilyWhiteLineBotServlet extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LilyWhiteLineBotServlet.class.getName());

  private UseCase mUseCase;

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    onLineWebhook(req, resp);
  }

  /**
   * LINE Platform からのリクエストを受信
   *
   * <p>友だち追加やメッセージの送信のようなイベントがトリガーされると、webhook URL に HTTPS POST リクエストが送信されます。Webhook URL
   * はチャネルに対してコンソールで設定します。
   *
   * <p>リクエストはボットアプリのサーバーで受信および処理されます。
   *
   * @param req an {@link HttpServletRequest} object that contains the request the client has made
   *     of the servlet
   * @param resp an {@link HttpServletResponse} object that contains the response the servlet sends
   *     to the client
   */
  private void onLineWebhook(HttpServletRequest req, HttpServletResponse resp) {
    mUseCase = new UseCase();
    try {
      Channel channel = mUseCase.loadChannel(1512704558L); // Channel ID - チャネルを区別するための識別子です。
      LineBotCallbackRequestParser parser =
          new LineBotCallbackRequestParser(
              new LineSignatureValidator(channel.getSecret().getBytes()));
      CallbackRequest callbackRequest = parser.handle(req);
      for (Event event : callbackRequest.getEvents()) {
        LOG.info("Sender Id: " + event.getSource().getSenderId());
        LOG.info("User Id:   " + event.getSource().getUserId());
        LOG.info("Timestamp: " + event.getTimestamp().toString());
        if (event instanceof AccountLinkEvent) {
          LOG.info("AccountLinkEvent");
        } else if (event instanceof BeaconEvent) {
          LOG.info("BeaconEvent");
        } else if (event instanceof FollowEvent) {
          LOG.info("FollowEvent");
        } else if (event instanceof JoinEvent) {
          LOG.info("JoinEvent");
          onLineJoin((JoinEvent) event);
        } else if (event instanceof LeaveEvent) {
          LOG.info("LeaveEvent");
        } else if (event instanceof MessageEvent) {
          LOG.info("MessageEvent");
          onLineMessage((MessageEvent) event);
        } else if (event instanceof PostbackEvent) {
          LOG.info("PostbackEvent");
          onLinePostBack((PostbackEvent) event);
        } else if (event instanceof ThingsEvent) {
          LOG.info("ThingsEvent");
        } else if (event instanceof UnfollowEvent) {
          LOG.info("UnfollowEvent");
        } else if (event instanceof UnknownEvent) {
          LOG.info("UnknownEvent");
        }
      }
    } catch (LineBotCallbackException | IOException e) {
      e.printStackTrace();
    } finally {
      // ボットアプリのサーバーに webhook から送信される HTTP POST リクエストには、ステータスコード 200 を返す必要があります。
      // https://developers.line.me/ja/docs/messaging-api/reference/#anchor-99cdae5b4b38ad4b86a137b508fd7b1b861e2366
      resp.setStatus(HttpServletResponse.SC_OK);
      mUseCase.close();
    }
  }

  private void onLineMessage(MessageEvent event) {
    if (event.getMessage() instanceof TextMessageContent) {
      onLineTextMessage(event, (TextMessageContent) event.getMessage());
    }
  }

  private void onLineJoin(JoinEvent event) {
    LOG.info("replyToken: " + event.getReplyToken());
    mUseCase.initSource(event.getSource().getSenderId());
    mUseCase.replyMessageToRequestReminderMessage(
        event.getSource().getSenderId(), event.getReplyToken());
  }

  private void onLineTextMessage(MessageEvent event, TextMessageContent message) {
    LOG.info("text: " + message.getText());
    if (message.getText() == null) {
      return;
    }
    State state = mUseCase.getState(event.getSource().getSenderId());
    switch (state) {
      case NO_REMINDER_MESSAGE:
        {
          String reminderMessage = message.getText().split("\n")[0];
          mUseCase.setReminderMessage(event.getSource().getSenderId(), reminderMessage);
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
    State state = mUseCase.getState(event.getSource().getSenderId());
    switch (state) {
      case NO_REMINDER_MESSAGE:
        {
          if (UseCase.ACTION_DATA_REQUEST_RESET.equals(event.getPostbackContent().getData())) {
            mUseCase.replyMessageToRequestReminderMessage(
                event.getSource().getSenderId(), event.getReplyToken());
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
            mUseCase.setCancellationConfirm(event.getSource().getSenderId(), true);
            mUseCase.replyReminderCancellationConfirmMessage(event.getReplyToken());
          }
          break;
        }
      case REMINDER_CANCELLATION_CONFIRM:
        {
          if (UseCase.ACTION_DATA_CANCEL_REMINDER.equals(event.getPostbackContent().getData())) {
            mUseCase.replyCanceledReminderMessage(event.getReplyToken());
            mUseCase.initSource(event.getSource().getSenderId());
          } else if (UseCase.ACTION_DATA_NOT_CANCEL_REMINDER.equals(
              event.getPostbackContent().getData())) {
            mUseCase.setCancellationConfirm(event.getSource().getSenderId(), false);
            mUseCase.replyNotCanceledReminderMessage(event.getReplyToken());
          }
          break;
        }
      default:
        break;
    }
  }

  private void onLinePostBackDateParam(PostbackEvent event, Date date) {
    State state = mUseCase.getState(event.getSource().getSenderId());
    switch (state) {
      case NO_REMINDER_MESSAGE:
        {
          // NOP
          break;
        }
      case HAS_REMINDER_MESSAGE:
        {
          if (UseCase.ACTION_DATA_REQUEST_REMINDER_DATE_SET.equals(
              event.getPostbackContent().getData())) {
            mUseCase.enqueueReminderTask(event.getSource().getSenderId(), date);
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
