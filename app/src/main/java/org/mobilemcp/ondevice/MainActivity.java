package org.mobilemcp.ondevice;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Visible, opt-in controller. No network listener is started automatically. */
public final class MainActivity extends Activity {
    private TextView status;
    private TextView settings;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        final int pad = (int)(16 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad,pad,pad,pad);
        TextView title = new TextView(this);
        title.setText("Mobile MCP • On-device Android port");
        title.setTextSize(21);
        layout.addView(title);

        TextView warning = new TextView(this);
        warning.setText("Accessibility can read screen content and control taps. Enable it only if you trust your MCP client. Server is bound to 127.0.0.1 and requires a secret bearer token. This is NOT the original desktop Mobile MCP server.");
        layout.addView(warning);
        status = new TextView(this); layout.addView(status);

        button(layout, "1. Enable Accessibility Service", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(layout, "2. Start local MCP server", v -> {
            ControlService svc = ControlService.active();
            if (svc == null) { status.setText("Enable Mobile MCP device controller in Android Accessibility settings first."); return; }
            try { svc.startServer(); } catch (Exception e) { status.setText("Start failed: " + e.getMessage()); }
            refresh();
        });
        button(layout, "Stop local MCP server", v -> { ControlService svc = ControlService.active(); if (svc != null) svc.stopServer(); refresh(); });
        button(layout, "Rotate bearer token (stops server)", v -> {
            ControlService svc = ControlService.active(); if (svc != null) svc.stopServer();
            Auth.rotate(this); refresh();
        });
        button(layout, "Copy MCP client settings", v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("MCP config", clientConfig()));
            status.setText("Client settings copied. They contain a SECRET token: do not share or paste publicly.");
        });
        settings = new TextView(this);
        settings.setTextIsSelectable(true);
        settings.setMovementMethod(new ScrollingMovementMethod());
        layout.addView(settings);
        setContentView(layout);
        refresh();
    }

    private void button(LinearLayout panel, String text, View.OnClickListener click) {
        Button b = new Button(this); b.setText(text); b.setOnClickListener(click); panel.addView(b);
    }

    private String clientConfig() {
        return "Endpoint: http://127.0.0.1:8765/mcp\nAuthorization: Bearer " + Auth.get(this)
                + "\nTransport: Streamable HTTP (JSON responses)\n";
    }

    private void refresh() {
        ControlService svc = ControlService.active();
        status.setText("Accessibility: " + (svc == null ? "disabled" : "enabled") +
                "\nLocal MCP: " + (svc != null && svc.isServerRunning() ? "RUNNING" : "stopped"));
        if (settings != null) settings.setText(clientConfig());
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
}
