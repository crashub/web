package org.crsh.web;

import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.CLS;
import org.crsh.text.Chunk;
import org.crsh.text.Style;
import org.crsh.text.Style.Composite;
import org.crsh.text.Text;
import org.crsh.web.ExecuteServlet.Event;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class CommandExecution implements ShellProcessContext
{

	/** . */
	final ShellProcess process;

	/** . */
	final CountDownLatch latch = new CountDownLatch(1);

	/** . */
	final AtomicReference<ShellResponse> shellResponse = new AtomicReference<ShellResponse>();
	
	/** . */
	final int width;

	/** . */
	private final JsonArray json;

	/** . */
	private Style.Composite style;
	
	/** . */
	private final PrintWriter writer;
	
	/** . */
	private Event source;

	CommandExecution(ShellProcess process, int width, PrintWriter writer, Event source)
	{
		this.process = process;
		this.width = width;
		this.json = new JsonArray();
		this.style = Style.style();
		this.writer = writer;
		this.source = source;
	}

	ShellResponse execute()
	{
		process.execute(this);
		try
		{
			latch.await();
		}
		catch (InterruptedException e)
		{
			// Should handle this case
			e.printStackTrace();
		}
		return shellResponse.get();
	}

	public int getWidth()
	{
		return width;
	}

	public String getProperty(String name)
	{
		return null;
	}

	public String readLine(String msg, boolean echo)
	{
		// Cannot be implemented at the moment
		return null;
	}

	public void end(ShellResponse response)
	{
		shellResponse.set(response);
		latch.countDown();
	}

	void cancel()
	{
		process.cancel();
	}

	public void provide(Chunk element) throws IOException
	{
		if (element instanceof Composite)
		{
			style = (Composite)style.merge((Composite)element);
		}
		else if (element instanceof Text)
		{
			Text text = (Text)element;
			if (text.getText().length() > 0)
			{
				JsonObject elt = new JsonObject();
				if (style != null && (style.getBackground() != null || style.getForeground() != null))
				{
					if (style.getForeground() != null)
					{
						elt.addProperty("fg", style.getForeground().name());
					}
					if (style.getBackground() != null)
					{
						elt.addProperty("bg", style.getBackground().name());
					}
				}
				elt.addProperty("text", text.getText().toString());
				json.add(elt);
			}
		} 
		else if (element instanceof CLS) 
		{
			JsonObject elt = new JsonObject();
			elt.addProperty("text", "cls");
			json.add(elt);
		}
	}

	public void flush() throws IOException
	{
		source.data(json);
		String data = new Gson().toJson(source);
		for (String datum : data.split("\r\n|\r|\n")) {
			writer.print("data: ");
			writer.print(datum);
			writer.print("\n");
		}
		writer.print('\n');
		writer.flush();
	}
	

	public int getHeight()
	{
		return 40;
	}
}
