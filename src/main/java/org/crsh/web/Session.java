package org.crsh.web;

import org.crsh.command.ShellCommand;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.util.TimestampedObject;

import java.util.HashMap;

/**
 * Our session.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class Session {

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
    this.commands = new HashMap<String, SimpleFS.Entry>();
  }

  /** . */
  public Shell getShell() {
    if (shell == null) {
      shell = factory.create(null);
    }
    return shell;
  }
}
