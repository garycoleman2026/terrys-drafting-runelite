package com.terrysdrafting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GameMessageParserTest
{
	@Test
	public void parsesCollectionLogWithoutKeepingMarkup()
	{
		GameMessageParser.Parsed parsed = GameMessageParser.parse(
			"New item added to your collection log: <col=ef1020>Oathplate helm</col>");
		assertEquals(GameMessageParser.Type.COLLECTION_LOG, parsed.type);
		assertEquals("Oathplate helm", parsed.target);
	}

	@Test
	public void parsesNamedAndChallengeModeRaidTimes()
	{
		GameMessageParser.Parsed tob = GameMessageParser.parse("Theatre of Blood completion time: 12:34. Personal best: 12:40");
		assertEquals("Theatre of Blood", tob.target);
		assertEquals(754, tob.value);
		GameMessageParser.Parsed cm = GameMessageParser.parse(
			"Congratulations - your Challenge Mode raid is complete! Duration: 24:05. Personal best: 23:59");
		assertEquals("Chambers of Xeric: Challenge Mode", cm.target);
		assertEquals(1_445, cm.value);
	}

	@Test
	public void ignoresOrdinaryChat()
	{
		assertNull(GameMessageParser.parse("Hello clan, good luck at bingo!"));
		assertNull(GameMessageParser.parse("Combat task completed: Perfect Theatre"));
	}
}
