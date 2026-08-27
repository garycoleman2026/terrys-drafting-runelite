package com.terrysdrafting;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Terry's Drafting",
	description = "Pair an OSRS clan bingo, view team progress, and submit opt-in task observations",
	tags = {"bingo", "clan", "teams", "tracker"}
)
public class TerrysDraftingPlugin extends Plugin
{
	static final String VERSION = "0.1.0";
	private static final long BATCH_INTERVAL_MILLIS = 10_000L;
	private static final long DEFAULT_POLL_INTERVAL_MILLIS = 5_000L;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private TerrysDraftingConfig config;

	@Inject
	private TerrysDraftingApiClient apiClient;

	private final TerrysDraftingState state = new TerrysDraftingState();
	private final ObservationQueue observationQueue = new ObservationQueue();
	private final Map<Skill, Integer> experience = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private TerrysDraftingPanel panel;
	private TerrysDraftingOverlay overlay;
	private NavigationButton navigationButton;
	private String etag = "";
	private long lastPollAt;
	private long lastBatchAt;
	private boolean pollInFlight;
	private boolean batchInFlight;
	private boolean started;

	@Override
	protected void startUp()
	{
		started = true;
		panel = new TerrysDraftingPanel(this, state, config);
		overlay = new TerrysDraftingOverlay(this, state, config);
		state.setListener(panel::refresh);
		navigationButton = NavigationButton.builder()
			.tooltip("Terry's Drafting")
			.icon(createNavigationIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		if (hasCredential())
		{
			state.setStatus(config.enableSharing() ? "Paired — waiting to sync" : "Paired — sharing paused");
		}
		log.debug("Terry's Drafting started");
	}

	@Override
	protected void shutDown()
	{
		started = false;
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		if (overlay != null)
		{
			overlayManager.remove(overlay);
		}
		state.setListener(null);
		observationQueue.clear();
		experience.clear();
		levels.clear();
		log.debug("Terry's Drafting stopped");
	}

	@Provides
	TerrysDraftingConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(TerrysDraftingConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			initializeSession();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			experience.clear();
			levels.clear();
			state.setCurrentRsn("");
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (state.getCurrentRsn().isEmpty())
		{
			initializeSession();
		}
		if (!config.enableSharing() || !hasCredential())
		{
			return;
		}
		long now = System.currentTimeMillis();
		ApiModels.OverlayResponse current = state.getOverlay();
		long pollInterval = current == null
			? DEFAULT_POLL_INTERVAL_MILLIS
			: Math.max(5_000L, Math.min(60_000L, current.pollAfterSeconds * 1_000L));
		if (!pollInFlight && now - lastPollAt >= pollInterval)
		{
			refreshOverlay();
		}
		if (!batchInFlight && observationQueue.size() > 0 && now - lastBatchAt >= BATCH_INTERVAL_MILLIS)
		{
			flushObservations();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!canCapture() || event.getSkill() == Skill.OVERALL)
		{
			return;
		}
		Skill skill = event.getSkill();
		Integer previousXp = experience.put(skill, event.getXp());
		Integer previousLevel = levels.put(skill, event.getLevel());
		List<ApiModels.CaptureRule> plan = state.getCapturePlan();
		if (previousXp != null && event.getXp() > previousXp
			&& CapturePlan.matches(plan, "xp_gain", "", null, skill.getName()))
		{
			Map<String, Object> observation = observationQueue.create("xp_delta", System.currentTimeMillis());
			observation.put("metric", skill.getName());
			observation.put("value", event.getXp() - previousXp);
			queue(observation);
		}
		if (previousLevel != null && event.getLevel() > previousLevel
			&& CapturePlan.matches(plan, "level_reached", "", null, skill.getName()))
		{
			Map<String, Object> observation = observationQueue.create("level_reached", System.currentTimeMillis());
			observation.put("metric", skill.getName());
			observation.put("value", event.getLevel());
			queue(observation);
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (!canCapture())
		{
			return;
		}
		long observedAt = System.currentTimeMillis();
		String npcName = event.getNpc() == null || event.getNpc().getName() == null ? "" : event.getNpc().getName();
		List<ApiModels.CaptureRule> plan = state.getCapturePlan();
		if (!npcName.isEmpty() && CapturePlan.matches(plan, "boss_kc", npcName, null, npcName))
		{
			Map<String, Object> observation = observationQueue.create("boss_kill", observedAt);
			observation.put("target", npcName);
			observation.put("metric", npcName);
			observation.put("value", 1);
			observation.put("participantCount", config.partySize());
			observation.put("correlationId", correlation("boss", npcName, 0, observedAt));
			queue(observation);
		}
		for (ItemStack stack : event.getItems())
		{
			ItemComposition composition = itemManager.getItemComposition(stack.getId());
			String itemName = composition == null || composition.getName() == null ? "" : composition.getName();
			if (CapturePlan.matches(plan, "item_acquired", itemName, stack.getId(), ""))
			{
				Map<String, Object> observation = observationQueue.create("item_drop", observedAt);
				observation.put("target", itemName);
				observation.put("targetId", stack.getId());
				observation.put("value", Math.max(1, stack.getQuantity()));
				queue(observation);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!canCapture() || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM))
		{
			return;
		}
		GameMessageParser.Parsed parsed = GameMessageParser.parse(event.getMessage());
		if (parsed == null)
		{
			return;
		}
		long observedAt = System.currentTimeMillis();
		List<ApiModels.CaptureRule> plan = state.getCapturePlan();
		switch (parsed.type)
		{
			case COLLECTION_LOG:
				queueSimpleIfMatched(plan, "collection_log", "collection_log", parsed.target, null, "", 1, observedAt);
				break;
			case COMBAT_ACHIEVEMENT:
				queueSimpleIfMatched(plan, "combat_achievement", "combat_achievement", parsed.target, null, "", 1, observedAt);
				break;
			case CLUE:
				queueSimpleIfMatched(plan, "clue_complete", "clue_complete", parsed.target, null, parsed.target, 1, observedAt);
				break;
			case PET:
				queueUnambiguousPet(plan, observedAt);
				break;
			case RAID_TIME:
				queueRaid(plan, parsed.target, parsed.value, observedAt);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!TerrysDraftingConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (!config.enableSharing())
		{
			observationQueue.clear();
			state.setQueuedCount(0);
			state.setStatus(hasCredential() ? "Paired — sharing paused" : "Not paired");
		}
		else if (hasCredential())
		{
			lastPollAt = 0;
			state.setStatus("Paired — waiting to sync");
		}
	}

	void pair(String code, boolean disclosureAccepted)
	{
		if (!config.enableSharing())
		{
			state.setError("Enable Share bingo observations in the plugin settings first.");
			return;
		}
		String rsn = state.getCurrentRsn();
		if (rsn.isEmpty())
		{
			state.setError("Log into the character named on the pairing code first.");
			return;
		}
		if (!disclosureAccepted)
		{
			state.setError("Review and accept the data disclosure before pairing.");
			return;
		}
		state.setStatus("Pairing…");
		apiClient.pair(code, rsn, (value, error, unauthorized) ->
		{
			if (!started)
			{
				return;
			}
			if (error != null || value == null || value.credential == null)
			{
				state.setError(error == null ? "The pairing response was incomplete." : error);
				return;
			}
			configManager.setConfiguration(TerrysDraftingConfig.GROUP, TerrysDraftingConfig.CREDENTIAL_KEY, value.credential);
			etag = "";
			lastPollAt = 0;
			state.setStatus("Paired — loading event");
			refreshOverlay();
		});
	}

	void refreshOverlay()
	{
		if (!started || !config.enableSharing() || !hasCredential() || pollInFlight)
		{
			return;
		}
		pollInFlight = true;
		lastPollAt = System.currentTimeMillis();
		apiClient.overlay(config.deviceCredential(), etag, (value, error, unauthorized) ->
		{
			pollInFlight = false;
			if (!started)
			{
				return;
			}
			if (unauthorized)
			{
				clearCredential("This device was disconnected. Pair it again to continue.");
				return;
			}
			if (error != null)
			{
				state.setError(error);
				return;
			}
			if (value != null)
			{
				etag = value.etag == null ? "" : value.etag;
				if (value.value != null)
				{
					state.setOverlay(value.value);
				}
				else
				{
					state.setStatus("Connected");
				}
			}
		});
	}

	void submitClaim(ApiModels.BoardTask task, String note)
	{
		if (task == null || !task.pluginClaimable)
		{
			state.setError("Choose a claimable non-screenshot task.");
			return;
		}
		if (!canCapture())
		{
			state.setError("Connect the paired character while the bingo is live.");
			return;
		}
		state.setStatus("Submitting claim…");
		apiClient.submitClaim(config.deviceCredential(), state.getCurrentRsn(), task.id, note, (value, error, unauthorized) ->
		{
			if (unauthorized)
			{
				clearCredential("This device was disconnected. Pair it again to continue.");
				return;
			}
			if (error != null)
			{
				state.setError(error);
				return;
			}
			state.setStatus("Claim submitted for organizer review");
			etag = "";
			lastPollAt = 0;
			refreshOverlay();
		});
	}

	void disconnect()
	{
		if (!hasCredential())
		{
			clearCredential("Not paired");
			return;
		}
		state.setStatus("Disconnecting…");
		apiClient.disconnect(config.deviceCredential(), (value, error, unauthorized) ->
		{
			if (error != null && !unauthorized)
			{
				state.setError(error);
				return;
			}
			clearCredential("Device disconnected");
		});
	}

	boolean hasCredential()
	{
		return config.deviceCredential() != null && !config.deviceCredential().trim().isEmpty();
	}

	private void initializeSession()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getName() == null)
		{
			return;
		}
		state.setCurrentRsn(local.getName());
		experience.clear();
		levels.clear();
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				experience.put(skill, client.getSkillExperience(skill));
				levels.put(skill, client.getRealSkillLevel(skill));
			}
		}
		lastPollAt = 0;
	}

	private boolean canCapture()
	{
		if (!config.enableSharing() || !hasCredential() || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		ApiModels.OverlayResponse current = state.getOverlay();
		return current != null && current.event != null && "live".equalsIgnoreCase(current.event.status)
			&& current.member != null && sameRsn(current.member.name, state.getCurrentRsn());
	}

	private void queueSimpleIfMatched(
		List<ApiModels.CaptureRule> plan, String signalType, String observationType, String target,
		Integer targetId, String metric, int value, long observedAt)
	{
		if (!CapturePlan.matches(plan, signalType, target, targetId, metric))
		{
			return;
		}
		Map<String, Object> observation = observationQueue.create(observationType, observedAt);
		observation.put("target", target);
		if (targetId != null)
		{
			observation.put("targetId", targetId);
		}
		if (metric != null && !metric.isEmpty())
		{
			observation.put("metric", metric);
		}
		observation.put("value", value);
		queue(observation);
	}

	private void queueUnambiguousPet(List<ApiModels.CaptureRule> plan, long observedAt)
	{
		List<ApiModels.CaptureRule> pets = CapturePlan.byType(plan, "pet_obtained");
		if (pets.size() != 1 || pets.get(0).target == null || pets.get(0).target.isEmpty())
		{
			return;
		}
		ApiModels.CaptureRule pet = pets.get(0);
		queueSimpleIfMatched(plan, "pet_obtained", "pet_drop", pet.target, pet.targetId, pet.metric, 1, observedAt);
	}

	private void queueRaid(List<ApiModels.CaptureRule> plan, String parsedTarget, int durationSeconds, long observedAt)
	{
		Set<String> emitted = new HashSet<>();
		for (ApiModels.CaptureRule rule : CapturePlan.matching(plan, "raid_time", parsedTarget, null, ""))
		{
			String target = rule.target == null || rule.target.isEmpty() ? parsedTarget : rule.target;
			String key = "time:" + CapturePlan.normalize(target) + ":" + CapturePlan.normalize(rule.metric);
			if (!emitted.add(key))
			{
				continue;
			}
			Map<String, Object> observation = observationQueue.create("raid_time", observedAt);
			observation.put("target", target);
			observation.put("metric", rule.metric == null ? "" : rule.metric);
			observation.put("value", durationSeconds);
			observation.put("participantCount", config.partySize());
			observation.put("correlationId", correlation("raidtime", target, durationSeconds, observedAt));
			queue(observation);
		}
		for (ApiModels.CaptureRule rule : CapturePlan.matching(plan, "raid_complete", parsedTarget, null, ""))
		{
			String target = rule.target == null || rule.target.isEmpty() ? parsedTarget : rule.target;
			String key = "complete:" + CapturePlan.normalize(target) + ":" + CapturePlan.normalize(rule.metric);
			if (!emitted.add(key))
			{
				continue;
			}
			Map<String, Object> observation = observationQueue.create("raid_complete", observedAt);
			observation.put("target", target);
			observation.put("metric", rule.metric == null ? "" : rule.metric);
			observation.put("value", 1);
			observation.put("participantCount", config.partySize());
			observation.put("correlationId", correlation("raidclear", target, durationSeconds, observedAt));
			queue(observation);
		}
	}

	private void queue(Map<String, Object> observation)
	{
		observationQueue.add(observation);
		state.setQueuedCount(observationQueue.size());
		if (observationQueue.size() >= 25 && !batchInFlight)
		{
			flushObservations();
		}
	}

	private void flushObservations()
	{
		if (!canCapture() || batchInFlight)
		{
			return;
		}
		ObservationQueue.Batch batch = observationQueue.peekBatch();
		if (batch == null)
		{
			return;
		}
		batchInFlight = true;
		lastBatchAt = System.currentTimeMillis();
		apiClient.submitBatch(config.deviceCredential(), state.getCurrentRsn(), batch, (value, error, unauthorized) ->
		{
			batchInFlight = false;
			if (unauthorized)
			{
				clearCredential("This device was disconnected. Pair it again to continue.");
				return;
			}
			if (error != null)
			{
				state.setError(error + " Queued observations will retry.");
				return;
			}
			observationQueue.acknowledge(batch);
			state.setQueuedCount(observationQueue.size());
			state.setStatus(observationQueue.size() == 0 ? "Connected — synced" : "Connected — syncing");
			etag = "";
			lastPollAt = 0;
		});
	}

	private void clearCredential(String status)
	{
		configManager.unsetConfiguration(TerrysDraftingConfig.GROUP, TerrysDraftingConfig.CREDENTIAL_KEY);
		observationQueue.clear();
		etag = "";
		state.clear();
		state.setStatus(status);
	}

	private String correlation(String kind, String target, int value, long observedAt)
	{
		String normalized = CapturePlan.normalize(target);
		if (normalized.length() > 24)
		{
			normalized = normalized.substring(0, 24);
		}
		return kind + "." + normalized + "." + value + "." + (observedAt / 10_000L) + "." + config.partySize();
	}

	private static boolean sameRsn(String left, String right)
	{
		return CapturePlan.normalize(left).equals(CapturePlan.normalize(right));
	}

	private static BufferedImage createNavigationIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(38, 28, 12));
		graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
		graphics.setColor(new Color(211, 164, 58));
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawLine(4, 4, 12, 4);
		graphics.drawLine(8, 4, 8, 12);
		graphics.drawLine(5, 12, 11, 12);
		graphics.dispose();
		return image;
	}
}
