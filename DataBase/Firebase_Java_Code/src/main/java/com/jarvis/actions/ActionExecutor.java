package com.jarvis.actions;

/**
 * É aqui que tudo se conecta: quando o motor Python reconhece um gesto e
 * manda o evento via WebSocket, o back-end busca o GestureActionMapping
 * correspondente no MySQL e chama executeForGesture(mapping).
 *
 * Esse método NÃO sabe (e não precisa saber) qual ação está sendo executada
 * — só delega pro ActionFactory. É isso que te dá a "generalização": novos
 * tipos de ação entram sem tocar nessa classe.
 */
public class ActionExecutor {

    public void executeForGesture(GestureActionMapping mapping) {
        try {
            GestureAction action = ActionFactory.create(mapping.getActionType());
            action.execute(mapping.getParams());
            System.out.println("Ação executada com sucesso para gesto: " + mapping.getGestureId());
        } catch (Exception e) {
            // Em produção: logar com um logger de verdade (SLF4J) em vez de println
            System.err.println("Falha ao executar ação para gesto '" + mapping.getGestureId()
                    + "': " + e.getMessage());
        }
    }

    // Exemplo de uso rápido, sem banco, só pra validar a estrutura:
    public static void main(String[] args) {
        ActionExecutor executor = new ActionExecutor();

        var mapping = new GestureActionMapping(
                "abrir_downloads",
                ActionType.OPEN_FOLDER,
                java.util.Map.of("path", "C:\\Users\\SeuUsuario\\Downloads")
        );

        executor.executeForGesture(mapping);
    }
}
