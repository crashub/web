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

import org.crsh.vfs.spi.FSDriver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class SimpleFS implements FSDriver<String> {

  public static class Entry {
    final String content;
    final long lastModified;
    private Entry(String content) {
      this.content = content;
      this.lastModified = System.currentTimeMillis();
    }
  }

  /** . */
  final LifeCycle lifeCycle;

  SimpleFS(LifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  public String root() throws IOException {
    return "/";
  }

  public String name(String handle) throws IOException {
    if ("/".equals(handle)) {
      return "";
    } else if ("user/".equals(handle)) {
      return "user";
    } else {
      return handle.substring("user/".length()) + ".groovy";
    }
  }

  public boolean isDir(String handle) throws IOException {
    return handle.charAt(handle.length() - 1) == '/';
  }

  public String child(String handle, String name) throws IOException {
    if ("/".equals(handle) && name.equals("user")) {
      return "user/";
    } else if ("user/".equals(handle)) {
      return "user/" + name;
    } else {
      return null;
    }
  }

  public Iterable<String> children(String handle) throws IOException {
    if ("/".equals(handle)) {
      return Collections.singletonList("user/");
    } else if ("user/".equals(handle)) {
      final HashMap<String, Entry> entries = lifeCycle.getSession().commands;
      return new Iterable<String>() {
        public Iterator<String> iterator() {
          return new Iterator<String>() {
            final Iterator<String> i = entries.keySet().iterator();
            public boolean hasNext() {
              return i.hasNext();
            }
            public String next() {
              return "user/" + i.next();
            }
            public void remove() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    } else {
      return Collections.emptyList();
    }
  }

  public long getLastModified(String handle) throws IOException {
    if ("/".equals(handle) || "user/".equals(handle)) {
      return 0;
    } else  {
      HashMap<String, Entry> entries = lifeCycle.getSession().commands;
      Entry entry = entries.get(handle.substring("user/".length()));
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return entry.lastModified;
      }
    }
  }

  public InputStream open(String handle) throws IOException {
    if (handle.startsWith("user/")) {
      HashMap<String, Entry> entries = lifeCycle.getSession().commands;
      Entry entry = entries.get(handle.substring("user/".length()));
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return new ByteArrayInputStream(entry.content.getBytes("UTF-8"));
      }
    } else {
      throw new IOException("No such entry " + handle);
    }
  }

  void setScript(String name, String text) {
    HashMap<String, Entry> entries = lifeCycle.getSession().commands;
    entries.put(name, new Entry(text));
  }

  boolean remove(String name) {
    HashMap<String, Entry> entries = lifeCycle.getSession().commands;
    return entries.remove(name) != null;
  }

  Iterable<String> list() {
    HashMap<String, Entry> entries = lifeCycle.getSession().commands;
    return entries.keySet();
  }

  String getScript(String name) {
    HashMap<String, Entry> entries = lifeCycle.getSession().commands;
    Entry entry = entries.get(name);
    if (entry != null) {
      return entry.content;
    }
    return null;
  }
}
