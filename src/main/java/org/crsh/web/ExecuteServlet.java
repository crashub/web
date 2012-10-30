package org.crsh.web;

import com.google.gson.Gson;
import org.crsh.shell.Shell;
import org.crsh.shell.impl.command.CRaSH;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/execute", asyncSupported = true)
public class ExecuteServlet extends HttpServlet {

  /** . */
  static final Map<String, Connection> connections = new ConcurrentHashMap<String, Connection>();

  /** . */
  CRaSH crash;

  @Override
  public void init() throws ServletException {
    crash = (CRaSH)getServletContext().getAttribute("crash");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String data = req.getReader().readLine();
    if (data != null) {
      data = data.substring("data=".length());
      Event event = new Gson().fromJson(data, Event.class);
      Connection conn = connections.get(event.socket);
      if (conn != null) {
        conn.process(event);
      } else {
        System.out.println("CONNECTION NOT FOUND " + event.socket);
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    SerializableTransient<Shell> shell = (SerializableTransient<Shell>)req.getSession().getAttribute("crash");

    //
    String id = req.getParameter("id");
    String transport = req.getParameter("transport");
    AsyncContext context = req.startAsync();
    context.setTimeout(300 * 1000L); // 5 minutes

    //
    resp.setCharacterEncoding("utf-8");
    resp.setHeader("Access-Control-Allow-Origin", "*");
    resp.setContentType("text/" + ("sse".equals(transport) ? "event-stream" : "plain"));

    //
    PrintWriter writer = resp.getWriter();
    for (int i = 0; i < 2000; i++) {
      writer.print(' ');
    }
    writer.print("\n");
    writer.flush();

    //
    Connection conn = new Connection(this, context, shell.object, id);
    connections.put(id, conn);
    context.addListener(conn);
  }

  static class Event
  {

    /** . */
    String type;

    /** . */
    String socket;

    /** . */
    Object data;

    public Event() {
    }

    public Event(String type) {
      this.type = type;
    }

    public Event data(Object data) {
      this.data = data;
      return this;
    }

    public Event socket(String socket) {
      this.socket = socket;
      return this;
    }

    @Override
    public String toString()
    {
      return "Event[type=" + type + ", socket=" + socket + ", data=" + data + "]";
    }
  }
}
