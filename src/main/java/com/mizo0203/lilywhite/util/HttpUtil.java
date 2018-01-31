package com.mizo0203.lilywhite.util;

import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class HttpUtil {

  private static final Logger LOG = Logger.getLogger(HttpUtil.class.getName());

  public static void post(
      URL url, Map<String, String> reqProp, @Nonnull String body, Callback callback) {
    LOG.info("post url:     " + url);
    LOG.info("post reqProp: " + reqProp);
    LOG.info("post body:    " + body);
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      for (String key : reqProp.keySet()) {
        connection.setRequestProperty(key, reqProp.get(key));
      }
      connection.setRequestProperty(
          "Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
      connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
      connection.getOutputStream().flush();
      LOG.info("getResponseCode():    " + connection.getResponseCode());
      LOG.info("getResponseMessage(): " + connection.getResponseMessage());
      if (connection.getErrorStream() != null) {
        LOG.severe("getErrorStream(): " + PaserUtil.parseString(connection.getErrorStream()));
      }
      if (callback != null) {
        callback.response(connection);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  public static void post(
      URL url, Map<String, String> reqProp, Map<String, String> params, Callback callback) {
    post(url, reqProp, createBody(params), callback);
  }

  public static void get(URL url, Map<String, String> reqProp, Callback callback) {
    LOG.info("get url:     " + url);
    LOG.info("get reqProp: " + reqProp);
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      for (String key : reqProp.keySet()) {
        connection.setRequestProperty(key, reqProp.get(key));
      }
      LOG.info("getResponseCode():    " + connection.getResponseCode());
      LOG.info("getResponseMessage(): " + connection.getResponseMessage());
      if (connection.getErrorStream() != null) {
        LOG.severe("getErrorStream(): " + PaserUtil.parseString(connection.getErrorStream()));
      }
      if (callback != null) {
        callback.response(connection);
        LOG.info("X-RateLimit-Limit:          " + connection.getHeaderField("X-RateLimit-Limit"));
        LOG.info(
            "X-RateLimit-Remaining:      " + connection.getHeaderField("X-RateLimit-Remaining"));
        LOG.info(
            "X-RateLimit-ImageLimit:     " + connection.getHeaderField("X-RateLimit-ImageLimit"));
        LOG.info(
            "X-RateLimit-ImageRemaining: "
                + connection.getHeaderField("X-RateLimit-ImageRemaining"));
        LOG.info("X-RateLimit-Reset:          " + connection.getHeaderField("X-RateLimit-Reset"));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @Nonnull
  private static String createBody(Map<String, String> params) {
    try {
      URIBuilder builder = new URIBuilder();
      for (String name : params.keySet()) {
        builder.addParameter(name, params.get(name));
      }
      LOG.info("getQuery():\t" + builder.build().getQuery());
      LOG.info("getRawQuery():\t" + builder.build().getRawQuery());
      LOG.info("toString():\t" + builder.build().toString());
      String query = builder.build().getQuery();
      return query != null ? query : "";
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return "";
    }
  }

  public interface Callback {

    void response(HttpURLConnection connection);
  }
}
