package com.mizo0203.lilywhite;

import com.mizo0203.lilywhite.domain.UseCase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LilyWhiteLineBotServlet extends HttpServlet {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LilyWhiteLineBotServlet.class.getName());

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    onLineWebhook(req, resp);
  }

  private void onLineWebhook(HttpServletRequest req, HttpServletResponse resp) {
    try (UseCase useCase = new UseCase()) {
      String date = req.getParameter("date");
      String time = req.getParameter("time");
      String text = req.getParameter("text");
      LOG.log(Level.INFO, "date: " + date);
      LOG.log(Level.INFO, "time: " + time);
      LOG.log(Level.INFO, "text: " + text);
      useCase.setReminderMessage("1", text);
      useCase.enqueueReminderTask("1", date, time);
    }
  }
}
