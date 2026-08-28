package com.jarvis.actions;

import java.util.Map;

/**
 * Contrato que toda ação disparada por um gesto precisa implementar.
 * Cada implementação recebe seus parâmetros como um Map<String, String>
 * — isso é o que permite o usuário "configurar" a ação pela interface,
 * sem precisar mexer em código: os params vêm do banco (MySQL) e mudam
 * de ação para ação (ex: "path" para abrir pasta, "keys" para atalho).
 */
public interface GestureAction {

    /**
     * Executa a ação com os parâmetros fornecidos.
     * @param params parâmetros específicos dessa ação (ex: {"path": "C:\\Users\\..."})
     */
    void execute(Map<String, String> params) throws Exception;
}
