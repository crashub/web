/*
 * Copyright (C) 2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.crsh.web;

import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.plugin.SimplePluginDiscovery;
import org.crsh.plugin.WebPluginLifeCycle;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.shell.impl.async.AsyncShell;
import org.crsh.shell.impl.command.CRaSH;
import org.crsh.shell.impl.command.CRaSHSession;
import org.crsh.util.Safe;
import org.crsh.util.ServletContextMap;
import org.crsh.vfs.FS;
import org.crsh.vfs.Path;
import org.crsh.web.servlet.CRaSHConnector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Permission;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebListener
public class LifeCycle extends WebPluginLifeCycle implements HttpSessionListener
{

  public static LifeCycle getLifeCycle(ServletContext sc) {
    return registry.get(sc.getContextPath());
  }

  /** . */
  private static final ConcurrentHashMap<String, LifeCycle> registry = new ConcurrentHashMap<String, LifeCycle>();

  /** . */
  final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	/** . */
	ShellFactory crash;

  /** The current session. */
  final ThreadLocal<Session> current = new ThreadLocal<Session>();

  /** . */
  private final SimpleFS commands = new SimpleFS(this);

  public Session getSession() {
    Session session = current.get();
    if (session == null) {
      // Special case : need to handle it better
      String id = CRaSHConnector.getHttpSessionId();
      session = sessions.get(id);
    }
    return session;
  }

  public void sessionCreated(HttpSessionEvent se) {
  }

  public void sessionDestroyed(HttpSessionEvent se) {
    Session session = sessions.remove((String)se.getSession().getAttribute("CRASHID"));
    current.set(session);
    try {
      Safe.close(session.getShell());
    }
    finally {
      current.set(null);
    }
  }

	public void contextInitialized(ServletContextEvent sce)
	{
    super.contextInitialized(sce);

    //
    PluginContext context = getPluginContext(sce.getServletContext().getContextPath());

    //
    crash = context.getPlugin(ShellFactory.class);

    //
    registry.put(sce.getServletContext().getContextPath(), this);

    // Preload all the classes used by the security manager to avoid ClassCircularityError...
    Class a = RuntimePermission.class;
    Class b = Permission.class;
    Class c = SecurityManager.class;
    Class d = SecurityException.class;
    System.setSecurityManager(new CRaSHSecurityManager());
	}

  void removeCommand(String name) {
    commands.remove(name);
    getSession().classes.remove(name);
  }

  @Override
  protected PluginContext createPluginContext(ServletContext context, FS cmdFS, FS confFS, PluginDiscovery discovery) {
    return new PluginContext(
        new ContextualExecutorService(this, 20),
        new ScheduledThreadPoolExecutor(1),
        discovery,
        new ServletContextMap(context),
        cmdFS,
        confFS,
        context.getClassLoader());
  }

  @Override
  protected PluginDiscovery createDiscovery(ServletContext context, ClassLoader classLoader) {
    class Factory extends CRaSHPlugin<ShellFactory> implements ShellFactory {
      @Override
      public ShellFactory getImplementation() {
        return this;
      }
      public Shell create(Principal principal) {
        PluginContext context = getContext();
        CRaSH crash = new CRaSH(context);
        CRaSHSession session = crash.createSession(principal);
        return new AsyncShell(getContext().getExecutor(), session);
      }
    }
    SimplePluginDiscovery discovery = new SimplePluginDiscovery();
    discovery.add(new Factory());
    for (CRaSHPlugin<?> plugin : super.createDiscovery(context, classLoader).getPlugins()) {
      if (!plugin.getType().isAssignableFrom(ShellFactory.class)) {
        discovery.add(plugin);
      }
    }
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
