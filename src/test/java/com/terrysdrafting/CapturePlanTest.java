package com.terrysdrafting;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CapturePlanTest
{
	@Test
	public void matchesOnlyRelevantItemAndSkillSignals()
	{
		ApiModels.CaptureRule loot = rule("item_acquired", "Oathplate helm", 30_799, "");
		ApiModels.CaptureRule xp = rule("xp_gain", "", null, "agility");
		assertTrue(CapturePlan.matches(Arrays.asList(loot, xp), "item_acquired", "Oathplate helm", 30_799, ""));
		assertFalse(CapturePlan.matches(Arrays.asList(loot, xp), "item_acquired", "Coins", 995, ""));
		assertTrue(CapturePlan.matches(Arrays.asList(loot, xp), "xp_gain", "", null, "Agility"));
		assertFalse(CapturePlan.matches(Arrays.asList(loot, xp), "xp_gain", "", null, "Mining"));
	}

	@Test
	public void recognizesGenericRaidTargetsWithoutMatchingAnotherSignalType()
	{
		ApiModels.CaptureRule raid = rule("raid_complete", "Any raid", null, "");
		assertTrue(CapturePlan.matches(Arrays.asList(raid), "raid_complete", "Theatre of Blood", null, "trio"));
		assertFalse(CapturePlan.matches(Arrays.asList(raid), "raid_time", "Theatre of Blood", null, "trio"));
	}

	private static ApiModels.CaptureRule rule(String type, String target, Integer targetId, String metric)
	{
		ApiModels.CaptureRule rule = new ApiModels.CaptureRule();
		rule.signalType = type;
		rule.target = target;
		rule.targetId = targetId;
		rule.metric = metric;
		return rule;
	}
}
