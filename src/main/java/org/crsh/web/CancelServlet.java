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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/cancel")
public class CancelServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String id = req.getParameter("id");
    if (id != null) {
      Connection conn = ExecuteServlet.connections.get(id);
      if (conn != null) {
        ProcessContext context = conn.current;
        if (context != null) {
          // System.out.println("Cancelling " + id);
          context.cancel();
        } else {
          // System.out.println("No process context found for " + id + " likely already cancelled");
        }
      } else {
        // System.out.println("No connection found for " + id);
      }
    } else {
      // System.out.println("Cancel without an id");
    }
  }
}
