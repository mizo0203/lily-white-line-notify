/*
 * Copyright 2015 Google Inc. <p> Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at <p> http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package com.mizo0203.lilywhite;

import com.mizo0203.lilywhite.domain.UseCase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class RedirectServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(RedirectServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter out = resp.getWriter();
    out.println("Hello, LINE Notify World");
    LOG.info("code:\t" + req.getParameter("code"));
    LOG.info("stat:\t" + req.getParameter("state"));
    LOG.info("error:\t" + req.getParameter("error"));
    LOG.info("error_description:\t" + req.getParameter("error_description"));
    try (UseCase useCase = new UseCase(1512704558L)) {
      long editingReminderId = Long.parseLong(req.getParameter("state"));
      useCase.tokenOauth(req.getParameter("code"), editingReminderId);
    }
  }
}
