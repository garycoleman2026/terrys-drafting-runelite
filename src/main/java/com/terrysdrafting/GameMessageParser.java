package com.terrysdrafting;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GameMessageParser
{
	private static final Pattern COLLECTION_LOG = Pattern.compile(
		"(?i)new item added to your collection log:\\s*(.+?)\\s*[.!]?$"
	);
	private static final Pattern COMBAT_ACHIEVEMENT = Pattern.compile(
		"(?i)(?:combat (?:task|achievement)[^:]*|congratulations[^:]*combat task[^:]*):\\s*(.+?)\\s*[.!]?$"
	);
	private static final Pattern CLUE = Pattern.compile(
		"(?i)you have completed \\d+ (beginner|easy|medium|hard|elite|master) treasure trails?"
	);
	private static final Pattern NAMED_RAID = Pattern.compile(
		"(?i)(theatre of blood(?:[: -]+hard mode)?|tombs of amascut|chambers of xeric(?:[: -]+challenge mode)?) completion time:\\s*(\\d+):(\\d{2})(?:\\.(\\d{1,2}))?"
	);
	private static final Pattern COX_RAID = Pattern.compile(
		"(?i)congratulations\\s*[-–]\\s*your (challenge mode )?raid is complete!.*?duration:\\s*(\\d+):(\\d{2})(?:\\.(\\d{1,2}))?"
	);
	private static final Pattern PET = Pattern.compile(
		"(?i)(you have a funny feeling like you're being followed|you feel something weird sneaking into your backpack)"
	);

	private GameMessageParser()
	{
	}

	static Parsed parse(String raw)
	{
		String message = stripTags(raw).replace('\u00a0', ' ').trim();
		Matcher matcher = COLLECTION_LOG.matcher(message);
		if (matcher.find())
		{
			return new Parsed(Type.COLLECTION_LOG, matcher.group(1).trim(), 1);
		}
		matcher = COMBAT_ACHIEVEMENT.matcher(message);
		if (matcher.find())
		{
			return new Parsed(Type.COMBAT_ACHIEVEMENT, matcher.group(1).trim(), 1);
		}
		matcher = CLUE.matcher(message);
		if (matcher.find())
		{
			return new Parsed(Type.CLUE, matcher.group(1).toLowerCase(Locale.ENGLISH), 1);
		}
		matcher = NAMED_RAID.matcher(message);
		if (matcher.find())
		{
			return new Parsed(Type.RAID_TIME, canonicalRaid(matcher.group(1)), seconds(matcher.group(2), matcher.group(3)), 1);
		}
		matcher = COX_RAID.matcher(message);
		if (matcher.find())
		{
			String target = matcher.group(1) == null ? "Chambers of Xeric" : "Chambers of Xeric: Challenge Mode";
			return new Parsed(Type.RAID_TIME, target, seconds(matcher.group(2), matcher.group(3)), 1);
		}
		if (PET.matcher(message).find())
		{
			return new Parsed(Type.PET, "", 1);
		}
		return null;
	}

	private static int seconds(String minutes, String seconds)
	{
		return Integer.parseInt(minutes) * 60 + Integer.parseInt(seconds);
	}

	private static String canonicalRaid(String value)
	{
		String normalized = value.toLowerCase(Locale.ENGLISH);
		if (normalized.contains("hard mode"))
		{
			return "Theatre of Blood: Hard Mode";
		}
		if (normalized.contains("theatre"))
		{
			return "Theatre of Blood";
		}
		if (normalized.contains("amascut"))
		{
			return "Tombs of Amascut";
		}
		if (normalized.contains("challenge"))
		{
			return "Chambers of Xeric: Challenge Mode";
		}
		return "Chambers of Xeric";
	}

	private static String stripTags(String value)
	{
		return value == null ? "" : value.replaceAll("<[^>]*>", "");
	}

	enum Type
	{
		COLLECTION_LOG,
		COMBAT_ACHIEVEMENT,
		CLUE,
		RAID_TIME,
		PET
	}

	static final class Parsed
	{
		final Type type;
		final String target;
		final int value;
		final int participantCount;

		Parsed(Type type, String target, int value)
		{
			this(type, target, value, 1);
		}

		Parsed(Type type, String target, int value, int participantCount)
		{
			this.type = type;
			this.target = target;
			this.value = value;
			this.participantCount = participantCount;
		}
	}
}
