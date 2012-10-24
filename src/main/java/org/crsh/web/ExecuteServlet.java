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
@WebServlet(urlPatterns = "/execute", asyncSupported = true)
public class ExecuteServlet extends HttpServlet
{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String line = req.getParameter("line");
		String widthP = req.getParameter("width");
		int width = 80;
		if (widthP != null)
		{
			try
			{
				int parsed = Integer.parseInt(widthP);
				if (parsed > 0)
				{
					width = parsed;
				}
			}
			catch (NumberFormatException ignore)
			{
			}
		}
		HttpSession session = req.getSession();
		Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
		ShellProcess process = shell.createProcess(line);
		CommandExecution execution = new CommandExecution(process, width);
		session.setAttribute("execution", new SerializableTransient<CommandExecution>(execution));
		try
		{
			ShellResponse response = execution.execute();
			if (response != null)
			{
				resp.getWriter().write(CompleteServlet.gson.toJson(execution.getDisplay()));
			}
		}
		finally
		{
			session.removeAttribute("execution");
		}
	}
}
