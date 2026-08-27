package com.terrysdrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class CapturePlan
{
	private CapturePlan()
	{
	}

	static boolean matches(List<ApiModels.CaptureRule> rules, String signalType, String target, Integer targetId, String metric)
	{
		return !matching(rules, signalType, target, targetId, metric).isEmpty();
	}

	static List<ApiModels.CaptureRule> matching(
		List<ApiModels.CaptureRule> rules, String signalType, String target, Integer targetId, String metric)
	{
		if (rules == null)
		{
			return Collections.emptyList();
		}
		List<ApiModels.CaptureRule> matches = new ArrayList<>();
		for (ApiModels.CaptureRule rule : rules)
		{
			if (rule == null || !normalize(rule.signalType).equals(normalize(signalType)))
			{
				continue;
			}
			if (rule.targetId != null && targetId != null && !rule.targetId.equals(targetId))
			{
				continue;
			}
			if (rule.targetId != null && targetId == null)
			{
				continue;
			}
			if (!targetMatches(rule.target, target))
			{
				continue;
			}
			if (!metricMatches(rule.metric, metric))
			{
				continue;
			}
			matches.add(rule);
		}
		return matches;
	}

	static List<ApiModels.CaptureRule> byType(List<ApiModels.CaptureRule> rules, String signalType)
	{
		if (rules == null)
		{
			return Collections.emptyList();
		}
		List<ApiModels.CaptureRule> matches = new ArrayList<>();
		for (ApiModels.CaptureRule rule : rules)
		{
			if (rule != null && normalize(rule.signalType).equals(normalize(signalType)))
			{
				matches.add(rule);
			}
		}
		return matches;
	}

	static String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "").trim();
	}

	private static boolean targetMatches(String expected, String actual)
	{
		String left = normalize(expected);
		String right = normalize(actual);
		if (left.isEmpty() || left.equals("anyraid") || left.equals("anyboss") || left.equals("anyitem"))
		{
			return true;
		}
		return !right.isEmpty() && (left.equals(right) || left.contains(right) || right.contains(left));
	}

	private static boolean metricMatches(String expected, String actual)
	{
		String left = normalize(expected);
		String right = normalize(actual);
		return left.isEmpty() || right.isEmpty() || left.equals(right);
	}
}
