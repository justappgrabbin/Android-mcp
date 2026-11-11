#!/usr/bin/env python3
"""Dependency-free MCP stdio bridge for OpenHands and other MCP clients.

Run on the host that can reach the phone's forwarded loopback endpoint.
Secrets are read from environment variables and never printed.
"""
import json
import os
import sys
import urllib.error
import urllib.request


def forward(message, endpoint, token):
    request = urllib.request.Request(
        endpoint, data=json.dumps(message).encode("utf-8"),
        headers={"Content-Type": "application/json",
                 "Accept": "application/json, text/event-stream",
                 "Authorization": "Bearer " + token}, method="POST")
    # Do not send the phone token through proxy settings inherited from the host.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}),
                                        NoRedirect())
    with opener.open(request, timeout=45) as response:
        body = response.read(16 * 1024 * 1024 + 1)
        if len(body) > 16 * 1024 * 1024:
            raise ValueError("MCP response exceeds bridge limit")
        return json.loads(body) if body else None


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("MCP endpoint redirects are not allowed")


def main():
    endpoint = "http://127.0.0.1:8765/mcp"
    token = os.environ.get("ANDROID_MCP_TOKEN", "")
    if not token:
        print("Set ANDROID_MCP_TOKEN to the secret displayed by the Android app.", file=sys.stderr)
        return 1
    for line in sys.stdin:
        message = None
        try:
            message = json.loads(line)
            result = forward(message, endpoint, token)
            if "id" in message and result is not None:
                print(json.dumps(result), flush=True)
        except Exception:
            # Avoid logging exception text, request bodies, or credentials.
            if isinstance(message, dict) and "id" in message:
                print(json.dumps({"jsonrpc": "2.0", "id": message["id"],
                                  "error": {"code": -32603, "message":
                                            "Phone MCP request failed. Check forwarding, server and token."}}), flush=True)
            else:
                print("MCP notification or input failed.", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
