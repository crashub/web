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
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class SimpleFS implements FSDriver<String> {

  public static class Entry {
    final String content;
    final long lastModified;
    public Entry(String content) {
      this.content = content;
      this.lastModified = System.currentTimeMillis();
    }
  }

  /** . */
  final LifeCycle lifeCycle;

  SimpleFS(LifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  private Map<String, Entry> getCommands() {
    Session session = null;
    try {
      session = lifeCycle.getSession();
    }
    catch (NullPointerException e) {
      // Workaround for initialization bug
      // 30-May-2014 21:37:46.191 SEVERE [localhost-startStop-1] org.apache.catalina.core.StandardContext.listenerStart Exception sending context initialized event to listener instance of class org.crsh.web.LifeCycle
      // java.lang.NullPointerException
      //   at org.crsh.web.servlet.CRaSHConnector.getHttpSessionId(CRaSHConnector.java:64)
      //   at org.crsh.web.LifeCycle.getSession(LifeCycle.java:68)
      //   at org.crsh.web.SimpleFS.getCommands(SimpleFS.java:50)
      //   at org.crsh.web.SimpleFS.children(SimpleFS.java:86)
      //   at org.crsh.web.SimpleFS.children(SimpleFS.java:31)
      //   at org.crsh.vfs.Handle.children(Handle.java:54)
      //   at org.crsh.vfs.File.children(File.java:100)
      //   at org.crsh.plugin.ResourceManager.refresh(ResourceManager.java:156)
      //   at org.crsh.plugin.PluginContext.refresh(PluginContext.java:321)
      //   at org.crsh.plugin.Embedded.start(Embedded.java:67)
      //   at org.crsh.plugin.WebPluginLifeCycle.contextInitialized(WebPluginLifeCycle.java:116)
      //   at org.crsh.web.LifeCycle.contextInitialized(LifeCycle.java:90)
    }
    return session != null ? session.commands : Collections.<String, Entry>emptyMap();
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
      final Map<String, Entry> entries = getCommands();
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
      Map<String, Entry> entries = getCommands();
      Entry entry = entries.get(handle.substring("user/".length()));
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return entry.lastModified;
      }
    }
  }

  public Iterator<InputStream> open(String handle) throws IOException {
    if (handle.startsWith("user/")) {
      Map<String, Entry> entries = getCommands();
      Entry entry = entries.get(handle.substring("user/".length()));
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return Collections.<InputStream>singleton(new ByteArrayInputStream(entry.content.getBytes("UTF-8"))).iterator();
      }
    } else {
      throw new IOException("No such entry " + handle);
    }
  }

  void clearScripts() {
    Map<String, Entry> entries = getCommands();
    if (entries.size() > 0) {
      entries.clear();
    }
  }

  void setScript(String name, String text) {
    Map<String, Entry> entries = getCommands();
    entries.put(name, new Entry(text));
  }

  boolean remove(String name) {
    Map<String, Entry> entries = getCommands();
    return entries.remove(name) != null;
  }

  Iterable<String> list() {
    Map<String, Entry> entries = getCommands();
    return entries.keySet();
  }

  String getScript(String name) {
    Map<String, Entry> entries = getCommands();
    Entry entry = entries.get(name);
    if (entry != null) {
      return entry.content;
    }
    return null;
  }
}
