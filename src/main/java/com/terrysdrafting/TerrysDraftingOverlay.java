package com.terrysdrafting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class TerrysDraftingOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(218, 177, 76);
	private final TerrysDraftingState state;
	private final TerrysDraftingConfig config;

	@Inject
	TerrysDraftingOverlay(TerrysDraftingPlugin plugin, TerrysDraftingState state, TerrysDraftingConfig config)
	{
		super(plugin);
		this.state = state;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ApiModels.OverlayResponse data = state.getOverlay();
		if (!config.showOverlay() || data == null || data.event == null || data.team == null)
		{
			return null;
		}
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(data.event.title == null ? "Terry's Drafting" : data.event.title)
			.color(GOLD)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(data.team.name == null ? "Your team" : data.team.name)
			.right("#" + data.team.rank + " · " + data.team.score + " pts")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Sync")
			.right(state.getQueuedCount() == 0 ? "Up to date" : state.getQueuedCount() + " queued")
			.rightColor(state.getQueuedCount() == 0 ? Color.GREEN : Color.YELLOW)
			.build());
		List<ApiModels.BoardTask> open = data.tasks().stream()
			.filter(task -> !task.concealed && !task.freeSpace && !task.claimedByOwnTeam && !task.pendingForOwnTeam)
			.sorted(Comparator.comparingInt(task -> task.sortOrder))
			.limit(3)
			.collect(Collectors.toList());
		for (ApiModels.BoardTask task : open)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(shorten(task.title, 24))
				.right(task.points == null ? "" : task.points + "")
				.build());
		}
		return super.render(graphics);
	}

	private static String shorten(String value, int maximum)
	{
		if (value == null)
		{
			return "Open task";
		}
		return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
	}
}
