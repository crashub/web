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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Julien Viet
 */
@WebFilter(urlPatterns = "/*")
public class FilterImpl implements Filter {

  /** . */
  private FilterConfig config;

  public void init(FilterConfig filterConfig) throws ServletException {
    this.config = filterConfig;
  }

  public void doFilter(ServletRequest servletReq, ServletResponse servletResp, FilterChain chain) throws IOException, ServletException {

    //
    LifeCycle lifeCycle = LifeCycle.getLifeCycle(config.getServletContext());

    //
    HttpServletRequest req = (HttpServletRequest)servletReq;
    HttpServletResponse resp = (HttpServletResponse)servletResp;

    //
    String sessionId = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("CRASHID")) {
          sessionId = cookie.getValue();
          break;
        }
      }
    }
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      Cookie cookie = new Cookie("CRASHID", sessionId);
      cookie.setPath("/");
      resp.addCookie(cookie);
    }

    // Associate the CRASHID with an HttpSession as the HttpSession will expire
    // and garbage our Session
    HttpSession httpSession = req.getSession(false);
    if (httpSession == null) {
      httpSession = req.getSession();
      httpSession.setAttribute("CRASHID", sessionId);
    }

    //
    Session session = lifeCycle.sessions.get(sessionId);
    if (session == null) {
      session = new Session(lifeCycle.crash);
      lifeCycle.sessions.put(sessionId, session);
    }

    //
    lifeCycle.current.set(session);

    //
    try {
      chain.doFilter(req, servletResp);
    }
    finally {
      lifeCycle.current.set(null);
    }
  }

  public void destroy() {
  }
}
