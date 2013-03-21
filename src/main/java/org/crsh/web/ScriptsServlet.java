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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/scripts")
public class ScriptsServlet extends HttpServlet {

  /** . */
  static final Gson gson = new Gson();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
    SimpleFS commands = lf.getCommands();
    Iterable<String> names = commands.list();
    HashMap<String, String> map = new LinkedHashMap<String, String>();
    for (String name : names) {
      String script = commands.getScript(name);
      if (name != null) {
        map.put(name, script);
      }
    }
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("application/json");
    gson.toJson(map, resp.getWriter());
  }
}
