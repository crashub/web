package org.crsh.web;

import org.crsh.shell.impl.command.CRaSH;
import org.crsh.shell.impl.command.CRaSHSession;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import java.io.Serializable;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class CRaSHServletSession implements Serializable, HttpSessionActivationListener {

  /** . */
  public CRaSHSession session;

  public CRaSHServletSession(ServletContext context) {
    this.session = session;
  }

  public CRaSHServletSession() {
  }

  public void sessionWillPassivate(HttpSessionEvent se) {
    session = null;
  }

  public void sessionDidActivate(HttpSessionEvent se) {
    ServletContext context = se.getSession().getServletContext();
    CRaSH crash = (CRaSH)context.getAttribute("crash");
    this.session = crash.createSession(null);
  }
}
