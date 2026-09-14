package com.bystiven.destroymode;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final String ARENA_PACKAGE = "com.proximabeta.mf.liteuamo";
    private static final int SHIZUKU_REQUEST_CODE = 41;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ICommandService shell;
    private Runnable pendingAction;

    private TextView statusText;
    private Button playButton;
    private Button restoreButton;

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (requestCode != SHIZUKU_REQUEST_CODE) return;
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindShellThenRun();
                } else {
                    showStatus("Shizuku sin permiso", false);
                }
            };

    private final Shizuku.OnBinderReceivedListener binderListener = () ->
            runOnUiThread(this::refreshStatus);

    private final Shizuku.OnBinderDeadListener binderDeadListener = () ->
            runOnUiThread(() -> showStatus("Shizuku detenido", false));

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            shell = ICommandService.Stub.asInterface(service);
            showStatus("Listo para destruir gráficos", true);
            Runnable action = pendingAction;
            pendingAction = null;
            if (action != null) action.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            shell = null;
            showStatus("Servicio desconectado", false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());

        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addBinderDeadListener(binderDeadListener);

        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        executor.shutdownNow();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(9, 10, 15));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(36));
        scroll.addView(root);

        TextView eyebrow = text("XIAOMI 14T PRO · ARENA BREAKOUT LITE", 12, Color.rgb(160, 165, 180));
        root.addView(eyebrow);

        TextView title = text("DESTROY\nMODE", 42, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(6), 0, dp(6));
        root.addView(title);

        TextView subtitle = text("FPS primero. Calidad después.", 18, Color.rgb(205, 208, 218));
        root.addView(subtitle);

        statusText = text("Comprobando…", 15, Color.rgb(255, 183, 77));
        statusText.setPadding(0, dp(24), 0, dp(20));
        root.addView(statusText);

        playButton = button("🔥 ACTIVAR Y JUGAR");
        playButton.setOnClickListener(v -> ensureShell(this::applyAndLaunch));
        root.addView(playButton, fullWidth(dp(58)));

        restoreButton = button("↩ RESTAURAR ARENA");
        restoreButton.setOnClickListener(v -> ensureShell(this::restoreArena));
        root.addView(restoreButton, fullWidth(dp(54)));

        Button shizukuButton = button("ABRIR SHIZUKU");
        shizukuButton.setOnClickListener(v -> openShizuku());
        root.addView(shizukuButton, fullWidth(dp(50)));

        TextView profile = text(
                "Perfil actual de Destroy Mode\n\n" +
                        "• 120 FPS objetivo\n" +
                        "• Downscale Android: 0.8\n" +
                        "• Game mode: Performance\n" +
                        "• Arena Lite: 480p / mínimo\n" +
                        "• Game Turbo: 1X / Alta velocidad / LOD +2\n\n" +
                        "La app NO modifica el APK, assets ni archivos internos de Arena.",
                14,
                Color.rgb(180, 184, 198));
        profile.setPadding(0, dp(26), 0, 0);
        root.addView(profile);

        return scroll;
    }

    private LinearLayout.LayoutParams fullWidth(int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, dp(8), 0, dp(8));
        return params;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        return b;
    }

    private TextView text(String value, int sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.START);
        return tv;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshStatus() {
        boolean arenaInstalled = getPackageManager().getLaunchIntentForPackage(ARENA_PACKAGE) != null;
        if (!arenaInstalled) {
            showStatus("Arena Breakout Lite no está instalado", false);
            return;
        }

        if (!Shizuku.pingBinder()) {
            showStatus("Inicia Shizuku para usar Destroy Mode", false);
            return;
        }

        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                showStatus("Falta autorizar Destroy Mode en Shizuku", false);
                return;
            }
        } catch (Throwable t) {
            showStatus("No pude comprobar Shizuku", false);
            return;
        }

        if (shell == null) {
            showStatus("Shizuku listo · toca ACTIVAR Y JUGAR", true);
        } else {
            showStatus("Listo para destruir gráficos", true);
        }
    }

    private void ensureShell(Runnable action) {
        pendingAction = action;

        if (!Shizuku.pingBinder()) {
            pendingAction = null;
            showStatus("Shizuku no está ejecutándose", false);
            Toast.makeText(this, "Abre Shizuku e inícialo primero", Toast.LENGTH_LONG).show();
            return;
        }

        if (Shizuku.isPreV11()) {
            pendingAction = null;
            showStatus("Esta versión de Shizuku es demasiado antigua", false);
            return;
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                pendingAction = null;
                showStatus("Autoriza la app desde Shizuku", false);
                return;
            }
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            return;
        }

        if (shell != null) {
            Runnable run = pendingAction;
            pendingAction = null;
            run.run();
            return;
        }

        bindShellThenRun();
    }

    private void bindShellThenRun() {
        try {
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(getPackageName(), ShellService.class.getName()))
                    .processNameSuffix("destroy")
                    .daemon(false)
                    .tag("destroy-mode-shell")
                    .version(1);

            showStatus("Conectando con Shizuku…", true);
            Shizuku.bindUserService(args, connection);
        } catch (Throwable t) {
            pendingAction = null;
            showStatus("Error conectando Shizuku: " + t.getMessage(), false);
        }
    }

    private void applyAndLaunch() {
        setBusy(true);
        showStatus("Aplicando 120 FPS + Performance + 0.8…", true);

        executor.execute(() -> {
            StringBuilder log = new StringBuilder();
            log.append(exec("cmd game set --mode 2 --downscale 0.8 --fps 120 " + ARENA_PACKAGE));
            log.append('\n').append(exec("cmd game mode 2 " + ARENA_PACKAGE));
            log.append('\n').append(exec("am force-stop " + ARENA_PACKAGE));

            try {
                Thread.sleep(700);
            } catch (InterruptedException ignored) {
            }

            String launchResult = exec("monkey -p " + ARENA_PACKAGE + " -c android.intent.category.LAUNCHER 1");
            log.append('\n').append(launchResult);

            runOnUiThread(() -> {
                setBusy(false);
                String fullLog = log.toString();
                if (fullLog.contains("ERROR") || fullLog.contains("Invalid") || fullLog.contains("No activities found")) {
                    showStatus("Perfil aplicado, pero Arena no abrió", false);
                    Toast.makeText(this, fullLog, Toast.LENGTH_LONG).show();
                    fallbackLaunch();
                    return;
                }

                showStatus("Destroy Mode activo · Arena iniciada", true);
            });
        });
    }

    private void fallbackLaunch() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(ARENA_PACKAGE);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launch);
            } else {
                showStatus("No encontré el launcher de Arena Lite", false);
            }
        } catch (Throwable t) {
            showStatus("No pude abrir Arena: " + t.getMessage(), false);
        }
    }

    private void restoreArena() {
        setBusy(true);
        showStatus("Restaurando configuración del sistema…", true);

        executor.execute(() -> {
            String result = exec("cmd game reset " + ARENA_PACKAGE) + "\n" +
                    exec("cmd game mode 1 " + ARENA_PACKAGE) + "\n" +
                    exec("am force-stop " + ARENA_PACKAGE);

            runOnUiThread(() -> {
                setBusy(false);
                if (result.contains("ERROR") || result.contains("Invalid")) {
                    showStatus("La restauración devolvió un error", false);
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                } else {
                    showStatus("Arena restaurado al perfil normal", true);
                    Toast.makeText(this, "Restaurado", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String exec(String command) {
        try {
            return shell.exec(command);
        } catch (Throwable t) {
            return "ERROR: " + t.getMessage();
        }
    }

    private void setBusy(boolean busy) {
        playButton.setEnabled(!busy);
        restoreButton.setEnabled(!busy);
    }

    private void showStatus(String message, boolean ok) {
        runOnUiThread(() -> {
            statusText.setText((ok ? "● " : "○ ") + message);
            statusText.setTextColor(ok ? Color.rgb(95, 220, 145) : Color.rgb(255, 183, 77));
        });
    }

    private void openShizuku() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if (launch != null) {
            startActivity(launch);
        } else {
            startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        }
    }
}
