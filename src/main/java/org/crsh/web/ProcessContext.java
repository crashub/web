package org.crsh.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.CLS;
import org.crsh.text.Chunk;
import org.crsh.text.Style;
import org.crsh.text.Text;

import java.io.IOException;
import java.io.PrintWriter;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class ProcessContext implements ShellProcessContext {

  /** . */
  private final Connection conn;

  /** . */
  private final String line;

  /** . */
  private final int width;

  /** . */
  private final int height;

  /** . */
  private ShellProcess process;

  /** . */
  private JsonArray buffer;

  /** . */
  private Style style;

  /** . */
  private boolean useAlternate;

  ProcessContext(Connection conn, String line, int width) {
    this.conn = conn;
    this.line = line;
    this.width = width;
    this.height = 34;
    this.buffer = null;
    this.style = Style.style();
    this.useAlternate = false;
  }

  public boolean takeAlternateBuffer() {
    if (!useAlternate) {
      if (buffer == null) {
        buffer = new JsonArray();
      }
      JsonObject elt = new JsonObject();
      elt.addProperty("type", "takeAlternate");
      buffer.add(elt);
    }
    useAlternate = true;
    return true;
  }

  public boolean releaseAlternateBuffer() {
    if (useAlternate) {
      if (buffer == null) {
        buffer = new JsonArray();
      }
      JsonObject elt = new JsonObject();
      elt.addProperty("type", "releaseAlternate");
      buffer.add(elt);
    }
    useAlternate = false;
    return true;
  }

  void begin() {
    System.out.println("Executing " + line);
    process = conn.shell.createProcess(line);
    process.execute(this);
  }

  void cancel() {
    System.out.println("Cancelling " + line);
    process.cancel();
  }

  public void end(ShellResponse response) {
    conn.current = null;
    System.out.println("Terminated " + line + " with " + response);
    try {
      conn.context.getResponse().getWriter().close();
    }
    catch (IOException ignore) {
    }
    finally {
      conn.context.complete();
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getProperty(String propertyName) {
    return null;
  }

  public String readLine(String msg, boolean echo) {
    return null;
  }

  public void provide(Chunk element) throws IOException {

    // TODO : handle Color.def

    if (element instanceof Style) {
      style = style.merge((Style)element);
    } else {
      JsonObject elt;
      if (element instanceof Text) {
        Text text = (Text)element;
        if (text.getText().length() > 0) {
          elt = new JsonObject();
          elt.addProperty("type", "text");
          if (style instanceof Style.Composite) {
            Style.Composite composite = (Style.Composite)style;
            if (composite.getForeground() != null) {
              elt.addProperty("fg", composite.getForeground().name());
            }
            if (composite.getBackground() != null) {
              elt.addProperty("bg", composite.getBackground().name());
            }
          }
          elt.addProperty("text", text.getText().toString());
        } else {
          elt = null;
        }
      } else if (element instanceof CLS) {
        elt = new JsonObject();
        elt.addProperty("type", "cls");
      } else {
        elt = null;
      }
      if (elt != null) {
        if (buffer == null) {
          buffer = new JsonArray();
        }
        buffer.add(elt);
      }
    }
  }

  public void flush() throws IOException {
    if (buffer != null && buffer.size() > 0) {
      JsonArray tmp = buffer;
      buffer = null;
      ExecuteServlet.Event event = new ExecuteServlet.Event("message");
      event.data(tmp);
      event.socket(conn.id);
      String data = new Gson().toJson(event);
      System.out.println("Sending data to " + conn.id);
      PrintWriter writer = conn.context.getResponse().getWriter();
      for (String datum : data.split("\r\n|\r|\n")) {
        writer.print("data: ");
        writer.print(datum);
        writer.print("\n");
      }
      writer.print('\n');
      writer.flush();
    }
  }
}
