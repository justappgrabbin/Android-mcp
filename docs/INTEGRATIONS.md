# Android MCP: supplied integration targets

These nine public repositories serve different roles. **Listed here does not mean
bundled or installed.** The APK is a native Android Accessibility-based MCP server
and now a *small native Ollama client*. No vendored Python/Node runtimes or bulk
copy of unrelated server code have been added.

| Repository | Role in ecosystem | Android APK status / dependency boundary |
| --- | --- | --- |
| [jonigl/mcp-client-for-ollama](https://github.com/jonigl/mcp-client-for-ollama) | Ollama-aware MCP client: tool discovery, model inference, tool calls, agent loop, approvals, multi-server support | **Partially implemented natively** in `OllamaBridge.java`: one local MCP server, one chosen Ollama endpoint/model, manual per-tool approval, bounded tool loop. Original full Python TUI, multi-server hot reload, providers, persistence, prompts/resources are **not** bundled. Requires a running Ollama server and an installed tool-capable model. |
| [mksglu/context-mode](https://github.com/mksglu/context-mode) | Context compression, session continuity, SQLite FTS5 search | **Reference only.** Future Android-native persistence and indexing work; existing app currently keeps its model messages in memory per Activity lifetime. The Node/CLI plugin is not installed. |
| [activepieces/activepieces](https://github.com/activepieces/activepieces) | Workflow automation, connectors, recurring tasks, MCP pieces | **Optional external workflow system.** Not packaged: the full web/server/database stack is not a native Android component. Integration would be through authenticated network APIs/MCP. |
| [mcp-use/mcp-use](https://github.com/mcp-use/mcp-use) | TypeScript MCP apps, interactive views, tool inspectors and client | **Potential build-time/dev tooling**, not included in this Android APK. Native views require separate implementation or compatible hosted app UI. |
| [archestra-ai/archestra](https://github.com/archestra-ai/archestra) | MCP gateway, policy, identity, guardrails, enterprise agent orchestration | **Optional external self-hosted service.** Heavy enterprise runtime, not copied into APK. Evaluate only if needed for multi-user orchestration. |
| [nitrocloudofficial/nitrostack](https://github.com/nitrocloudofficial/nitrostack) | TypeScript MCP server framework | **Development option**, not an Android-native library. Existing Java MCP server remains in place. |
| [dsphper/lanhu-mcp](https://github.com/dsphper/lanhu-mcp) | Lanhu design/requirements MCP, exports and asset linking | **Optional external server.** Needs Python/Chromium and Lanhu credentials; no account or cookie is bundled. |
| [ArcadeAI/arcade-mcp](https://github.com/ArcadeAI/arcade-mcp) | Python tool server framework, authorization and tool evaluations | **Development/tool factory reference**, not bundled. Its cloud authorization is optional and not required for device control. |
| [harness/mcp-server](https://github.com/harness/mcp-server) | Harness deployment/platform operations MCP | **Optional Harness account connector**, not packaged. Harness API credentials needed for that specific service only. |

## Implemented native connection (Android v0.2)

```text
A human enters a question on Android
       |
       v
OllamaBridge.java (HTTP /api/chat; model of user's choice)
       |   model returns tool proposal
       v
Android displays tool name + exact JSON arguments
       |   person approves or denies
       v
LocalMcpServer /mcp (Bearer token; loopback only)
       |
       v
ControlService Accessibility tools
       |
       v
Tool result returns to Ollama; model continues
```

* One model server per active conversation, tool descriptions discovered from local MCP server.
* **Per-tool human confirmation**, including read-only tools: no blind automatic execution.
* No Ollama runtime or weights installed. Model server may be on the same phone or a trusted LAN device; plain HTTP to private addresses exposes traffic to others on that network.
* One local MCP server, no persistence between activity recreation, no SSE, no MCP sessions, no concurrent tool execution, no external MCP connectors.
* Screenshot results can be forwarded to a vision-capable model after explicit approval. They may contain private content.
* The Android client does not reproduce all the features of `ollmcp`, which is a Python program.

## Compatibility warning

Do **not** merge all nine projects into a single APK or add their Python/Node
dependencies blindly. They have materially different roles and runtime needs.
Prioritize a working on-device model/tool loop, then add integrations by protocol
over explicit, authenticated boundaries, keeping build dependencies minimal.
