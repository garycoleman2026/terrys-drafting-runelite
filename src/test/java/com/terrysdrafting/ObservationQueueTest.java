package com.terrysdrafting;

import java.util.Map;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

	@Test
	public void restoresPendingObservationsWithStableIds()
	{
		ObservationQueue firstRun = new ObservationQueue();
		Map<String, Object> observation = firstRun.create("clue_complete", 1_900_000_000_000L);
		observation.put("target", "elite");
		firstRun.add(observation);
		List<Map<String, Object>> saved = firstRun.snapshot();

		ObservationQueue restarted = new ObservationQueue();
		restarted.restore(saved);
		assertEquals(1, restarted.size());
		assertEquals(firstRun.peekBatch().batchKey, restarted.peekBatch().batchKey);
		assertEquals(firstRun.peekBatch().observations, restarted.peekBatch().observations);
	}

	@Test
	public void keepsQueuesBoundToOneCharacter()
	{
		assertTrue(ObservationQueue.sameOwner("Terry Name", "terry_name"));
		assertFalse(ObservationQueue.sameOwner("Terry Name", "Other Terry"));
	}
}
