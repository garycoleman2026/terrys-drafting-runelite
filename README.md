# Terry's Drafting for RuneLite

The opt-in RuneLite companion for [Terry's Drafting](https://draftsmith-teams.companyscreeninginfo.chatgpt.site/runelite), an independent OSRS clan bingo and team-drafting service.

## What the beta does

- Pairs one logged-in character to one event with a ten-minute, one-use code.
- Shows the event, team standing, score, and open tasks in a sidebar and compact overlay.
- Sends only observations that match the live event's capture plan: relevant XP/levels, relevant NPC loot, boss kills, collection-log notices, clues, and supported raid completion messages.
- Lets a player submit a non-screenshot tile for organizer review from RuneLite.
- Retries a bounded local queue across restarts without duplicating accepted progress, and clears it if you switch characters.

## Privacy and consent

Sharing is **off by default**. Enabling it displays RuneLite's required third-party-server warning. The plugin never asks for Jagex credentials, never sends raw chat, never sends other players' names, and never uploads unrelated gameplay. Shared encounters use only an anonymous party-size setting. The revocable token is limited to the paired event and character.

Read the full [data disclosure](https://draftsmith-teams.companyscreeninginfo.chatgpt.site/runelite). The service source lives in [garycoleman2026/draftsmith](https://github.com/garycoleman2026/draftsmith).

## Pairing

1. On the event's private team board, issue a RuneLite code beside your character.
2. In RuneLite, enable the plugin's **Share bingo observations** setting after reviewing the warning.
3. Open the Terry's Drafting sidebar, accept the disclosure, paste the code, and pair while logged into that exact character.
4. Use **Disconnect this device** at any time to revoke the credential.

## Local development

The project requires Java 11.

```text
./gradlew test
./gradlew run
```

`./gradlew run` starts RuneLite in developer mode. Sign in through the Jagex Launcher/account flow as usual; never place account credentials in this repository or a command line. Test on a disposable bingo event before using a live clan event.

## Status

This is a source beta. Plugin Hub submission will follow successful in-game testing. Terry's Drafting is not affiliated with or endorsed by RuneLite, Jagex, Wise Old Man, or Old School RuneScape.
