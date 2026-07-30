# Repo Rules

## External directory: C:\Dev\PPNAM-Station-4

This is the sibling WPF/Core/CLI repo for PPNAM Station 4 (not this Android app). It is **read-only**
reference material — never edit or write to any file under it. The normative MQTT wire contract this
Android app implements lives at `C:\Dev\PPNAM-Station-4\DOCS\Station4_Wastage_MQTT_Contract.md`.

As of the contract's "Implementation status" note, the Station4 WPF/Core consumer in that repo is
**not yet schema v2 compliant** — it only accepts the legacy 7-field payload. This Android app
publishes schema v2 (`schemaVersion: 2`, adds `machineOperatorUserId`) because that is the target
contract, but per the contract's own rollout order, a v2 publisher must not go live against the
current legacy consumer until Station4 is upgraded — that upgrade is out of scope for this repo.

## Operator login is mirrored from Station 2 AA, not Station 4's own contract

`Station4_Wastage_MQTT_Contract.md` defines no login/auth mechanism at all — it covers one
publish-only event topic (`station4/waste/collection`). At the user's explicit request, the
handheld's login (SCRAM-SHA-256 challenge/proof + RFID badge, `PPNAM/{deviceId}/req|res/...`
request/response) is ported from `C:\Dev\PPNAM_Station_2_AA`'s real, working login mechanism —
see `data/mqtt/MqttTopics.kt`'s class doc. There is no evidence Station 4's actual backend
(`C:\Dev\PPNAM-Station-4`) implements a matching MQTT auth service: its only login today is
`PPNAM.Station4.Core/Services/AuthenticationService.cs`, a local SQL Server username/password
check inside the WPF desktop app, unreachable from this handheld. So this login flow is
speculative/forward-looking the same way schema v2 publishing was before Station 4 supported it —
tested on-device against no live auth backend, it correctly fails with "Not connected to the
broker" / a request timeout rather than crashing, but nothing will actually authenticate until a
matching backend exists.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
