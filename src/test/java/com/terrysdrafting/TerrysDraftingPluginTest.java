package com.terrysdrafting;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TerrysDraftingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TerrysDraftingPlugin.class);
		RuneLite.main(args);
	}
}
