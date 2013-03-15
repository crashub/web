package org.crsh.web;

import org.crsh.vfs.spi.FSDriver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class SimpleFS implements FSDriver<String> {

  private static class Entry {
    final String content;
    final long lastModified;
    private Entry(String content) {
      this.content = content;
      this.lastModified = System.currentTimeMillis();
    }
  }

  /** . */
  final ThreadLocal<HttpServletRequest> current = new ThreadLocal<HttpServletRequest>();

  private HashMap<String, Entry> getEntries() {
    return getEntries(false);
  }

  private HashMap<String, Entry> getEntries(boolean create) {
    HttpServletRequest request = current.get();
    if (request != null) {
      HttpSession session = request.getSession(create);
      if (session != null) {
        HashMap<String, Entry> entries = (HashMap<String, Entry>)session.getAttribute("entries");
        if (entries == null && create) {
          session.setAttribute("entries", entries = new HashMap<String, Entry>());
        }
        return entries;
      } else {
        return null;
      }
    } else {
      if (create) {
        System.out.println("Cannot create entries with non existing request");
      }
      return null;
    }
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
      final HashMap<String, Entry> entries = getEntries();
      if (entries != null) {
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
    } else {
      return Collections.emptyList();
    }
  }

  public long getLastModified(String handle) throws IOException {
    if ("/".equals(handle) || "user/".equals(handle)) {
      return 0;
    } else  {
      HashMap<String, Entry> entries = getEntries();
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
      HashMap<String, Entry> entries = getEntries();
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

  public void setCommand(String name, String text) {
    HashMap<String, Entry> entries = getEntries(true);
    entries.put(name, new Entry(text));
  }
}
