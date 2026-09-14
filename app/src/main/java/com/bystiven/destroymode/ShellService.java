package com.bystiven.destroymode;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ShellService extends ICommandService.Stub {

    public ShellService() {
    }

    public ShellService(Context context) {
    }

    @Override
    public String exec(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int code = process.waitFor();
            output.append("exit=").append(code);
        } catch (Throwable t) {
            output.append("ERROR: ").append(t.getClass().getSimpleName())
                    .append(": ").append(t.getMessage());
        }
        return output.toString();
    }
}
