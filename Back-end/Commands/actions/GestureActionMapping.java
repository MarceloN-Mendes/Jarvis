package com.jarvis.actions;

import java.util.Map;

/**
 * Representa uma linha da tabela gesture_action_mapping no MySQL:
 * qual gesto dispara qual tipo de ação, com quais parâmetros.
 *
 * Sugestão de schema:
 *   CREATE TABLE gesture_action_mapping (
 *       id           BIGINT PRIMARY KEY AUTO_INCREMENT,
 *       user_id      BIGINT NOT NULL,
 *       gesture_id   VARCHAR(50) NOT NULL,   -- nome do gesto cadastrado
 *       action_type  VARCHAR(30) NOT NULL,   -- valor do enum ActionType
 *       params_json  JSON NOT NULL,          -- ex: {"path": "C:\\Users\\..."}
 *       FOREIGN KEY (user_id) REFERENCES users(id)
 *   );
 *
 * O params_json vira o Map<String,String> que cada GestureAction recebe.
 */
public class GestureActionMapping {

    private String gestureId;
    private ActionType actionType;
    private Map<String, String> params;

    public GestureActionMapping(String gestureId, ActionType actionType, Map<String, String> params) {
        this.gestureId = gestureId;
        this.actionType = actionType;
        this.params = params;
    }

    public String getGestureId() {
        return gestureId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
