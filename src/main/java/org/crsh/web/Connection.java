package org.crsh.web;

import org.crsh.shell.impl.command.CRaSHSession;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class Connection implements AsyncListener
{

  /** . */
  final ProcessServlet servlet;

  /** . */
  final AsyncContext context;

  /** . */
  final String id;

  /** . */
  final CRaSHSession session;

  /** . */
  ProcessContext current;

  Connection(ProcessServlet servlet, AsyncContext context, String id) {
    this.servlet = servlet;
    this.context = context;
    this.id = id;
    this.session = servlet.crash.createSession(null);
  }

  void process(ProcessServlet.Event event) {

    if("message".equals(event.type)) {

      //
      if (current != null) {
        System.out.println("DUPLICATE PROCESS EXECUTION (WTF?)");
        return;
      }

      // Create a shell session if needed
      if (session == null) {
        // Should use request principal :-)
      }

      //
      Map<String, Object> map = (Map<String, Object>) event.data;
      String line = (String)map.get("line");
      Double widthP = (Double)map.get("width");
      int width = 80;
      if (widthP != null)
      {
        width = widthP.intValue();
      }

      // Execute process and we are done
      current = new ProcessContext(this, line, width);
      current.begin();
    }
  }











  public void onStartAsync(AsyncEvent event) throws IOException {
    System.out.println("STARTING " + id);
    servlet.connections.put(id, this);
  }

  public void onComplete(AsyncEvent event) throws IOException {
    System.out.println("COMPLETING " + id);
    servlet.connections.remove(id);
  }

  public void onTimeout(AsyncEvent event) throws IOException {
    System.out.println("TIMED OUT " + id);
    servlet.connections.remove(id);
  }

  public void onError(AsyncEvent event) throws IOException {
    System.out.println("ERROR " + id);
    servlet.connections.remove(id);
  }
}
