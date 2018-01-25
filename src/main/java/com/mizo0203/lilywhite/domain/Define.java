package com.mizo0203.lilywhite.domain;

import java.util.TimeZone;

public class Define {
  /** LINE アプリから日時指定操作をする場合は日本標準時(JST)で指定すること */
  public static final TimeZone LINE_TIME_ZONE = TimeZone.getTimeZone("Asia/Tokyo");

  public static final String DATE_FORMAT_PATTERN = "MM/dd(E) HH:mm -";

  public static final String DATE_JST = "(JST)";
  public static final String REDIRECT_URI_STR =
      "https://lily-white-line-notify.appspot.com/redirect";
}
