package org.example;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles macOS LWJGL3 -XstartOnFirstThread requirement automatically.
 * If running on macOS without -XstartOnFirstThread, this spawns a new JVM with the argument.
 */
public class StartupHelper {
    private static final String JVM_RESTARTED_ARG = "jvmIsRestarted";

    public static boolean startNewJvmIfRequired(boolean redirectOutput) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            return false;
        }

        if ("true".equals(System.getProperty(JVM_RESTARTED_ARG))) {
            return false;
        }

        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (arg.contains("-XstartOnFirstThread")) {
                return false;
            }
        }

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-XstartOnFirstThread");
        command.add("-D" + JVM_RESTARTED_ARG + "=true");
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));

        String mainClass = "org.example.DesktopLauncher";
        try {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            if (trace.length > 0) {
                mainClass = trace[trace.length - 1].getClassName();
            }
        } catch (Exception ignored) {}

        command.add(mainClass);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (redirectOutput) {
                pb.inheritIO();
            }
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean startNewJvmIfRequired() {
        return startNewJvmIfRequired(true);
    }
}
