package com.jarvis.actions;

/**
 * Tipos de ação suportados. Esse valor é o que fica salvo no MySQL
 * junto com o gesto cadastrado (tabela gesture_action_mapping.action_type).
 * Para adicionar uma ação nova: criar o valor aqui + a implementação em
 * impl/ + registrar no ActionFactory. O resto do sistema não muda.
 */
public enum ActionType {
    OPEN_FOLDER,
    OPEN_APP,
    CLOSE_APP,
    KEYBOARD_SHORTCUT,
    MOUSE_CLICK,
    SET_VOLUME,
    MOVE_WINDOW
}
