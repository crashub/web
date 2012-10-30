package org.crsh.web;

import org.crsh.shell.impl.command.CRaSH;
import org.crsh.shell.impl.command.CRaSHSession;
import org.crsh.standalone.Bootstrap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.reflect.UndeclaredThrowableException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebListener
public class LifeCycle implements HttpSessionListener, ServletContextListener
{

	/** . */
	private Bootstrap bootstrap;

	/** . */
	private CRaSH crash;

	public void sessionCreated(HttpSessionEvent se)
	{
		HttpSession session = se.getSession();
		ServletContext context = session.getServletContext();
		CRaSH crash = (CRaSH)context.getAttribute("crash");
		CRaSHSession shell = crash.createSession(null);
		session.setAttribute("crash", new SerializableTransient<CRaSHSession>(shell));
	}

	public void sessionDestroyed(HttpSessionEvent se)
	{
		HttpSession session = se.getSession();
		SerializableTransient<CRaSHSession> ref = (SerializableTransient<CRaSHSession>)session.getAttribute("crash");
		if (ref != null && ref.object != null)
		{
			ref.object.close();
		}
	}

	public void contextInitialized(ServletContextEvent sce)
	{
		System.out.println("Starting");
		try
		{
			Bootstrap bootstrap = new Bootstrap(Thread.currentThread().getContextClassLoader());
			bootstrap.bootstrap();
			crash = new CRaSH(bootstrap.getContext());
			sce.getServletContext().setAttribute("crash", crash);
			this.bootstrap = bootstrap;
		}
		catch (Exception e)
		{
			throw new UndeclaredThrowableException(e);
		}
		System.out.println("Started");
	}

	public void contextDestroyed(ServletContextEvent sce)
	{
		if (bootstrap != null)
		{
			sce.getServletContext().setAttribute("crash", null);
			crash = null;
			bootstrap.shutdown();
			bootstrap = null;
		}
	}
}
