package org.crsh.web;

import org.crsh.vfs.spi.FSDriver;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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
    HttpSession session = current.get().getSession(false);
    if (session != null) {
      HashMap<String, Entry> entries = (HashMap<String, Entry>)session.getAttribute("entries");
      if (entries != null) {
        return entries;
      }
    }
    return null;
  }

  public String root() throws IOException {
    return "";
  }

  public String name(String handle) throws IOException {
    return handle;
  }

  public boolean isDir(String handle) throws IOException {
    return handle.length() == 0;
  }

  public String child(String handle, String name) throws IOException {
    if (handle.length() == 0) {
      return name;
    } else {
      return null;
    }
  }

  public Iterable<String> children(String handle) throws IOException {
    if (handle.length() > 0) {
      HashMap<String, Entry> entries = getEntries();
      if (entries != null) {
        return entries.keySet();
      }
    }
    return Collections.emptyList();
  }

  public long getLastModified(String handle) throws IOException {
    if (handle.length() > 0) {
      HashMap<String, Entry> entries = getEntries();
      Entry entry = entries.get(handle);
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return entry.lastModified;
      }
    } else {
      return 0;
    }
  }

  public InputStream open(String handle) throws IOException {
    if (handle.length() > 0) {
      HashMap<String, Entry> entries = getEntries();
      Entry entry = entries.get(handle);
      if (entry == null) {
        throw new IOException("No such entry " + handle);
      } else {
        return new ByteArrayInputStream(entry.content.getBytes("UTF-8"));
      }
    } else {
      throw new IOException("No such entry " + handle);
    }
  }
}
