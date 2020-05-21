package translatoid;

import java.util.ArrayList;

public class ScriptParser
{
	private static final StringBuilder stringBuilder = new StringBuilder();

	public static String stripComments(String totalFile)
	{
		stringBuilder.setLength(0);
		stringBuilder.append(totalFile);
		int end = stringBuilder.lastIndexOf("*/");
		while (end != -1)
		{
			int start = stringBuilder.lastIndexOf("/*", end - 1);
			if (start == -1)
			{
				break;
			}

			int innerCommentEnd = stringBuilder.lastIndexOf("*/", end - 1);
			while (innerCommentEnd > start)
			{
				int innerCommentStart = start;
//				String comment = buf2.substring(innerCommentStart, innerCommentEnd + 2);
				start = stringBuilder.lastIndexOf("/*", start - 2);
				if (start == -1)
				{
					break;
				}
				innerCommentEnd = stringBuilder.lastIndexOf("*/", innerCommentStart - 2);
			}
			if (start == -1)
			{
				break;
			}

//			String comment = buf2.substring(start, end + 2);
			stringBuilder.replace(start, end + 2, "");
			end = stringBuilder.lastIndexOf("*/", start);
		}
		totalFile = stringBuilder.toString();
		stringBuilder.setLength(0);
		return totalFile;
	}

	public static ArrayList<String> parseTokens(String totalFile)
	{
		final ArrayList<String> Tokens = new ArrayList<>();

		for (;;)
		{
			int depth = 0;
			int nextindexOfOpen = 0;
			int nextindexOfClosed = 0;

			if (totalFile.indexOf("}", nextindexOfOpen + 1) == -1)
			{
				break;
			}

			do
			{
				nextindexOfOpen = totalFile.indexOf("{", nextindexOfOpen + 1);
				nextindexOfClosed = totalFile.indexOf("}", nextindexOfClosed + 1);
				if (((nextindexOfClosed < nextindexOfOpen) && (nextindexOfClosed != -1)) || (nextindexOfOpen == -1))
				{
					nextindexOfOpen = nextindexOfClosed;
					depth--;
				}
				else
				{
					nextindexOfClosed = nextindexOfOpen;
					depth++;
				}
			}
			while (depth > 0);

			Tokens.add(totalFile.substring(0, nextindexOfOpen + 1).trim());
			totalFile = totalFile.substring(nextindexOfOpen + 1);
		}

		if (totalFile.trim().length() > 0)
		{
			Tokens.add(totalFile.trim());
		}

		return Tokens;
	}
}