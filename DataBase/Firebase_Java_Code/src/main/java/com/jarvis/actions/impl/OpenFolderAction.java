package com.jarvis.actions.impl;

import com.jarvis.actions.GestureAction;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Abre uma pasta no explorador de arquivos padrão do sistema.
 * Não precisa de .exe/.bat: java.awt.Desktop já faz isso de forma nativa
 * e funciona em Windows, Linux e macOS sem mudar uma linha de código.
 *
 * Parâmetro esperado: "path" -> caminho absoluto da pasta
 */
public class OpenFolderAction implements GestureAction {

    @Override
    public void execute(Map<String, String> params) throws IOException {
        String path = params.get("path");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'path' é obrigatório para OPEN_FOLDER");
        }

        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Pasta não encontrada: " + path);
        }

        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop não suportado neste ambiente");
        }

        Desktop.getDesktop().open(folder);
    }
}
