package org.crsh.web;

import org.crsh.web.servlet.CRaSHConnector;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Julien Viet
 */
class ContextualExecutorService extends ThreadPoolExecutor {

  /** . */
  private final LifeCycle lifeCycle;

  ContextualExecutorService(LifeCycle lifeCycle, int nThreads) {
    super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    //
    this.lifeCycle = lifeCycle;
  }

  @Override
  public void execute(final Runnable command) {
    String sessionId = CRaSHConnector.getHttpSessionId();
    final Session session = lifeCycle.sessions.get(sessionId);
    Runnable wrapper = new Runnable() {
      public void run() {
        lifeCycle.current.set(session);
        try {
          CRaSHSecurityManager.runWithinContext(command);
        }
        finally {
          lifeCycle.current.set(null);
        }
      }
    };
    super.execute(wrapper);
  }
}
