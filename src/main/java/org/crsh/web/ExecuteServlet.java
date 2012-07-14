package org.crsh.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellResponse;
import org.crsh.text.Color;
import org.crsh.text.Decoration;
import org.crsh.text.Style;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/execute")
public class ExecuteServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String line = req.getParameter("line");
    String widthP = req.getParameter("width");
    int width = 80;
    if (widthP != null) {
      try {
        int parsed = Integer.parseInt(widthP);
        if (parsed > 0) {
          width = parsed;
        }
      }
      catch (NumberFormatException ignore) {
      }
    }
    HttpSession session = req.getSession();
    Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
    ShellProcess process = shell.createProcess(line);
    CommandExecution execution = new CommandExecution(process, width);
    session.setAttribute("execution", new SerializableTransient<CommandExecution>(execution));
    try {
      ShellResponse response = execution.execute();
      if (response != null) {
        Style style = Style.reset;
        JsonArray array = new JsonArray();
        for (Object o : response.getReader()) {
          if (o instanceof Style) {
            style = style.merge((Style)o);
          } else {
            JsonObject elt = new JsonObject();
            if (style != null &&
               (style.getBackground() != null ||
                style.getForeground() != null ||
                style.getDecoration() != null)
               ) {
              if (style.getDecoration() != null) {
                elt.addProperty("decoration", style.getDecoration().name());
              }
              if (style.getForeground() != null) {
                elt.addProperty("fg", style.getForeground().name());
              }
              if (style.getBackground() != null) {
                elt.addProperty("bg", style.getBackground().name());
              }
            }
            String text = o.toString();
            elt.addProperty("text", text);
            array.add(elt);
          }
        }
        resp.getWriter().write(CompleteServlet.gson.toJson(array));
      }
    }
    finally {
      session.removeAttribute("execution");
    }
  }
}
