/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mizo0203.lilywhite.push_task;

import com.mizo0203.lilywhite.domain.UseCase;
import com.mizo0203.lilywhite.repo.OfyRepository;
import com.mizo0203.lilywhite.repo.objectify.entity.Reminder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class ReminderTaskServlet extends HttpServlet {
  public static final String PARAM_NAME_REMINDER_ID = "param_name_reminder_id";
  public static final String PARAM_NAME_MESSAGE = "param_name_message";

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(ReminderTaskServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try (UseCase useCase = new UseCase(1512704558L)) {
      String message = req.getParameter(PARAM_NAME_MESSAGE);
      OfyRepository ofyRepository = new OfyRepository();
      Reminder reminder =
          ofyRepository.loadReminder(Long.parseLong(req.getParameter(PARAM_NAME_REMINDER_ID)));
      useCase.notify(reminder, message);
      useCase.revoke(reminder.getAccessToken());
      ofyRepository.deleteReminder(reminder.getId());
    }
  }
}
