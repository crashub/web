package org.crsh.web;

import org.crsh.plugin.PluginContext;
import org.crsh.plugin.WebPluginLifeCycle;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.util.Safe;
import org.crsh.vfs.FS;
import org.crsh.vfs.Path;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.reflect.UndeclaredThrowableException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebListener
public class LifeCycle extends WebPluginLifeCycle implements HttpSessionListener, ServletRequestListener
{

	/** . */
	private ShellFactory crash;

  /** . */
  private final SimpleFS commands = new SimpleFS();

  public void requestInitialized(ServletRequestEvent sre) {
    commands.current.set((HttpServletRequest)sre.getServletRequest());
  }

  public void requestDestroyed(ServletRequestEvent sre) {
    commands.current.remove();
  }

  public void sessionCreated(HttpSessionEvent se)
	{
		HttpSession session = se.getSession();
		Shell shell = crash.create(null);
		session.setAttribute("crash", new SerializableTransient<Shell>(shell));
	}

	public void sessionDestroyed(HttpSessionEvent se)
	{
		HttpSession session = se.getSession();
		SerializableTransient<Shell> ref = (SerializableTransient<Shell>)session.getAttribute("crash");
		if (ref != null && ref.object != null)
		{
			Safe.close(ref.object);
		}
	}

	public void contextInitialized(ServletContextEvent sce)
	{
    super.contextInitialized(sce);

    //
    PluginContext context = getPluginContext(sce.getServletContext());

    //
    crash = context.getPlugin(ShellFactory.class);
	}

  @Override
  protected FS createCommandFS(ServletContext context) {
    try {
      FS fs = super.createCommandFS(context);
      fs.mount(Thread.currentThread().getContextClassLoader(), Path.get("/crash/commands/"));
      fs.mount(commands);
      return fs;
    }
    catch (Exception e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  protected FS createConfFS(ServletContext context) {
    try {
      FS fs = super.createCommandFS(context);
      fs.mount(Thread.currentThread().getContextClassLoader(), Path.get("/crash/"));
      return fs;
    }
    catch (Exception e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public void contextDestroyed(ServletContextEvent sce)
	{
    super.contextDestroyed(sce);

    //
    if (crash != null) {
      crash = null;
    }
	}
}
