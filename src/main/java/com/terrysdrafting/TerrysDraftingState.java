package com.terrysdrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

final class TerrysDraftingState
{
	private ApiModels.OverlayResponse overlay;
	private String currentRsn = "";
	private String status = "Not paired";
	private String error = "";
	private String lastServerCheck = "Board check: never";
	private String lastSignal = "No gameplay signals yet";
	private int queuedCount;
	private Runnable listener;

	synchronized ApiModels.OverlayResponse getOverlay()
	{
		return overlay;
	}

	synchronized List<ApiModels.CaptureRule> getCapturePlan()
	{
		return overlay == null ? Collections.emptyList() : new ArrayList<>(overlay.captureRules());
	}

	synchronized String getCurrentRsn()
	{
		return currentRsn;
	}

	synchronized String getStatus()
	{
		return status;
	}

	synchronized String getError()
	{
		return error;
	}

	synchronized int getQueuedCount()
	{
		return queuedCount;
	}

	synchronized String getLastServerCheck()
	{
		return lastServerCheck;
	}

	synchronized String getLastSignal()
	{
		return lastSignal;
	}

	synchronized void setListener(Runnable listener)
	{
		this.listener = listener;
	}

	void setCurrentRsn(String value)
	{
		synchronized (this)
		{
			currentRsn = value == null ? "" : value;
		}
		notifyListener();
	}

	void setOverlay(ApiModels.OverlayResponse value)
	{
		synchronized (this)
		{
			overlay = value;
			status = value == null ? "Paired — waiting for event" : "Connected";
			error = "";
		}
		notifyListener();
	}

	void setStatus(String value)
	{
		synchronized (this)
		{
			status = value;
			error = "";
		}
		notifyListener();
	}

	void setError(String value)
	{
		synchronized (this)
		{
			error = value == null ? "" : value;
			status = "Needs attention";
		}
		notifyListener();
	}

	void setQueuedCount(int value)
	{
		synchronized (this)
		{
			queuedCount = Math.max(0, value);
		}
		notifyListener();
	}

	void setLastServerCheck(String value)
	{
		synchronized (this)
		{
			lastServerCheck = value == null || value.trim().isEmpty() ? "Board check: never" : value;
		}
		notifyListener();
	}

	void setLastSignal(String value)
	{
		synchronized (this)
		{
			lastSignal = value == null || value.trim().isEmpty() ? "No gameplay signals yet" : value;
		}
		notifyListener();
	}

	void clear()
	{
		synchronized (this)
		{
			overlay = null;
			status = "Not paired";
			error = "";
			queuedCount = 0;
			lastServerCheck = "Board check: never";
			lastSignal = "No gameplay signals yet";
		}
		notifyListener();
	}

	private void notifyListener()
	{
		final Runnable current;
		synchronized (this)
		{
			current = listener;
		}
		if (current != null)
		{
			SwingUtilities.invokeLater(current);
		}
	}
}
