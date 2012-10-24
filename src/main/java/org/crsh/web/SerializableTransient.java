package org.crsh.web;

import java.io.Serializable;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class SerializableTransient<T> implements Serializable
{

	public transient T object;

	public SerializableTransient(T object)
	{
		this.object = object;
	}

	public SerializableTransient()
	{
	}
}
