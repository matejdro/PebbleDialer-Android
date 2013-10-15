package com.matejdro.pebbledialer;

public class PebbleUtil {	
	public static String replaceInvalidCharacters(String data)
	{
		data = data.replace("è", "c");
		data = data.replace("È", "C");
		
		return data;
	}
	
	public static String prepareString(String text)
	{
		return prepareString(text, 20);
	}
	
	public static String prepareString(String text, int length)
	{
		if (text == null)
			return null;
	
		int targetLength = length - 3;
		
		text = text.trim();
		if (text.getBytes().length > length)
		{
			if (text.length() > targetLength)
				text = text.substring(0, targetLength).trim();
			
			while (text.getBytes().length > targetLength)
			{
				text = text.substring(0, text.length() - 1);
			}
			
			text = text + "...";

		}
		text = replaceInvalidCharacters(text);		
		
		return text;

	}
}
