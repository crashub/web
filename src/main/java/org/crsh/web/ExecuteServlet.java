package org.crsh.web;

import org.crsh.shell.Shell;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/execute")
public class ExecuteServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String line = req.getParameter("line");
    HttpSession session = req.getSession();
    Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
    ShellProcess process = shell.createProcess(line);
    CommandExecution execution = new CommandExecution(process);
    session.setAttribute("execution", new SerializableTransient<CommandExecution>(execution));
    try {
      ShellResponse response = execution.execute();
      if (response != null) {
        resp.getWriter().write(response.getText());
      }
    }
    finally {
      session.removeAttribute("execution");
    }
  }
}
