package com.mizo0203.lilywhite.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.model.ReplyMessage;
import com.mizo0203.lilywhite.util.HttpUtil;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/* package */ class LineMessagingService {

  private static final String MESSAGING_API_REPLY_MESSAGE_URL_STR =
      "https://api.line.me/v2/bot/message/reply";

  @SuppressWarnings("EmptyMethod")
  /* package */ void destroy() {
    // NOP
  }

  /**
   * 応答メッセージを送る
   *
   * <p>ユーザー、グループ、またはトークルームからのイベントに対して応答メッセージを送信するAPIです。
   *
   * <p>イベントが発生するとwebhookを使って通知されます。応答できるイベントには応答トークンが発行されます。
   *
   * <p>応答トークンは一定の期間が経過すると無効になるため、メッセージを受信したらすぐに応答を返す必要があります。応答トークンは1回のみ使用できます。
   *
   * <p>https://developers.line.me/ja/docs/messaging-api/reference/#anchor-36ddabf319927434df30f0a74e21ad2cc69f0013
   *
   * @param channelAccessToken channel access token
   * @param replyMessage Respond to events from users, groups, and rooms.
   */
  /* package */ void replyMessage(String channelAccessToken, ReplyMessage replyMessage) {
    try {
      String body = new ObjectMapper().writeValueAsString(replyMessage);
      URL url = new URL(MESSAGING_API_REPLY_MESSAGE_URL_STR);
      Map<String, String> reqProp = new HashMap<>();
      reqProp.put("Content-Type", "application/json");
      reqProp.put("Authorization", "Bearer " + channelAccessToken);
      HttpUtil.post(url, reqProp, body, responseCode -> {});
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
