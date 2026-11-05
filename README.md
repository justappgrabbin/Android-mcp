# Mobile MCP On Device — Android-native experimental port

**Status: Android project source prepared; APK not compiled or tested on a phone in this environment.**

Independent Android-native implementation of a subset of the public [mobile-next/mobile-mcp](https://github.com/mobile-next/mobile-mcp) tool interface. It does **not** package the upstream Node.js server and does **not** implement every upstream command. Upstream project is Apache-2.0; this Android implementation is separately authored; see `NOTICE.md`.

## Why a port is necessary

The upstream repo is a Node.js MCP server that drives attached devices via platform tools, `adb`, MobileCLI, or Xcode. Ordinary on-device Android apps cannot obtain the same ADB/system privileges on themselves. This version uses opt-in Android Accessibility Service for screen reading and gestures. It works against one **local Android device** only; it cannot drive iOS, remote devices, or another phone via USB.

## Build an APK

Android Studio: open this directory, install Android SDK 35 and JDK 17, then Build > Build Bundle(s)/APK(s) > Build APK(s). Output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions: put all project files into a repository, open the **Actions** tab, run **Build Android APK**. Download the artifact `MobileMcpOnDevice-debug-apk`, unzip it, install `app-debug.apk` on your phone. The included workflow uses Gradle 8.9 and Android Gradle Plugin 8.7.3, installed **at build time**. The installed APK does not require Node.js, Termux, or ADB.

## Phone setup

1. Install the built APK by sideloading.
2. Launch **Mobile MCP On Device** and tap **Enable Accessibility Service**. In Android settings, enable **Mobile MCP device controller** after reviewing the security warning.
3. Return to the app and tap **Start local MCP server**.
4. Copy the connection settings from the app. The MCP endpoint is `http://127.0.0.1:8765/mcp` on the same phone and requires `Authorization: Bearer <token>`.
5. Connect a client that supports **Streamable HTTP MCP with JSON responses**, supplying the URL and Authorization header. This is only reachable from the same phone or from a client using a user-configured safe transport/tunnel.
6. The device id is `local-android`. `mobile_list_available_devices` reports it.

Example MCP request from a terminal **on the same phone**, substituting the token visible in the app:

```bash
curl -sS -X POST http://127.0.0.1:8765/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'Authorization: Bearer REPLACE_WITH_YOUR_TOKEN' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","clientInfo":{"name":"test","version":"1"},"capabilities":{}}}'
```

Then try `tools/list` and `tools/call` with `{"name":"mobile_get_screen_size","arguments":{"device":"local-android"}}`.

## Implemented tools

- `mobile_list_available_devices`
- `mobile_get_screen_size`, `mobile_get_orientation`, `mobile_get_foreground_app`
- `mobile_list_elements_on_screen` (text/json, not `raw`)
- `mobile_click_on_screen_at_coordinates` (x/y or ref)
- `mobile_double_tap_on_screen`, `mobile_long_press_on_screen_at_coordinates`
- `mobile_swipe_on_screen`
- `mobile_type_keys` (Accessibility ACTION_SET_TEXT, may replace entire current field; not equivalent to injected key events)
- `mobile_press_button` (BACK, HOME, APP_SWITCH, NOTIFICATIONS only)
- `mobile_open_url` (http/https)
- `mobile_list_apps`, `mobile_launch_app`
- `mobile_take_screenshot` (Android API 30+, screenshots may be blocked for secure windows)

## Not implemented — not silently omitted

- iOS simulators/real devices; Android devices other than the phone running the APK; cloud device fleet.
- ADB-level capabilities: install/uninstall/force-stop apps, clear app data, change GPS location, read full crash logs, device hardware buttons beyond accessible global actions.
- Screen recording, save-to-desktop output paths, clipboard read/write, orientation override, folding, arbitrary deep links.
- `mobile_batch_commands`, raw platform hierarchy, remote device manager.
- Full MCP SDK protocol support (this server implements stateless JSON responses, no SSE subscription endpoint or session resumption).

## Permissions and security

- ACCESSIBILITY is **high-privilege**: it can observe app content and control the phone. Explicitly enabled by the phone owner; service is inert until started from the app.
- INTERNET permission permits the local listening socket. Server binds strictly to `127.0.0.1:8765`, not `0.0.0.0`.
- `QUERY_ALL_PACKAGES` allows installed-app discovery. This is intended for sideloading; Play Store policies might disallow it.
- Bearer token generated using SecureRandom; required on every `/mcp` POST. Rotate token from app; server stops until restarted.
- No telemetry, advertising, remote model call, or external API key. Tool replies are sent only to authenticated MCP clients.
- Do not expose port 8765 to untrusted networks. Do not paste your token into public logs or issue reports.
- Accessibility UI content may contain personal information; MCP agents with a token can read it.
- Android background process restrictions may stop the service or server. This is not a guaranteed always-on daemon.

## Validation status

Source XML and package structure can be validated offline. A build and actual Accessibility/Android device testing have not been performed in the current environment (Android SDK, Gradle, and internet access are unavailable here). This package is **not** a verified release APK. Build may need fixes from Android Studio's compiler, and specific gestures/OS versions require phone testing.
