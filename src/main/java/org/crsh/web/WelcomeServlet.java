package org.crsh.web;

import org.crsh.shell.Shell;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/welcome")
public class WelcomeServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
    String welcome = shell.getWelcome();
    resp.getWriter().print(welcome);
  }
}
