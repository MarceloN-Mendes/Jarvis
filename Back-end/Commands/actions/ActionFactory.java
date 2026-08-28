package com.jarvis.actions;

import com.jarvis.actions.impl.CloseAppAction;
import com.jarvis.actions.impl.KeyboardShortcutAction;
import com.jarvis.actions.impl.MouseClickAction;
import com.jarvis.actions.impl.OpenAppAction;
import com.jarvis.actions.impl.OpenFolderAction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ponto único de resolução: dado um ActionType, devolve uma instância
 * pronta pra executar. Para adicionar uma ação nova no futuro (ex: SET_VOLUME,
 * MOVE_WINDOW), basta criar a classe em impl/ e registrar uma linha aqui —
 * nenhum outro ponto do sistema precisa mudar.
 */
public class ActionFactory {

    private static final Map<ActionType, Supplier<GestureAction>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(ActionType.OPEN_FOLDER, OpenFolderAction::new);
        REGISTRY.put(ActionType.OPEN_APP, OpenAppAction::new);
        REGISTRY.put(ActionType.CLOSE_APP, CloseAppAction::new);
        REGISTRY.put(ActionType.KEYBOARD_SHORTCUT, KeyboardShortcutAction::new);
        REGISTRY.put(ActionType.MOUSE_CLICK, MouseClickAction::new);
        // TODO: SET_VOLUME e MOVE_WINDOW ainda não implementados
        // (precisam de PowerShell/JNA respectivamente — posso montar quando chegarmos lá)
    }

    public static GestureAction create(ActionType type) {
        Supplier<GestureAction> supplier = REGISTRY.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Nenhuma implementação registrada para: " + type);
        }
        return supplier.get();
    }
}
