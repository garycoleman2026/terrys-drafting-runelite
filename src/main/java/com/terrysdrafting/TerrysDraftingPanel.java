package com.terrysdrafting;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

final class TerrysDraftingPanel extends PluginPanel
{
	private static final Color GOLD = new Color(218, 177, 76);
	private final TerrysDraftingPlugin plugin;
	private final TerrysDraftingState state;
	private final TerrysDraftingConfig config;
	private final JLabel sharingLabel = new JLabel();
	private final JLabel characterLabel = new JLabel();
	private final JLabel eventLabel = new JLabel();
	private final JLabel teamLabel = new JLabel();
	private final JLabel scoreLabel = new JLabel();
	private final JLabel queueLabel = new JLabel();
	private final JTextArea statusArea = textArea();
	private final JTextField codeField = new JTextField();
	private final JCheckBox disclosure = new JCheckBox("I accept the data disclosure");
	private final JButton pairButton = new JButton("Pair character");
	private final JButton refreshButton = new JButton("Refresh");
	private final JButton openBoardButton = new JButton("Open public board");
	private final JComboBox<ApiModels.BoardTask> taskBox = new JComboBox<>();
	private final JTextArea noteArea = textArea();
	private final JButton claimButton = new JButton("Submit for organizer review");
	private final JButton disconnectButton = new JButton("Disconnect this device");

	TerrysDraftingPanel(TerrysDraftingPlugin plugin, TerrysDraftingState state, TerrysDraftingConfig config)
	{
		this.plugin = plugin;
		this.state = state;
		this.config = config;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Terry's Drafting", SwingConstants.LEFT);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(GOLD);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(title);
		add(label("OSRS clan bingo companion", ColorScheme.LIGHT_GRAY_COLOR));
		add(Box.createVerticalStrut(10));

		add(card("Connection", sharingLabel, characterLabel, statusArea, queueLabel));
		add(Box.createVerticalStrut(8));

		codeField.setToolTipText("XXXX-XXXX-XXXX");
		codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		disclosure.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		disclosure.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		pairButton.addActionListener(event -> plugin.pair(codeField.getText().trim(), disclosure.isSelected()));
		JPanel pairing = verticalCard("Pairing code");
		pairing.add(codeField);
		pairing.add(Box.createVerticalStrut(6));
		pairing.add(disclosure);
		pairing.add(Box.createVerticalStrut(6));
		pairing.add(pairButton);
		pairing.add(Box.createVerticalStrut(6));
		JButton privacyButton = new JButton("Read privacy details");
		privacyButton.addActionListener(event -> LinkBrowser.browse(TerrysDraftingApiClient.SERVICE_ORIGIN + "/runelite"));
		pairing.add(privacyButton);
		add(pairing);
		add(Box.createVerticalStrut(8));

		JPanel eventCard = verticalCard("Live event");
		eventCard.add(eventLabel);
		eventCard.add(teamLabel);
		eventCard.add(scoreLabel);
		eventCard.add(Box.createVerticalStrut(6));
		JPanel eventActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		eventActions.setOpaque(false);
		refreshButton.addActionListener(event -> plugin.refreshOverlay());
		openBoardButton.addActionListener(event -> openBoard());
		eventActions.add(refreshButton);
		eventActions.add(openBoardButton);
		eventCard.add(eventActions);
		add(eventCard);
		add(Box.createVerticalStrut(8));

		taskBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		noteArea.setRows(3);
		noteArea.setEditable(true);
		noteArea.setOpaque(true);
		noteArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
		noteArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		noteArea.setLineWrap(true);
		noteArea.setWrapStyleWord(true);
		noteArea.setToolTipText("Optional note for the organizer");
		claimButton.addActionListener(event -> plugin.submitClaim((ApiModels.BoardTask) taskBox.getSelectedItem(), noteArea.getText()));
		JPanel claimCard = verticalCard("Manual claim");
		claimCard.add(label("Use this when the tile needs organizer judgment.", ColorScheme.LIGHT_GRAY_COLOR));
		claimCard.add(Box.createVerticalStrut(5));
		claimCard.add(taskBox);
		claimCard.add(Box.createVerticalStrut(5));
		claimCard.add(new JScrollPane(noteArea));
		claimCard.add(Box.createVerticalStrut(6));
		claimCard.add(claimButton);
		add(claimCard);
		add(Box.createVerticalStrut(8));

		disconnectButton.addActionListener(event -> plugin.disconnect());
		disconnectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(disconnectButton);
		add(Box.createVerticalGlue());
		add(label("Independent community tool · not affiliated with Jagex or RuneLite", ColorScheme.LIGHT_GRAY_COLOR));
		refresh();
	}

	void refresh()
	{
		ApiModels.OverlayResponse data = state.getOverlay();
		sharingLabel.setText(config.enableSharing() ? "Sharing: enabled" : "Sharing: off in settings");
		sharingLabel.setForeground(config.enableSharing() ? Color.GREEN : Color.ORANGE);
		characterLabel.setText("Logged in: " + (state.getCurrentRsn().isEmpty() ? "—" : state.getCurrentRsn()));
		String status = state.getError().isEmpty() ? state.getStatus() : state.getError();
		statusArea.setText(status);
		statusArea.setForeground(state.getError().isEmpty() ? ColorScheme.LIGHT_GRAY_COLOR : new Color(255, 145, 120));
		queueLabel.setText("Retry queue: " + state.getQueuedCount());
		boolean paired = plugin.hasCredential();
		pairButton.setEnabled(config.enableSharing() && !paired);
		codeField.setEnabled(!paired);
		disclosure.setEnabled(!paired);
		disconnectButton.setEnabled(paired);
		refreshButton.setEnabled(paired && config.enableSharing());
		if (data == null || data.event == null || data.team == null)
		{
			eventLabel.setText("Event: —");
			teamLabel.setText("Team: —");
			scoreLabel.setText("Standing: —");
			openBoardButton.setEnabled(false);
			setTasks(java.util.Collections.emptyList());
			return;
		}
		eventLabel.setText("Event: " + safe(data.event.title));
		teamLabel.setText("Team: " + safe(data.team.name));
		scoreLabel.setText("Standing: #" + data.team.rank + " · " + data.team.score + " points");
		openBoardButton.setEnabled(data.event.publicUrl != null && !data.event.publicUrl.isEmpty());
		List<ApiModels.BoardTask> claimable = data.tasks().stream()
			.filter(task -> task.pluginClaimable)
			.collect(Collectors.toList());
		setTasks(claimable);
	}

	private void setTasks(List<ApiModels.BoardTask> tasks)
	{
		ApiModels.BoardTask selected = (ApiModels.BoardTask) taskBox.getSelectedItem();
		DefaultComboBoxModel<ApiModels.BoardTask> model = new DefaultComboBoxModel<>();
		for (ApiModels.BoardTask task : tasks)
		{
			model.addElement(task);
		}
		taskBox.setModel(model);
		if (selected != null)
		{
			for (int index = 0; index < model.getSize(); index++)
			{
				if (selected.id.equals(model.getElementAt(index).id))
				{
					taskBox.setSelectedIndex(index);
					break;
				}
			}
		}
		claimButton.setEnabled(model.getSize() > 0 && config.enableSharing());
	}

	private void openBoard()
	{
		ApiModels.OverlayResponse data = state.getOverlay();
		if (data != null && data.event != null && data.event.publicUrl != null)
		{
			LinkBrowser.browse(data.event.publicUrl);
		}
	}

	private static JPanel card(String heading, Component... components)
	{
		JPanel card = verticalCard(heading);
		for (Component component : components)
		{
			card.add(component);
		}
		return card;
	}

	private static JPanel verticalCard(String heading)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
		JLabel label = label(heading, GOLD);
		label.setFont(FontManager.getDefaultBoldFont());
		card.add(label);
		card.add(Box.createVerticalStrut(6));
		return card;
	}

	private static JLabel label(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static JTextArea textArea()
	{
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setOpaque(false);
		area.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setAlignmentX(Component.LEFT_ALIGNMENT);
		return area;
	}

	private static String safe(String value)
	{
		return value == null || value.trim().isEmpty() ? "—" : value;
	}
}
