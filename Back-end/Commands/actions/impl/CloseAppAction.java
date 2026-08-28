package com.jarvis.actions.impl;

import com.jarvis.actions.GestureAction;
import com.jarvis.actions.ProcessRegistry;

import java.util.Map;

/**
 * Fecha um programa que foi aberto anteriormente por OpenAppAction,
 * localizando o processo pelo mesmo "appName" usado na abertura.
 *
 * Parâmetro esperado: "appName" -> mesmo apelido usado no OPEN_APP
 *
 * Observação: só funciona para apps abertos pelo próprio sistema.
 * Para fechar algo que já estava aberto antes (ex: um app que o usuário
 * abriu manualmente), seria necessário localizar o processo pelo nome via
 * comando do SO (tasklist/taskkill no Windows) — dá pra evoluir depois.
 */
public class CloseAppAction implements GestureAction {

    @Override
    public void execute(Map<String, String> params) {
        String appName = params.get("appName");
        if (appName == null || appName.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'appName' é obrigatório para CLOSE_APP");
        }

        Process process = ProcessRegistry.getInstance().get(appName);
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException("Nenhum processo em execução encontrado para: " + appName);
        }

        process.destroy();
        ProcessRegistry.getInstance().remove(appName);
    }
}
