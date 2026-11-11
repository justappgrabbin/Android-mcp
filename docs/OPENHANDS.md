# Use Android MCP tools from OpenHands

OpenHands runs on your computer/server; the Android app provides device and
workspace tools through MCP. OpenHands itself and its coding runtime are not
bundled into the APK. The included bridge adapts MCP stdio to the Android
server's authenticated HTTP transport using only Python 3's standard library.

1. Install the updated APK. Enable its Accessibility service, choose a
   read/write workspace folder, and start the local MCP server.
2. Connect the phone to the computer over USB, enable USB debugging, and accept
   the phone's debugging authorization. Install Android platform tools on the
   computer and run `adb forward tcp:8765 tcp:8765`.
3. On that computer, set `ANDROID_MCP_TOKEN` to the app's displayed bearer token
   in your private process environment. Do not commit it to the repository.
4. In your OpenHands version's MCP server settings, add a **stdio** server:
   command `python3`, arguments
   `["/absolute/path/to/Android-mcp/tools/openhands_mcp_bridge.py"]`.
   Pass `ANDROID_MCP_TOKEN` in that server's environment. The exact settings UI
   and configuration format depend on your OpenHands release.
5. Verify tool discovery, then try `mobile_list_available_devices`,
   `mobile_list_files`, and a test `mobile_write_file` in the selected folder.

The bridge must run in the same network namespace as `adb forward`. If
OpenHands runs inside Docker, run the stdio bridge in an environment that can
reach that host loopback listener. A container's `127.0.0.1` is normally the
container itself. Do not solve this by making the phone MCP server public.

For a quick bridge check, pipe this line to the Python script after forwarding
and setting the token:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"check","version":"1"}}}
```

## GitHub push approval

Phone file permission and GitHub repository permission are separate. Let
OpenHands edit and test a checkout, then review its diff and approve a push in
the host's workflow. For a firm approval boundary, keep GitHub write credentials
and SSH agents outside the agent runtime and perform the push yourself after
review. A prompt instruction alone does not enforce a push-only approval gate
when an agent has unrestricted terminal access and write credentials.

This bridge grants callers holding the token access to all advertised phone
tools. The Android Ollama client's per-call approval dialogs do not apply to
external MCP clients. Configure approvals in OpenHands for phone actions as
needed. End access by stopping the phone server or rotating its secret.

## File tool limits

Only the folder selected through Android's system picker is available. Other
apps' private data, restricted Android folders, and system files remain governed
by Android. Text reads/writes are capped at 256 KiB. List results are capped at
500 entries with a `truncated` flag. Parent directories must exist; create them
with `mobile_create_directory`. File replacement requires `overwrite=true`;
file deletion requires `confirm=true` and cannot delete directories. Text writes
are not atomic and provider failures can leave partial contents; keep backups.
Folder providers may impose additional restrictions or require connectivity.
