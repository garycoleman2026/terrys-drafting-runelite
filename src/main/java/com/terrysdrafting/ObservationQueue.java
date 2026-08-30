package com.terrysdrafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ObservationQueue
{
	private static final int MAX_QUEUE_SIZE = 250;
	private static final int MAX_BATCH_SIZE = 25;
	private final Deque<Map<String, Object>> queue = new ArrayDeque<>();
	private final String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	private long sequence;

	synchronized Map<String, Object> create(String type, long observedAtMillis)
	{
		Map<String, Object> observation = new LinkedHashMap<>();
		observation.put("type", type);
		observation.put("clientEventId", sessionId + ":" + (++sequence));
		observation.put("observedAt", java.time.Instant.ofEpochMilli(observedAtMillis).toString());
		return observation;
	}

	synchronized void add(Map<String, Object> observation)
	{
		if (queue.size() == MAX_QUEUE_SIZE)
		{
			queue.removeFirst();
		}
		queue.addLast(new LinkedHashMap<>(observation));
	}

	synchronized Batch peekBatch()
	{
		if (queue.isEmpty())
		{
			return null;
		}
		List<Map<String, Object>> observations = new ArrayList<>();
		for (Map<String, Object> observation : queue)
		{
			observations.add(new LinkedHashMap<>(observation));
			if (observations.size() == MAX_BATCH_SIZE)
			{
				break;
			}
		}
		String firstId = String.valueOf(observations.get(0).get("clientEventId"));
		return new Batch("batch:" + firstId, observations);
	}

	synchronized void acknowledge(Batch batch)
	{
		for (Map<String, Object> expected : batch.observations)
		{
			Map<String, Object> current = queue.peekFirst();
			if (current == null || !expected.get("clientEventId").equals(current.get("clientEventId")))
			{
				return;
			}
			queue.removeFirst();
		}
	}

	synchronized int size()
	{
		return queue.size();
	}

	synchronized List<Map<String, Object>> snapshot()
	{
		List<Map<String, Object>> copy = new ArrayList<>();
		for (Map<String, Object> observation : queue)
		{
			copy.add(new LinkedHashMap<>(observation));
		}
		return copy;
	}

	synchronized void restore(List<Map<String, Object>> observations)
	{
		queue.clear();
		if (observations == null)
		{
			return;
		}
		for (Map<String, Object> observation : observations)
		{
			if (observation == null || !observation.containsKey("clientEventId")
				|| !observation.containsKey("type") || !observation.containsKey("observedAt"))
			{
				continue;
			}
			add(observation);
		}
	}

	synchronized void clear()
	{
		queue.clear();
	}

	static boolean sameOwner(String left, String right)
	{
		return CapturePlan.normalize(left).equals(CapturePlan.normalize(right));
	}

	static final class Batch
	{
		final String batchKey;
		final List<Map<String, Object>> observations;

		Batch(String batchKey, List<Map<String, Object>> observations)
		{
			this.batchKey = batchKey;
			this.observations = observations;
		}
	}
}
