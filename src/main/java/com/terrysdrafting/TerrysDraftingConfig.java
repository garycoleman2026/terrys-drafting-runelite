package com.terrysdrafting;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(TerrysDraftingConfig.GROUP)
public interface TerrysDraftingConfig extends Config
{
	String GROUP = "terrysdrafting";
	String CREDENTIAL_KEY = "deviceCredential";
	String PENDING_QUEUE_KEY = "pendingObservationQueue";
	String PENDING_RSN_KEY = "pendingObservationRsn";

	@ConfigItem(
		keyName = "enableSharing",
		name = "Share bingo observations",
		description = "Allow task-relevant observations to be sent while paired",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean enableSharing()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show bingo overlay",
		description = "Show your team score and a few open tasks on the game canvas"
	)
	default boolean showOverlay()
	{
		return true;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "partySize",
		name = "Current party size",
		description = "Anonymous party size used only for team boss and raid rules"
	)
	default int partySize()
	{
		return 1;
	}

	@ConfigItem(
		keyName = CREDENTIAL_KEY,
		name = "Paired device credential",
		description = "Revocable Terry's Drafting event credential",
		hidden = true,
		secret = true
	)
	default String deviceCredential()
	{
		return "";
	}

	@ConfigItem(
		keyName = PENDING_QUEUE_KEY,
		name = "Pending observation queue",
		description = "Locally retained observations waiting to retry",
		hidden = true
	)
	default String pendingObservationQueue()
	{
		return "";
	}

	@ConfigItem(
		keyName = PENDING_RSN_KEY,
		name = "Pending queue character",
		description = "Character that owns the local retry queue",
		hidden = true
	)
	default String pendingObservationRsn()
	{
		return "";
	}
}
