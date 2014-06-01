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

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.crsh.lang.impl.groovy.command.GroovyScriptCommand;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.shell.impl.command.spi.Command;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Our session.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class Session {

  private static final Logger log = Logger.getLogger(Session.class.getSimpleName());

  /** Initial commands. */
  private static final HashMap<String, Script> initial = new LinkedHashMap<String, Script>();

  static {
    // Now as templates
//    initial.put("hello", get("hello"));
//    initial.put("date", get("date"));
//    initial.put("dashboard", get("dashboard"));
  }

  /** . */
  private Shell shell;

  /** . */
  private final ShellFactory factory;

  /** . */
  final HashMap<String, Script> commands;

  Session(ShellFactory factory) {
    this.factory = factory;
    this.commands = new LinkedHashMap<String, Script>(initial);
  }

  /** . */
  public Shell getShell() {
    if (shell == null) {
      shell = factory.create(null);
    }
    return shell;
  }

  void clearScripts() {
    Session session = LifeCycle.getSession();
    session.commands.clear();
  }

  void removeScript(String name) {
    commands.remove(name);
  }

  void setScript(String scriptName, String scriptText) throws CompilationFailedException {

    // Check syntax errors and determine the name of the class
    CompilerConfiguration config = new CompilerConfiguration();
    config.setRecompileGroovySource(true);
    config.setScriptBaseClass(GroovyScriptCommand.class.getName());
    GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config);

    //
    Class<? extends Command<?>> clazz = gcl.parseClass(scriptText);

    //
    if (scriptName == null || scriptName.length() == 0) {
      scriptName = clazz.getSimpleName();
    }

    //
    log.log(Level.INFO, "Saving script " + scriptName + " " + scriptText);

    // Save the command
    Session session = LifeCycle.getSession();
    session.commands.put(scriptName, new Script(scriptName, scriptText, clazz));
  }
}
