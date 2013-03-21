package org.crsh.web;

import org.crsh.command.GroovyScript;
import org.crsh.command.GroovyScriptCommand;
import org.crsh.command.ShellCommand;
import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.plugin.ResourceKind;
import org.crsh.plugin.SimplePluginDiscovery;
import org.crsh.plugin.WebPluginLifeCycle;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.shell.impl.command.AbstractClassManager;
import org.crsh.shell.impl.command.CRaSH;
import org.crsh.shell.impl.command.ClassManager;
import org.crsh.util.Safe;
import org.crsh.util.TimestampedObject;
import org.crsh.vfs.FS;
import org.crsh.vfs.Path;
import org.crsh.vfs.Resource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebListener
public class LifeCycle extends WebPluginLifeCycle implements HttpSessionListener, ServletRequestListener
{

  static {
    System.setSecurityManager(new CRaSHSecurityManager());
  }

  public static LifeCycle getLifeCycle(ServletContext sc) {
    return registry.get(sc.getContextPath());
  }

  /** . */
  private static final ConcurrentHashMap<String, LifeCycle> registry = new ConcurrentHashMap<String, LifeCycle>();

  /** . */
  private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	/** . */
	private ShellFactory crash;

  /** . */
  final ThreadLocal<HttpServletRequest> current = new ThreadLocal<HttpServletRequest>();

  /** . */
  private final SimpleFS commands = new SimpleFS(this);

  public Session getSession() {
    return sessions.get(current.get().getSession().getId());
  }

  public void requestInitialized(ServletRequestEvent sre) {
    // Force session creation
    ((HttpServletRequest)sre.getServletRequest()).getSession();
    current.set((HttpServletRequest)sre.getServletRequest());
  }

  public void requestDestroyed(ServletRequestEvent sre) {
    current.remove();
  }

  public void sessionCreated(HttpSessionEvent se) {
    sessions.put(se.getSession().getId(), new Session(crash));
	}

	public void sessionDestroyed(HttpSessionEvent se)
	{
    Session session = sessions.remove(se.getSession().getId());
    Safe.close(session.getShell());
	}

	public void contextInitialized(ServletContextEvent sce)
	{
    super.contextInitialized(sce);

    //
    PluginContext context = getPluginContext(sce.getServletContext());

    //
    crash = context.getPlugin(ShellFactory.class);

    //
    registry.put(sce.getServletContext().getContextPath(), this);
	}

  void removeCommand(String name) {
    commands.remove(name);
    getSession().classes.remove(name);
  }

  @Override
  protected PluginDiscovery createDiscovery(ServletContext context, ClassLoader classLoader) {
    class Factory extends CRaSHPlugin<ShellFactory> implements ShellFactory {
      private CRaSH crash;
      @Override
      public void init() {
        final PluginContext context = getContext();
        crash = new CRaSH(
          context,
          new AbstractClassManager<ShellCommand>(context, ShellCommand.class, GroovyScriptCommand.class) {
            @Override
            protected TimestampedObject<Class<? extends ShellCommand>> loadClass(String name) {
              return getSession().classes.get(name);
            }
            @Override
            protected void saveClass(String name, TimestampedObject<Class<? extends ShellCommand>> clazz) {
              getSession().classes.put(name, clazz);
            }
            @Override
            protected Resource getResource(String name) {
              return context.loadResource(name, ResourceKind.COMMAND);
            }
          },
          new ClassManager<GroovyScript>(context, ResourceKind.LIFECYCLE, GroovyScript.class, GroovyScript.class));
      }
      @Override
      public ShellFactory getImplementation() {
        return this;
      }
      public Shell create(Principal principal) {
        return crash.createSession(principal);
      }
    }
    SimplePluginDiscovery discovery = new SimplePluginDiscovery();
    discovery.add(new Factory());
    return discovery;
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
    registry.remove(sce.getServletContext().getContextPath());

    //
    if (crash != null) {
      crash = null;
    }

    //
    super.contextDestroyed(sce);
	}

  public SimpleFS getCommands() {
    return commands;
  }
}
