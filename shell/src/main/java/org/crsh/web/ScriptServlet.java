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
import com.google.gson.JsonObject;
import org.codehaus.groovy.control.CompilationFailedException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/script")
public class ScriptServlet extends HttpServlet {

  /** . */
  static final Gson gson = new Gson();

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String name = req.getParameter("name");
    if (name == null) {
      super.doDelete(req, resp);
    } else {
      // Save the command
      Session session = LifeCycle.getSession();
      session.removeScript(name);
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String script = req.getParameter("script");
    if (script == null) {
      JsonObject payload = new JsonObject();
      payload.addProperty("message", "No script provided");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json");
      gson.toJson(payload, resp.getWriter());
    } else {
      Session session = LifeCycle.getSession();
      try {
        // Perhaps we should check that :-)
        session.setScript(req.getParameter("name"), script);
      }
      catch (CompilationFailedException e) {
        e.printStackTrace();
        JsonObject payload = new JsonObject();
        payload.addProperty("message", e.getMessage());
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.setContentType("application/json");
        gson.toJson(payload, resp.getWriter());
        return;
      }

      // Say ok
      resp.setStatus(200);
    }
  }
}
