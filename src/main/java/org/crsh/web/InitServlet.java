package org.crsh.web;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/init")
public class InitServlet extends HttpServlet {

  /** . */
  private static final Pattern GROOVY = Pattern.compile("(\\p{Alpha}\\p{Alnum}*)(?:\\.groovy)?", Pattern.CASE_INSENSITIVE);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String id = req.getParameter("id");
    if (id == null) {
      // Use a default id
      id = "5205321";
    }

    //
    Client c = Client.create();
    WebResource r = c.resource("https://api.github.com/gists/" + id);
    ClientResponse response = r.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
    String entity = response.getEntity(String.class);
    JsonObject object= (JsonObject)new JsonParser().parse(entity);

    //
    LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());

    //
    JsonObject files = object.getAsJsonObject("files");
    for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
      Matcher m = GROOVY.matcher(entry.getKey());
      if (m.matches()) {
        String name = m.group(1);
        JsonObject file = (JsonObject)entry.getValue();
        String content = file.get("content").getAsString();
        lf.getCommands().setCommand(name, content);
      }
    }

    //
    resp.sendRedirect(req.getContextPath());
  }
}
