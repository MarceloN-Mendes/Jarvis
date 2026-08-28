package com.jarvis.actions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda referência dos processos que o próprio sistema abriu (OpenAppAction),
 * para que CloseAppAction consiga encerrá-los depois pelo mesmo "apelido".
 * Singleton simples — em uma versão futura pode virar um bean gerenciado pelo Spring.
 */
public class ProcessRegistry {

    private static final ProcessRegistry INSTANCE = new ProcessRegistry();
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    private ProcessRegistry() {}

    public static ProcessRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String appName, Process process) {
        runningProcesses.put(appName, process);
    }

    public Process get(String appName) {
        return runningProcesses.get(appName);
    }

    public void remove(String appName) {
        runningProcesses.remove(appName);
    }
}
