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
import org.crsh.shell.impl.command.spi.Command;
import org.crsh.shell.impl.command.spi.CommandException;
import org.crsh.shell.impl.command.spi.CommandResolver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A custom implementation of the command resolve that resolves the scripts in the current user session.
 *
 * @author Julien Viet
 */
public class CommandResolverImpl extends CRaSHPlugin<CommandResolver> implements CommandResolver {

  public CommandResolver getImplementation() {
    return this;
  }

  public Iterable<Map.Entry<String, String>> getDescriptions() {
    Session session = LifeCycle.getSession();
    if (session != null) {
      LinkedHashMap<String, String> descriptions = new LinkedHashMap<String, String>();
      for (Script s : session.commands.values()) {
        try {
          descriptions.put(s.name, s.getCommand().getDescriptor().getDescription().getUsage());
        }
        catch (CommandException e) {
          // Should not happen
        }
      }
      return descriptions.entrySet();
    }
    return Collections.<String, String>emptyMap().entrySet();
  }

  public Command<?> resolveCommand(String name) throws CommandException, NullPointerException {
    Session session = LifeCycle.getSession();
    if (session != null) {
      Script script =  session.commands.get(name);
      if (script != null) {
        return script.getCommand();
      }
    }
    return null;
  }
}
