package org.crsh.web;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;

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
      LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
      SimpleFS commands = lf.getCommands();
      commands.remove(name);
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

      // Check syntax errors and determine the name of the class
      CompilationUnit unit = new CompilationUnit();
      SourceUnit su = unit.addSource("whatever", script);

      //
      try {
        unit.compile(Phases.CLASS_GENERATION);
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

      //
      unit.getClasses();
      String mainClass = su.getAST().getMainClassName();

      //
      String name = req.getParameter("name");
      if (name == null || name.length() == 0) {
        name = mainClass;
      }

      //
      System.out.println("Saving " + name);

      // Save the command
      LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
      SimpleFS commands = lf.getCommands();
      commands.setCommand(name, script);

      // Say ok
      resp.setStatus(200);
    }
  }
}
