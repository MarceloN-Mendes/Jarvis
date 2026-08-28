package com.jarvis.actions.impl;

import com.jarvis.actions.GestureAction;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Map;

/**
 * Simula um clique de mouse — o gesto de "clicar com a mão" que você mencionou.
 *
 * Parâmetros esperados (todos opcionais):
 *   "x", "y"  -> posição do clique. Se omitidos, clica na posição atual do cursor
 *                (útil quando o próprio gesto de mão já está controlando o cursor).
 *   "button"  -> "LEFT" (padrão) ou "RIGHT"
 */
public class MouseClickAction implements GestureAction {

    @Override
    public void execute(Map<String, String> params) throws AWTException {
        Robot robot = new Robot();

        int x, y;
        if (params.containsKey("x") && params.containsKey("y")) {
            x = Integer.parseInt(params.get("x"));
            y = Integer.parseInt(params.get("y"));
            robot.mouseMove(x, y);
        } else {
            var pos = MouseInfo.getPointerInfo().getLocation();
            x = pos.x;
            y = pos.y;
        }

        String button = params.getOrDefault("button", "LEFT").toUpperCase();
        int mask = button.equals("RIGHT")
                ? InputEvent.BUTTON3_DOWN_MASK
                : InputEvent.BUTTON1_DOWN_MASK;

        robot.mousePress(mask);
        robot.mouseRelease(mask);
    }
}
