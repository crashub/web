package org.crsh.web;

import org.crsh.shell.Shell;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class Connection implements AsyncListener
{

  /** . */
  final ExecuteServlet servlet;

  /** . */
  final AsyncContext context;

  /** . */
  final String id;

  /** . */
  final Shell shell;

  /** . */
  ProcessContext current;

  Connection(ExecuteServlet servlet, AsyncContext context, Shell shell, String id) {
    this.servlet = servlet;
    this.context = context;
    this.id = id;
    this.shell = shell;
  }

  void process(ExecuteServlet.Event event) {

    if("message".equals(event.type)) {

      //
      if (current != null) {
        System.out.println("Duplicate process execution");
      } else {
        // Create a shell session if needed
        if (shell == null) {
          // Should use request principal :-)
        }

        //
        Map<String, Object> map = (Map<String, Object>) event.data;
        String line = (String)map.get("line");
        Double widthP = (Double)map.get("width");
        Double heightP = (Double)map.get("height");
        int width = 110;
        if (widthP != null) {
          width = widthP.intValue();
        }
        int height = 30;
        if (heightP != null) {
          height = heightP.intValue();
        }

        // Execute process and we are done
        current = new ProcessContext(this, line, width, height);
        current.begin();
      }
    } else {
      System.out.println("Unhandled event " + event);
    }
  }

  public void onStartAsync(AsyncEvent event) throws IOException {
    // Should not be called
  }

  public void onComplete(AsyncEvent event) throws IOException {
    System.out.println("onComplete " + id);
    servlet.connections.remove(id);
  }

  public void onTimeout(AsyncEvent event) throws IOException {
    System.out.println("onTimeOut " + id);
    servlet.connections.remove(id);
  }

  public void onError(AsyncEvent event) throws IOException {
    System.out.println("onError " + id);
    servlet.connections.remove(id);
  }
}
