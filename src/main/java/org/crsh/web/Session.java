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

import org.crsh.command.ShellCommand;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.util.IO;
import org.crsh.util.TimestampedObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Our session.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class Session {

  private static SimpleFS.Entry get(String name) {
    InputStream in = Session.class.getResourceAsStream(name + ".groovy");
    if (in != null) {
      return new SimpleFS.Entry(IO.readAsUTF8(in));
    } else {
      return new SimpleFS.Entry("// Could not retrieve command : " + name);
    }
  }

  /** Initial commands. */
  private static final HashMap<String, SimpleFS.Entry> initial = new LinkedHashMap<String, SimpleFS.Entry>();

  static {
    initial.put("hello", get("hello"));
    initial.put("date", get("date"));
    initial.put("dashboard", get("dashboard"));
  }

  /** . */
  private Shell shell;

  /** . */
  private final ShellFactory factory;

  /** . */
  final HashMap<String, TimestampedObject<Class<? extends ShellCommand>>> classes;

  /** . */
  final HashMap<String, SimpleFS.Entry> commands;

  Session(ShellFactory factory) {
    this.factory = factory;
    this.classes = new HashMap<String, TimestampedObject<Class<? extends ShellCommand>>>();
    this.commands = new LinkedHashMap<String, SimpleFS.Entry>(initial);
  }

  /** . */
  public Shell getShell() {
    if (shell == null) {
      shell = factory.create(null);
    }
    return shell;
  }
}
