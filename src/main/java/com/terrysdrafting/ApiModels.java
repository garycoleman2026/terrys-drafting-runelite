package com.terrysdrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ApiModels
{
	private ApiModels()
	{
	}

	static final class PairResponse
	{
		String credential;
		Event event;
		Team team;
		Member member;
	}

	static final class OverlayResponse
	{
		int schemaVersion;
		int pollAfterSeconds = 5;
		Event event;
		Team team;
		Member member;
		List<Team> standings = new ArrayList<>();
		List<BoardTask> board = new ArrayList<>();
		List<CaptureRule> capturePlan = new ArrayList<>();

		List<BoardTask> tasks()
		{
			return board == null ? Collections.emptyList() : board;
		}

		List<CaptureRule> captureRules()
		{
			return capturePlan == null ? Collections.emptyList() : capturePlan;
		}
	}

	static final class Event
	{
		String id;
		String title;
		String status;
		String publicUrl;
		int revision;
	}

	static final class Team
	{
		String id;
		String name;
		String color;
		int score;
		int rank;
		int completedCount;
		int lineCount;
	}

	static final class Member
	{
		String id;
		String name;
	}

	static final class BoardTask
	{
		String id;
		String title;
		String category;
		int sortOrder;
		Integer points;
		boolean concealed;
		boolean freeSpace;
		boolean claimed;
		boolean claimedByOwnTeam;
		boolean pendingForOwnTeam;
		boolean claimable;
		boolean pluginClaimable;
		String claimBlockedReason;
		Progress progress;

		@Override
		public String toString()
		{
			return title == null ? "Bingo task" : title;
		}
	}

	static final class Progress
	{
		String status;
		double value;
		double target;
		String confidence;
	}

	static final class CaptureRule
	{
		String taskId;
		String signalType;
		String target;
		Integer targetId;
		String metric;
	}

	static final class ClaimResponse
	{
		String id;
		String status;
		String taskId;
		String submittedAt;
	}

	static final class BatchResponse
	{
		int eventCount;
		int matchedCount;
		int ignoredCount;
		int rejectedCount;
		int duplicateCount;
		int scoredCount;
		int reviewCount;
		String message;
		List<ObservationResult> results = new ArrayList<>();

		List<ObservationResult> observationResults()
		{
			return results == null ? Collections.emptyList() : results;
		}
	}

	static final class ObservationResult
	{
		String status;
		String message;
		String label;
	}

	static final class DiagnosticResponse
	{
		String status;
		String message;
		String eventStatus;
		int captureRuleCount;
		String serverTime;
	}

	static final class ErrorResponse
	{
		String error;
	}
}
