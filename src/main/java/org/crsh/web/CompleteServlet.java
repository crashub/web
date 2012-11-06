package org.crsh.web;

import com.google.gson.Gson;
import org.crsh.cmdline.CommandCompletion;
import org.crsh.cmdline.Delimiter;
import org.crsh.cmdline.spi.Completion;
import org.crsh.shell.Shell;
import org.crsh.util.Strings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/complete")
public class CompleteServlet extends HttpServlet {

  /** . */
  static final Gson gson = new Gson();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Shell shell = ((SerializableTransient<Shell>)req.getSession().getAttribute("crash")).object;
    String prefix = req.getParameter("prefix");
    CommandCompletion completion = shell.complete(prefix);
    Completion completions = completion.getValue();
    List<String> values = new ArrayList<String>();
    if (completions.getSize() == 0) {
      // Do nothing
    }
    else {
      Delimiter delimiter = completion.getDelimiter();
      StringBuilder sb = new StringBuilder();
      if (completions.getSize() == 1) {
        String value = completions.getValues().iterator().next();
        delimiter.escape(value, sb);
        if ((Boolean)completions.get(value)) {
          sb.append(delimiter.getValue());
        }
        values.add(sb.toString());
      }
      else {
        String commonCompletion = Strings.findLongestCommonPrefix(completions.getValues());
        if (commonCompletion.length() > 0) {
          delimiter.escape(commonCompletion, sb);
          values.add(sb.toString());
        }
        else {
          String completionPrefix = completions.getPrefix();
          for (Map.Entry<String, Boolean> entry : completions) {
            sb.append(completionPrefix);
            delimiter.escape(entry.getKey(), sb);
            values.add(sb.toString());
            sb.setLength(0);
          }
        }
      }
    }
    resp.getWriter().write(gson.toJson(values));
  }
}
