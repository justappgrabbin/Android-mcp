import contextlib
import importlib.util
import io
import json
import os
import unittest
from unittest.mock import patch
from pathlib import Path

spec = importlib.util.spec_from_file_location("bridge", Path(__file__).with_name("openhands_mcp_bridge.py"))
bridge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bridge)


class BridgeTests(unittest.TestCase):
    def test_authenticated_request_and_response(self):
        response = io.BytesIO(b'{"jsonrpc":"2.0","id":7,"result":{"tools":[]}}')
        class Opener:
            def open(self, request, timeout):
                self_request = json.loads(request.data)
                self_test.assertEqual(self_request["method"], "tools/list")
                self_test.assertEqual(request.get_header("Authorization"), "Bearer private-test-token")
                return response
        self_test = self
        with patch.object(bridge.urllib.request, "build_opener", return_value=Opener()):
            result = bridge.forward({"id": 7, "method": "tools/list"}, "http://127.0.0.1:8765/mcp", "private-test-token")
        self.assertEqual(result["result"], {"tools": []})

    def test_stdio_notification_and_error_keep_request_ids(self):
        lines = '\n'.join(json.dumps(item) for item in [
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {"jsonrpc": "2.0", "id": 9, "method": "tools/list"}])
        output = io.StringIO()
        with patch.dict(os.environ, {"ANDROID_MCP_TOKEN": "private-test-token"}), \
                patch.object(bridge.sys, "stdin", io.StringIO(lines)), \
                patch.object(bridge, "forward", side_effect=[None, RuntimeError("private-test-token")]), \
                contextlib.redirect_stdout(output):
            self.assertEqual(bridge.main(), 0)
        result = json.loads(output.getvalue())
        self.assertEqual(result["id"], 9)
        self.assertEqual(result["error"]["code"], -32603)
        self.assertNotIn("private-test-token", output.getvalue())

    def test_missing_secret_does_not_start(self):
        output = io.StringIO()
        with patch.dict(os.environ, {}, clear=True), contextlib.redirect_stderr(output):
            self.assertEqual(bridge.main(), 1)

    def test_redirect_rejected(self):
        with self.assertRaises(ValueError):
            bridge.NoRedirect().redirect_request(None, None, 302, "", {}, "https://example.com")


if __name__ == "__main__":
    unittest.main()
