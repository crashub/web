package org.crsh.web;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Those tests don't work yet but I leave there for the Arquillian setup that
 * is quite long to do and that works
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
@RunWith(Arquillian.class)
public class ShellServletTestCase {

  @Deployment(testable=false)
  public static WebArchive createDeployment()
  {
    URL descriptor = ShellServletTestCase.class.getResource("web.xml");
    return ShrinkWrap.
      create(WebArchive.class, "simple.war").
      addAsWebInfResource(descriptor, "web.xml").
      addClass(WelcomeServlet.class);
  }

  @ArquillianResource
  protected URL deploymentURL;

  @Test
  public void testSimple() throws Exception {

    URL url = deploymentURL.toURI().resolve("foo").toURL();

    InetAddress addr = InetAddress.getByName(url.getHost());
    Socket socket = new Socket(addr, url.getPort());
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");

    //
    String s = "execute\r\n" + "hello world\r\n" + "bye\r\n";

    writer.write("POST " + url.getPath() + " HTTP/1.1\r\n");
    writer.write("Host: localhost\r\n");
    writer.write("Content-Type: application/octet-stream\r\n");
    writer.write("Content-Length: " + s.getBytes().length + "\r\n");
    writer.write("\r\n");
    writer.flush();

    //
    writer.write(s);
    writer.flush();
    writer.close();

    //
    Thread.sleep(5000);

/*
    System.out.println("Invoking " + url);
    URLConnection conn = url.openConnection();
    ((HttpURLConnection)conn).setRequestMethod("POST");
//    conn.setDoInput(true);
    conn.setDoOutput(true);

//    InputStream in = conn.getInputStream();
    OutputStream out = conn.getOutputStream();

    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
//    Reader reader = new InputStreamReader(in);

    writer.println("execute");
    writer.println("foo bar");
    writer.flush();

    Thread.sleep(500);
    writer.print("close");
    writer.flush();
*/


  }

}
