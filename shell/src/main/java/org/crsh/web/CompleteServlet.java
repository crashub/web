/*
 * Copyright (C) 2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.crsh.web;

import com.google.gson.Gson;
import org.crsh.cli.impl.Delimiter;
import org.crsh.cli.impl.completion.CompletionMatch;
import org.crsh.cli.spi.Completion;
import org.crsh.shell.Shell;
import org.crsh.util.Strings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/complete")
public class CompleteServlet extends HttpServlet {

  /** . */
  private static final Logger log = Logger.getLogger(CompleteServlet.class.getSimpleName());

  /** . */
  static final Gson gson = new Gson();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
    Session session = lf.getSession();
    Shell shell = session.getShell();
    String prefix = req.getParameter("prefix");
    if (prefix != null) {
      log.info(req.getRemoteHost() + " completing " + prefix);
      CompletionMatch completion = shell.complete(prefix);
      Completion completions = completion.getValue();
      List<String> values = new ArrayList<String>();
      if (completions.getSize() == 0) {
        // Do nothing
      }
      else {
        Delimiter delimiter = completion.getDelimiter();
        StringBuilder sb = new StringBuilder();
        if (completions.getSize() == 1) {
          String value = completions.getValues().iterator().next();
          delimiter.escape(value, sb);
          if ((Boolean)completions.get(value)) {
            sb.append(delimiter.getValue());
          }
          values.add(sb.toString());
        }
        else {
          String commonCompletion = Strings.findLongestCommonPrefix(completions.getValues());
          if (commonCompletion.length() > 0) {
            delimiter.escape(commonCompletion, sb);
            values.add(sb.toString());
          }
          else {
            String completionPrefix = completions.getPrefix();
            for (Map.Entry<String, Boolean> entry : completions) {
              sb.append(completionPrefix);
              delimiter.escape(entry.getKey(), sb);
              values.add(sb.toString());
              sb.setLength(0);
            }
          }
        }
      }
      resp.getWriter().write(gson.toJson(values));
    } else {
      log.info(req.getRemoteHost() + " no completion prefix for " + req.getRequestURL() + " " + req.getParameterMap());
      resp.getWriter().write("[]");
    }
  }
}
