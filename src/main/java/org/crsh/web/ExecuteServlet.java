package org.crsh.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.crsh.shell.Shell;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellResponse;

import com.google.gson.Gson;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
//@WebServlet(urlPatterns = "/execute", asyncSupported = true)
public class ExecuteServlet extends HttpServlet
{

	/** . */ 
	private final Map<String, AsyncContext> connections = new ConcurrentHashMap<String, AsyncContext>();

	/** . */
	private final AtomicReference<HttpSession> currentSession = new AtomicReference<HttpSession>();

	/** . */
	private final BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();

	/** . */
	private final Executor executor = Executors.newSingleThreadExecutor();

	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		request.setCharacterEncoding("utf-8");
		response.setHeader("Access-Control-Allow-Origin", "*");

		String data = request.getReader().readLine();
		if (data != null) {
			data = data.substring("data=".length());
		}

		Event event = new Gson().fromJson(data, Event.class);
		if("message".equals(event.type))
		{
			queue.offer(event);
			currentSession.set(request.getSession());
		} 
		else if("close".equals(event.type))
		{
			connections.remove(event.socket);
		}

		executor.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					Event event = queue.take();
					Map<String, Object> map = (Map<String, Object>) event.data;
					String line = (String)map.get("line");
					Double widthP = (Double)map.get("width");
					int width = 80;
					if (widthP != null)
					{
						width = widthP.intValue();
					}

					HttpSession session = currentSession.get();
					SerializableTransient<Shell> crash = (SerializableTransient<Shell>)session.getAttribute("crash");

					for(Map.Entry<String, AsyncContext> entry : connections.entrySet())
					{
						AsyncContext context = entry.getValue();
						try
						{
							PrintWriter writer = context.getResponse().getWriter();
							Shell shell = crash.object;
							ShellProcess process = shell.createProcess(line);
							CommandExecution execution = new CommandExecution(process, width, writer, event);
							session.setAttribute("execution", new SerializableTransient<CommandExecution>(execution));
							ShellResponse response = execution.execute();
							if(response != null)
							{
								event.data = "it's done";
								String data = new Gson().toJson(event);
								for (String datum : data.split("\r\n|\r|\n")) {
									writer.print("data: ");
									writer.print(datum);
									writer.print("\n");
								}
								writer.print('\n');
								writer.flush();
								context.complete();
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						finally
						{
							session.removeAttribute("execution");
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		final String id = request.getParameter("id");
		String transport = request.getParameter("transport");
		AsyncContext context = request.startAsync();
		context.setTimeout(300 * 1000L); // 5 minutes

		response.setCharacterEncoding("utf-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("text/" + ("sse".equals(transport) ? "event-stream" : "plain"));

		PrintWriter writer = response.getWriter();
		for (int i = 0; i < 2000; i++) {
			writer.print(' ');
		}
		writer.print("\n");
		writer.flush();

		context.addListener(new AsyncListener()
		{
			public void onTimeout(AsyncEvent event) throws IOException
			{
				connections.remove(id);
			}

			public void onStartAsync(AsyncEvent event) throws IOException
			{
        System.out.println("SHOULD START");
			}
			public void onError(AsyncEvent event) throws IOException
			{
				connections.remove(id);	
			}
			public void onComplete(AsyncEvent event) throws IOException
			{
				connections.remove(id);	
			}
		});

		connections.put(id, context);
		currentSession.set(request.getSession());
	}

	static class Event 
	{
		/** . */
		private String type;

		/** . */
		private String socket;

		/** . */
		private Object data;
		
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
