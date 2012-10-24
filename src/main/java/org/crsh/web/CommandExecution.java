package org.crsh.web;

import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.Chunk;
import org.crsh.text.Style;
import org.crsh.text.Style.Composite;
import org.crsh.text.Text;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
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
	final JsonArray json;

	/** . */
	private Style.Composite style;

	CommandExecution(ShellProcess process, int width)
	{
		this.process = process;
		this.width = width;
		this.json = new JsonArray();
		this.style = Style.style();
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

	/**
	 * @see org.crsh.Pipe#provide(java.lang.Object)
	 */
	@Override
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
	}

	public JsonArray getDisplay()
	{
		return json;
	}

	/**
	 * @see java.io.Flushable#flush()
	 */
	@Override
	public void flush() throws IOException
	{
	}

	/**
	 * @see org.crsh.InteractionContext#getHeight()
	 */
	@Override
	public int getHeight()
	{
		return 0;
	}
}
