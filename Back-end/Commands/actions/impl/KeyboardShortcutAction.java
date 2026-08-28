package com.jarvis.actions.impl;

import com.jarvis.actions.GestureAction;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Simula um atalho de teclado (ex: CTRL+C, ALT+TAB, WIN+D).
 *
 * Parâmetro esperado: "keys" -> teclas separadas por "+", em maiúsculo
 *   Exemplos válidos: "CTRL+C", "ALT+TAB", "WINDOWS+D", "CTRL+SHIFT+ESCAPE"
 *
 * As teclas são resolvidas dinamicamente pelos campos VK_* de KeyEvent,
 * então qualquer tecla que o Java conheça já funciona sem código extra.
 */
public class KeyboardShortcutAction implements GestureAction {

    @Override
    public void execute(Map<String, String> params) throws AWTException {
        String keysParam = params.get("keys");
        if (keysParam == null || keysParam.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'keys' é obrigatório para KEYBOARD_SHORTCUT");
        }

        String[] keyNames = keysParam.toUpperCase().split("\\+");
        int[] keyCodes = new int[keyNames.length];

        for (int i = 0; i < keyNames.length; i++) {
            keyCodes[i] = resolveKeyCode(keyNames[i].trim());
        }

        Robot robot = new Robot();

        // Pressiona todas na ordem
        for (int code : keyCodes) {
            robot.keyPress(code);
        }
        // Solta na ordem inversa (boa prática para combinações)
        for (int i = keyCodes.length - 1; i >= 0; i--) {
            robot.keyRelease(keyCodes[i]);
        }
    }

    private int resolveKeyCode(String keyName) {
        try {
            Field field = KeyEvent.class.getField("VK_" + keyName);
            return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Tecla desconhecida: " + keyName, e);
        }
    }
}
