package org.mobilemcp.ondevice;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The device MCP server and a separately opt-in Ollama model client.
 * No remote connections or device-control actions start automatically.
 */
public final class MainActivity extends Activity {
    private TextView status;
    private TextView settings;
    private TextView modelLog;
    private EditText modelHost;
    private EditText modelName;
    private EditText prompt;
    private Button approve;
    private Button deny;
    private OllamaBridge bridge;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        heading(layout, "Mobile MCP • Android", 22);
        label(layout, "On-device Accessibility MCP server. It can inspect screen content and control taps. " +
                "Start it only when needed. The server uses a secret bearer token and listens on 127.0.0.1 only.");
        status = new TextView(this);
        status.setTextIsSelectable(true);
        layout.addView(status);
        button(layout, "1. Enable Accessibility Service", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(layout, "Choose read/write workspace folder", v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, 41);
        });
        button(layout, "Disconnect workspace folder", v -> {
            WorkspaceTools.forget(this);
            refresh();
        });
        button(layout, "2. Start local MCP server", v -> {
            ControlService svc = ControlService.active();
            if (svc == null) {
                status.setText("Enable the Mobile MCP Accessibility Service first.");
                return;
            }
            try { svc.startServer(); }
            catch (Exception e) { status.setText("Server could not start: " + e.getMessage()); }
            refresh();
        });
        button(layout, "Stop local MCP server", v -> {
            ControlService svc = ControlService.active();
            if (svc != null) svc.stopServer();
            refresh();
        });
        button(layout, "Rotate secret (stops server)", v -> {
            ControlService svc = ControlService.active();
            if (svc != null) svc.stopServer();
            Auth.rotate(this);
            refresh();
        });
        button(layout, "Copy MCP client settings", v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("MCP config", clientConfig()));
            status.setText("Settings copied to clipboard. They contain your secret. Do not post them online.");
        });
        settings = new TextView(this);
        settings.setTextIsSelectable(true);
        layout.addView(settings);

        heading(layout, "Local model ↔ MCP tools", 20);
        label(layout, "Native client based on the Ollama MCP workflow. No Python, pip, Node, cloud login, " +
                "or API key is needed in this APK. An Ollama model SERVER is still required on your phone, " +
                "tablet or another computer. This is NOT a bundled model runtime.");
        label(layout, "The model server receives your messages and any approved tool results. " +
                "Unencrypted HTTP is permitted only for local/private network addresses; use trusted Wi-Fi. " +
                "Model tool requests are NOT executed until you approve each one.");

        modelHost = edit(layout, "Ollama server URL", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        modelName = edit(layout, "Ollama model name (must support tool calling)", InputType.TYPE_CLASS_TEXT);
        modelHost.setText(getPreferences(MODE_PRIVATE).getString("ollama_host", "http://127.0.0.1:11434"));
        modelName.setText(getPreferences(MODE_PRIVATE).getString("ollama_model", ""));

        bridge = new OllamaBridge(this, (message, waiting, working) -> {
            appendLog(message);
            approve.setEnabled(waiting && !working);
            deny.setEnabled(waiting && !working);
        });
        button(layout, "Check Ollama connection", v -> {
            saveModel();
            bridge.test(modelHost.getText().toString(), modelName.getText().toString());
        });
        prompt = edit(layout, "Ask the model (it may request an MCP tool)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        prompt.setMinLines(3);
        prompt.setGravity(android.view.Gravity.TOP);
        button(layout, "Ask model with MCP tools", v -> {
            saveModel();
            appendLog("You: " + prompt.getText().toString());
            bridge.ask(modelHost.getText().toString(), modelName.getText().toString(), prompt.getText().toString());
        });
        approve = button(layout, "Approve the proposed tool", v -> bridge.approve());
        deny = button(layout, "Deny the proposed tool", v -> bridge.deny());
        approve.setEnabled(false);
        deny.setEnabled(false);
        button(layout, "Clear model conversation", v -> bridge.clear());
        modelLog = new TextView(this);
        modelLog.setTextIsSelectable(true);
        modelLog.setText("Model output and approval requests appear here. Every tool action requires a separate approval.");
        layout.addView(modelLog);

        setContentView(scroll);
        refresh();
    }
    private void heading(LinearLayout parent, String title, int size) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(size);
        t.setPadding(0, 18, 0, 8);
        parent.addView(t);
    }
    private void label(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setPadding(0, 4, 0, 10);
        parent.addView(t);
    }
    private EditText edit(LinearLayout parent, String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        parent.addView(e);
        return e;
    }
    private Button button(LinearLayout parent, String text, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(action);
        parent.addView(b);
        return b;
    }
    private void appendLog(String text) {
        if (modelLog == null) return;
        String original = modelLog.getText().toString();
        if (original.length() > 12000) original = original.substring(original.length() - 9000);
        modelLog.setText(original + "\n\n" + text);
    }
    private void saveModel() {
        getPreferences(MODE_PRIVATE).edit()
                .putString("ollama_host", modelHost.getText().toString().trim())
                .putString("ollama_model", modelName.getText().toString().trim())
                .apply();
    }
    private String clientConfig() {
        return "Endpoint: http://127.0.0.1:8765/mcp\nAuthorization: Bearer " + Auth.get(this) +
                "\nTransport: Streamable HTTP (stateless JSON responses)\n";
    }
    private void refresh() {
        ControlService svc = ControlService.active();
        status.setText("Accessibility: " + (svc == null ? "disabled" : "enabled") +
                "\nLocal MCP: " + (svc != null && svc.isServerRunning() ? "RUNNING" : "stopped") +
                "\nWorkspace read/write: " + (WorkspaceTools.selected(this) == null ? "choose a folder" : "enabled for selected folder"));
        if (settings != null) settings.setText(clientConfig());
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 41 || result != RESULT_OK || data == null || data.getData() == null) return;
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (flags != (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) {
            status.setText("The folder provider must grant both read and write access. Choose another folder.");
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(data.getData(), flags);
            WorkspaceTools.select(this, data.getData());
            refresh();
        } catch (SecurityException e) {
            status.setText("Could not retain folder access. Choose a folder that supports persistent read/write permissions.");
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (status != null) refresh();
    }
}
