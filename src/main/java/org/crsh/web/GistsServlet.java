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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/gists/*")
public class GistsServlet extends HttpServlet {

  /** . */
  private static final Pattern GROOVY = Pattern.compile("(\\p{Alpha}\\p{Alnum}*)(?:\\.groovy)?", Pattern.CASE_INSENSITIVE);

  /** . */
  private static final Logger log = Logger.getLogger(GistsServlet.class.getSimpleName());

  /** . */
  private final LoadingCache<String, JsonObject> loader = CacheBuilder.newBuilder().maximumSize(1000)
      .build(
          new CacheLoader<String, JsonObject>() {
            public JsonObject load(String key) throws Exception {
              Client c = Client.create();
              WebResource r = c.resource("https://api.github.com/gists/" + key);
              ClientResponse response = r.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
              String entity = response.getEntity(String.class);
              int status = response.getStatus();
              if (status >= 200 && status <= 299) {
                return (JsonObject)new JsonParser().parse(entity);
              } else {
                throw new Exception("Could not retriev gist " + key + " status=" + status + " body=" + entity);
              }
            }
          });

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String pathInfo = req.getPathInfo();
    if (pathInfo == null || pathInfo.length() < 2) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No gist id provided");
    } else {
      // Remove /
      String id = pathInfo.substring(1);

      // Get gist
      JsonObject object;
      try {
        object = loader.get(id);
        log.info(req.getRemoteHost() + " loaded gist " + id);
      }
      catch (ExecutionException e) {
        log.log(Level.SEVERE, req.getRemoteHost() + " could not access gist " + id, e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getCause().getMessage());
        return;
      }

      //
      LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
      SimpleFS commands = lf.getCommands();
      commands.clearScripts();
      JsonObject files = object.getAsJsonObject("files");
      for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
        Matcher m = GROOVY.matcher(entry.getKey());
        if (m.matches()) {
          String name = m.group(1);
          JsonObject file = (JsonObject)entry.getValue();
          String content = file.get("content").getAsString();
          commands.setScript(name, content);
        }
      }

      // Display index
      getServletContext().getRequestDispatcher("/index.html").include(req, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    //
    String pathInfo = req.getPathInfo();
    if (pathInfo != null && pathInfo.length() > 0) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No gist id must be provided");
    } else {

      // Build body
      JsonObject body = new JsonObject();
      body.addProperty("description", "A set of shell JVM commands for CRaSH http://try.crashub.org");
      body.addProperty("public", true);
      JsonObject files = new JsonObject();
      LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
      SimpleFS commands = lf.getCommands();
      for (String name : commands.list()) {
        String script = commands.getScript(name);
        JsonObject file = new JsonObject();
        file.addProperty("content", script);
        files.add(name + ".groovy", file);
      }
      body.add("files", files);

      // Perform request
      Client c = Client.create();
      WebResource r = c.resource("https://api.github.com/gists");
      ClientResponse response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, body.toString());
      String entity = response.getEntity(String.class);

      //
      int status = response.getStatus();
      if (status >= 200 && status <= 299) {
        JsonObject object= (JsonObject)new JsonParser().parse(entity);
        String id = object.getAsJsonPrimitive("id").getAsString();
        log.log(Level.INFO, req.getRemoteHost() + " created gist " + id);
        resp.sendRedirect("/gists/" + id);
      } else {
        log.log(Level.SEVERE, req.getRemoteHost() + " could not create gist status =" + status + " entity = " + entity);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create gist status =" + status + " entity = " + entity);
      }
    }
  }
}
