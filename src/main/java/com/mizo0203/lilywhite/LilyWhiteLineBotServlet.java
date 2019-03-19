package com.mizo0203.lilywhite;

import com.linecorp.bot.model.event.CallbackRequest;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.servlet.LineBotCallbackException;
import com.mizo0203.lilywhite.domain.EventUseCase;
import com.mizo0203.lilywhite.domain.UseCase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class LilyWhiteLineBotServlet extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LilyWhiteLineBotServlet.class.getName());

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
    try (UseCase useCase = new UseCase(1512704558L)) { // Channel ID - チャネルを区別するための識別子です。
      CallbackRequest callbackRequest = useCase.getLineBotCallbackRequestParser().handle(req);
      for (Event event : callbackRequest.getEvents()) {
        try (EventUseCase eventUseCase = useCase.createEventUseCase(event)) {
          eventUseCase.onEvent();
        }
      }
    } catch (LineBotCallbackException | IOException e) {
      e.printStackTrace();
    } finally {
      // ボットアプリのサーバーに webhook から送信される HTTP POST リクエストには、ステータスコード 200 を返す必要があります。
      // https://developers.line.me/ja/docs/messaging-api/reference/#anchor-99cdae5b4b38ad4b86a137b508fd7b1b861e2366
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }
}
