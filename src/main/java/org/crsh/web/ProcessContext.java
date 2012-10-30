package org.crsh.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.CLS;
import org.crsh.text.Chunk;
import org.crsh.text.Style;
import org.crsh.text.Text;

import javax.servlet.AsyncContext;
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
  private final JsonArray buffer;

  /** . */
  private Style.Composite style;

  ProcessContext(Connection conn, String line, int width) {
    this.conn = conn;
    this.line = line;
    this.width = width;
    this.height = 40;
    this.buffer = new JsonArray();
    this.style = Style.style();
  }

  void begin() {
    System.out.println("BEGINNING " + line);
    process = conn.session.createProcess(line);
    process.execute(this);
    System.out.println("BEGAN " + line);
  }

  public void end(ShellResponse response) {
    conn.current = null;
    System.out.println("TERMINATED " + line);
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
    if (element instanceof Style.Composite) {
      style = (Style.Composite)style.merge((Style.Composite)element);
    }
    else if (element instanceof Text) {
      Text text = (Text)element;
      if (text.getText().length() > 0) {
        JsonObject elt = new JsonObject();
        if (style != null && (style.getBackground() != null || style.getForeground() != null)) {
          if (style.getForeground() != null) {
            elt.addProperty("fg", style.getForeground().name());
          }
          if (style.getBackground() != null) {
            elt.addProperty("bg", style.getBackground().name());
          }
        }
        elt.addProperty("text", text.getText().toString());
        buffer.add(elt);
      }
    }
    else if (element instanceof CLS) {
      JsonObject elt = new JsonObject();
      elt.addProperty("text", "cls");
      buffer.add(elt);
    }
  }

  public void flush() throws IOException {
    ProcessServlet.Event event = new ProcessServlet.Event("message");
    event.data(buffer);
    event.socket(conn.id);
    String data = new Gson().toJson(event);
    System.out.println("SENDING DATA " + data);
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
