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
import org.crsh.util.Safe;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class ProcessContext implements ShellProcessContext {

  /** . */
  private static final Logger log = Logger.getLogger(ProcessContext.class.getSimpleName());

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

  /** . */
  private final String remoteHost;

  ProcessContext(String remoteHost, Connection conn, String line, int width, int height) {
    this.remoteHost = remoteHost;
    this.conn = conn;
    this.line = line;
    this.width = width;
    this.height = height;
    this.buffer = null;
    this.style = Style.style();
    this.useAlternate = false;
  }

  public Class<Chunk> getConsumedType() {
    return Chunk.class;
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
    log.log(Level.INFO, remoteHost + " executing " + line);
    process = conn.shell.createProcess(line);
    process.execute(this);
  }

  void cancel() {
    log.log(Level.INFO, remoteHost + " cancelling " + line);
    process.cancel();
  }

  public void end(ShellResponse response) {

    // Send last message
    String msg = response.getMessage();
    if (msg.length() > 0) {
      provide(Text.create(msg));
      Safe.flush(this);
    }

    //
    conn.current = null;
    log.log(Level.INFO, remoteHost + " terminated " + line + " with " + response);
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

  public void provide(Chunk element) {

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
      // System.out.println("Sending data to " + conn.id);
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
