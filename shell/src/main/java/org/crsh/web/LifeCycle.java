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

import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.plugin.WebPluginLifeCycle;
import org.crsh.shell.ShellFactory;
import org.crsh.util.Utils;
import org.crsh.vfs.FS;
import org.crsh.vfs.Path;
import org.crsh.web.servlet.CRaSHConnector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.LogManager;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebListener
public class LifeCycle extends WebPluginLifeCycle implements HttpSessionListener
{

  /** . */
  static final String log_config =
      // Logging
      "handlers = java.util.logging.ConsoleHandler\n" +
      // Console Logging
      "java.util.logging.ConsoleHandler.level = ALL\n" +
      // Default global logging level
      ".level=ALL\n";

  static {
    // Configure logging
    try {
      LogManager logManager = LogManager.getLogManager();
      logManager.readConfiguration(new ByteArrayInputStream(log_config.getBytes()));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

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
      if (id != null) {
        session = sessions.get(id);
      }
    }
    return session;
  }

  public void sessionCreated(HttpSessionEvent se) {
  }

  public void sessionDestroyed(HttpSessionEvent se) {
    Session session = sessions.remove((String)se.getSession().getAttribute("CRASHID"));
    current.set(session);
    try {
      Utils.close(session.getShell());
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
  protected PluginContext create(Map<String, Object> attributes, PluginDiscovery discovery, ClassLoader loader) {
    FS cmdFS;
    FS confFS;
    try {
      cmdFS = new FS();
      cmdFS.mount(Thread.currentThread().getContextClassLoader(), Path.get("/crash/commands/"));
      cmdFS.mount(commands);
      confFS = new FS();
      confFS.mount(Thread.currentThread().getContextClassLoader(), Path.get("/crash/"));
    }
    catch (Exception e) {
      log.log(Level.SEVERE, "Coult not initialize the file system", e);
      return null;
    }
    return new PluginContext(
        new ContextualExecutorService(this, 20),
        new ScheduledThreadPoolExecutor(1),
        discovery,
        attributes,
        cmdFS,
        confFS,
        loader);
  }

  @Override
  protected String resolveCmdMountPointConfig() {
    return "classpath:/crash/commands/;simple:";
  }

  @Override
  protected String resolveConfMountPointConfig() {
    return "classpath:/crash/";
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
