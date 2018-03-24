package com.mizo0203.lilywhite.domain;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Translator {

  private final DateFormat mDateFormat;
  private final DateFormat mFormDateFormat;

  Translator() {
    mDateFormat = new SimpleDateFormat(Define.DATE_FORMAT_PATTERN);
    mDateFormat.setTimeZone(Define.LINE_TIME_ZONE);
    mFormDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    mFormDateFormat.setTimeZone(Define.LINE_TIME_ZONE);
  }

  public String formatDate(Date date) {
    return mDateFormat.format(date) + " " + Define.DATE_JST;
  }

  public Date parseFormDatetime(String date, String time) {
    try {
      return mFormDateFormat.parse(date + "T" + time);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Date parseDatetime(String datetime) {
    try {
      SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
      fmt.setTimeZone(Define.LINE_TIME_ZONE);
      return fmt.parse(datetime);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Date parseDate(String date) {
    // 未実装
    throw new UnsupportedOperationException();
  }

  public Date parseTime(String time) {
    // 未実装
    throw new UnsupportedOperationException();
  }
}
