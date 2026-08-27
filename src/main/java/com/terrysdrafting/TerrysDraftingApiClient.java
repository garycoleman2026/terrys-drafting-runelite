package com.terrysdrafting;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class TerrysDraftingApiClient
{
	static final String SERVICE_ORIGIN = "https://draftsmith-teams.companyscreeninginfo.chatgpt.site";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	TerrysDraftingApiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	void pair(String code, String rsn, Result<ApiModels.PairResponse> callback)
	{
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("rsn", rsn);
		body.put("deviceName", "RuneLite desktop");
		body.put("pluginVersion", TerrysDraftingPlugin.VERSION);
		body.put("scopes", Arrays.asList("xp", "loot", "kills", "raids", "achievements"));
		body.put("consent", true);
		body.put("disclosureVersion", 1);
		execute(jsonRequest("/api/runelite/pair", "", rsn, body), ApiModels.PairResponse.class, callback);
	}

	void overlay(String credential, String etag, Result<OverlayResult> callback)
	{
		Request.Builder builder = requestBuilder("/api/runelite/overlay", credential, "");
		if (etag != null && !etag.isEmpty())
		{
			builder.header("If-None-Match", etag);
		}
		httpClient.newCall(builder.get().build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				callback.complete(null, readableFailure(exception), false);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closed = response)
				{
					if (closed.code() == 304)
					{
						callback.complete(new OverlayResult(null, etag), null, false);
						return;
					}
					if (!closed.isSuccessful())
					{
						callback.complete(null, errorMessage(closed), closed.code() == 401);
						return;
					}
					ApiModels.OverlayResponse value = read(closed, ApiModels.OverlayResponse.class);
					callback.complete(new OverlayResult(value, closed.header("ETag", "")), null, false);
				}
				catch (RuntimeException exception)
				{
					callback.complete(null, "The bingo response could not be read.", false);
				}
			}
		});
	}

	void submitBatch(
		String credential, String rsn, ObservationQueue.Batch batch, Result<Void> callback)
	{
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("batchKey", batch.batchKey);
		body.put("observations", batch.observations);
		execute(jsonRequest("/api/runelite/events", credential, rsn, body), Void.class, callback);
	}

	void submitClaim(String credential, String rsn, String taskId, String note, Result<ApiModels.ClaimResponse> callback)
	{
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("taskId", taskId);
		body.put("note", note == null ? "" : note);
		execute(jsonRequest("/api/runelite/claims", credential, rsn, body), ApiModels.ClaimResponse.class, callback);
	}

	void disconnect(String credential, Result<Void> callback)
	{
		Request request = requestBuilder("/api/runelite/device", credential, "").delete().build();
		execute(request, Void.class, callback);
	}

	private Request jsonRequest(String path, String credential, String rsn, Object value)
	{
		return requestBuilder(path, credential, rsn)
			.post(RequestBody.create(JSON, gson.toJson(value)))
			.build();
	}

	private Request.Builder requestBuilder(String path, String credential, String rsn)
	{
		Request.Builder builder = new Request.Builder()
			.url(SERVICE_ORIGIN + path)
			.header("Accept", "application/json")
			.header("User-Agent", "Terrys-Drafting-RuneLite/" + TerrysDraftingPlugin.VERSION);
		if (credential != null && !credential.isEmpty())
		{
			builder.header("Authorization", "Bearer " + credential);
		}
		if (rsn != null && !rsn.isEmpty())
		{
			builder.header("X-RuneLite-RSN", rsn);
		}
		return builder;
	}

	private <T> void execute(Request request, Class<T> responseType, Result<T> callback)
	{
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				callback.complete(null, readableFailure(exception), false);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closed = response)
				{
					if (!closed.isSuccessful())
					{
						callback.complete(null, errorMessage(closed), closed.code() == 401);
						return;
					}
					T value = responseType == Void.class ? null : read(closed, responseType);
					callback.complete(value, null, false);
				}
				catch (RuntimeException exception)
				{
					callback.complete(null, "The bingo response could not be read.", false);
				}
			}
		});
	}

	private <T> T read(Response response, Class<T> type)
	{
		ResponseBody body = response.body();
		if (body == null)
		{
			throw new IllegalStateException("Missing response body");
		}
		try
		{
			return gson.fromJson(body.string(), type);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException(exception);
		}
	}

	private String errorMessage(Response response)
	{
		try
		{
			ResponseBody body = response.body();
			if (body != null)
			{
				ApiModels.ErrorResponse parsed = gson.fromJson(body.string(), ApiModels.ErrorResponse.class);
				if (parsed != null && parsed.error != null && !parsed.error.trim().isEmpty())
				{
					return parsed.error;
				}
			}
		}
		catch (IOException | RuntimeException ignored)
		{
			// Fall through to a bounded generic message; response content is never logged.
		}
		return "Terry's Drafting returned HTTP " + response.code() + ".";
	}

	private static String readableFailure(IOException exception)
	{
		return exception.getMessage() == null ? "Terry's Drafting could not be reached." : "Terry's Drafting could not be reached.";
	}

	interface Result<T>
	{
		void complete(T value, String error, boolean unauthorized);
	}

	static final class OverlayResult
	{
		final ApiModels.OverlayResponse value;
		final String etag;

		OverlayResult(ApiModels.OverlayResponse value, String etag)
		{
			this.value = value;
			this.etag = etag;
		}
	}
}
