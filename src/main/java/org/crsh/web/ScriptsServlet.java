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
