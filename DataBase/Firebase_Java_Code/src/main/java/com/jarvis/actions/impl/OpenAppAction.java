package com.jarvis.actions.impl;

import com.jarvis.actions.GestureAction;
import com.jarvis.actions.ProcessRegistry;

import java.io.IOException;
import java.util.Map;

/**
 * Abre um programa (executável) e guarda a referência no ProcessRegistry,
 * usando "appName" como apelido — é esse apelido que o CloseAppAction vai
 * usar depois para encontrar e fechar o processo certo.
 *
 * Parâmetros esperados:
 *   "path"    -> caminho do executável (ex: "C:\\Windows\\notepad.exe")
 *   "appName" -> apelido único definido pelo usuário no cadastro (ex: "bloco_de_notas")
 */
public class OpenAppAction implements GestureAction {

    @Override
    public void execute(Map<String, String> params) throws IOException {
        String path = params.get("path");
        String appName = params.get("appName");

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'path' é obrigatório para OPEN_APP");
        }
        if (appName == null || appName.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'appName' é obrigatório para OPEN_APP");
        }

        ProcessBuilder builder = new ProcessBuilder(path);
        Process process = builder.start();

        ProcessRegistry.getInstance().register(appName, process);
    }
}
