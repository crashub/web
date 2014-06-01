package org.crsh.web;

import org.crsh.cli.impl.descriptor.IntrospectionException;
import org.crsh.command.BaseCommand;
import org.crsh.lang.impl.groovy.command.GroovyScriptCommand;
import org.crsh.lang.impl.groovy.command.GroovyScriptShellCommand;
import org.crsh.lang.impl.java.ClassShellCommand;
import org.crsh.shell.ErrorKind;
import org.crsh.shell.impl.command.spi.Command;
import org.crsh.shell.impl.command.spi.CommandException;

/**
* @author Julien Viet
*/
public class Script {

  final String name;
  final String content;
  final long lastModified;
  final Class<? extends Command<?>> clazz;

  public Script(String name, String content, Class<? extends Command<?>> clazz) {
    this.content = content;
    this.lastModified = System.currentTimeMillis();
    this.clazz = clazz;
    this.name = name;
  }

  public Command<?> getCommand() throws CommandException, NullPointerException {
    Session session = LifeCycle.getSession();
    if (session != null) {
      if (BaseCommand.class.isAssignableFrom(clazz)) {
        Class<? extends BaseCommand> cmd = clazz.asSubclass(BaseCommand.class);
        try {
          return make(cmd);
        }
        catch (IntrospectionException e) {
          throw new CommandException(name, ErrorKind.EVALUATION, "Invalid cli annotations", e);
        }
      }
      else if (GroovyScriptCommand.class.isAssignableFrom(clazz)) {
        Class<? extends GroovyScriptCommand> cmd = clazz.asSubclass(GroovyScriptCommand.class);
        try {
          return make2(cmd);
        }
        catch (IntrospectionException e) {
          throw new CommandException(name, ErrorKind.EVALUATION, "Invalid cli annotations", e);
        }
      }
      else {
        throw new CommandException(name, ErrorKind.INTERNAL, "Could not create command " + name + " instance");
      }
    }
    return null;
  }

  private <C extends BaseCommand> ClassShellCommand<C> make(Class<C> clazz) throws IntrospectionException {
    return new ClassShellCommand<C>(clazz);
  }

  private <C extends GroovyScriptCommand> GroovyScriptShellCommand<C> make2(Class<C> clazz) throws IntrospectionException {
    return new GroovyScriptShellCommand<C>(clazz);
  }
}
