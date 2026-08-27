package com.terrysdrafting;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class ObservationQueueTest
{
	@Test
	public void keepsStableIdsUntilTheBatchIsAcknowledged()
	{
		ObservationQueue queue = new ObservationQueue();
		Map<String, Object> first = queue.create("xp_delta", 1_900_000_000_000L);
		first.put("metric", "Agility");
		first.put("value", 100);
		queue.add(first);

		ObservationQueue.Batch firstAttempt = queue.peekBatch();
		ObservationQueue.Batch retry = queue.peekBatch();
		assertNotNull(firstAttempt);
		assertEquals(firstAttempt.batchKey, retry.batchKey);
		assertEquals(firstAttempt.observations, retry.observations);

		queue.acknowledge(firstAttempt);
		assertEquals(0, queue.size());
	}

	@Test
	public void createsUniqueClientEventIds()
	{
		ObservationQueue queue = new ObservationQueue();
		String first = String.valueOf(queue.create("xp_delta", 1).get("clientEventId"));
		String second = String.valueOf(queue.create("xp_delta", 2).get("clientEventId"));
		assertNotEquals(first, second);
	}
}
