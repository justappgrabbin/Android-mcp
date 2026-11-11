package org.mobilemcp.ondevice;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** On-device MCP tool server for external agents. Starts only on owner request. */
public final class MainActivity extends Activity {
    private TextView status;
    private TextView settings;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        heading(layout, "Mobile MCP • Android", 22);
        label(layout, "Your MCP server runs inside this app, on this phone. " +
                "Enable Accessibility, then start the server. No model setup is needed to use MCP tools.");
        status = new TextView(this);
        status.setTextIsSelectable(true);
        layout.addView(status);
        button(layout, "1. Enable Accessibility Service", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(layout, "2. Start local MCP server", v -> {
            ControlService svc = ControlService.active();
            if (svc == null) {
                status.setText("Enable the Mobile MCP Accessibility Service first.");
                return;
            }
            try { svc.startServer(); }
            catch (Exception e) {
                status.setText("Server could not start: " + e.getMessage());
                return;
            }
            refresh();
        });
        button(layout, "Stop local MCP server", v -> {
            ControlService svc = ControlService.active();
            if (svc != null) svc.stopServer();
            refresh();
        });
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
        LinearLayout connectionPanel = collapsible(layout, "Connection settings and secret");
        button(connectionPanel, "Rotate secret (stops server)", v -> {
            ControlService svc = ControlService.active();
            if (svc != null) svc.stopServer();
            Auth.rotate(this);
            refresh();
        });
        button(connectionPanel, "Copy MCP client settings", v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("MCP config", clientConfig()));
            status.setText("Settings copied to clipboard. They contain your secret. Do not post them online.");
        });
        settings = new TextView(this);
        settings.setTextIsSelectable(true);
        connectionPanel.addView(settings);

        setContentView(scroll);
        refresh();
    }
    private LinearLayout collapsible(LinearLayout parent, String title) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        button(parent, title, v -> panel.setVisibility(
                panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        parent.addView(panel);
        return panel;
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
    private Button button(LinearLayout parent, String text, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(action);
        parent.addView(b);
        return b;
    }
    private String clientConfig() {
        return "Endpoint: http://127.0.0.1:8765/mcp\nAuthorization: Bearer " + Auth.get(this) +
                "\nTransport: Streamable HTTP (stateless JSON responses)\n";
    }
    private void refresh() {
        ControlService svc = ControlService.active();
        status.setText("Accessibility: " + (svc == null ? "disabled" : "enabled") +
                "\nBuilt-in MCP server: " + (svc != null && svc.isServerRunning() ? "RUNNING on this phone" : "stopped") +
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
