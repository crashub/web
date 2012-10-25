package org.crsh.web;

import com.google.gson.JsonObject;
import org.crsh.shell.Shell;
import org.crsh.shell.impl.command.CRaSH;
import org.crsh.shell.impl.command.CRaSHSession;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/welcome")
public class WelcomeServlet extends HttpServlet
{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
		/*if (shell == null)
		{
			ServletContext context = req.getServletContext();
			CRaSH crash = (CRaSH)context.getAttribute("crash");
			req.getSession(true).setAttribute("crash", new SerializableTransient<CRaSHSession>(crash.createSession(null)));
		}*/
		String welcome = shell.getWelcome();
		String prompt = shell.getPrompt();
		JsonObject obj = new JsonObject();
		obj.addProperty("welcome", welcome);
		obj.addProperty("prompt", prompt);
		resp.getWriter().write(CompleteServlet.gson.toJson(obj));
	}
}
