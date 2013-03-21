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

import java.security.Permission;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class CRaSHSecurityManager extends SecurityManager {

  @Override
  public void checkPermission(Permission perm, Object context) {
    checkPermission(perm);
  }

  @Override
  public void checkPermission(Permission perm) {

    // Deny access to crash. properties
    if (perm instanceof RuntimePermission) {
      RuntimePermission runtimePerm = (RuntimePermission)perm;
      String name = runtimePerm.getName();
      if (name.startsWith("exitVM.")) {
        throw new SecurityException("Nice try dude... try harder");
      }
      if (name.equals("stopThread")) {
        throw new SecurityException("I'm gonna kick your ass, bitch!");
      }
      if (name.equals("setSecurityManager")) {
        throw new SecurityException("My boot, your face; the perfect couple.");
      }
      if (name.equals("createSecurityManager")) {
        throw new SecurityException("You're an inspiration for birth control.");
      }
      if (name.equals("setIO")) {
        throw new SecurityException("Your face, your ass - what's the difference?");
      }
    }
  }

}
