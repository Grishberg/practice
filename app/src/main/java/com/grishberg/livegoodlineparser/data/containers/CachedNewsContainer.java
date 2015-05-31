package com.grishberg.livegoodlineparser.data.containers;

/**
 * Created by G on 29.05.15.
 */
public class CachedNewsContainer
{
	private String body;	// тело письма
	private boolean isDescription	= false; // признак того, что это описание
	public CachedNewsContainer(String body, boolean isDescription)
	{
		this.body			= body;
		this.isDescription	= isDescription;
	}
	public String getBody(){return body;}
	public boolean getDescriptionStatus() { return isDescription;}
}
