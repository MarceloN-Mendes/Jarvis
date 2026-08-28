package com.jarvis.firebase;

import java.util.List;
import java.util.Map;

/**
 * Exemplo de uso do fluxo multi-usuário: cada usuário loga e só enxerga
 * (e só consegue escrever) os próprios gestos, graças ao idToken + regras
 * de segurança do Firestore — sem nenhuma chave privilegiada no app.
 */
public class FirebaseExample {

    public static void main(String[] args) {
        AuthService auth = new AuthService();
        GestureCloudRepository repo = new GestureCloudRepository();

        // 1) Login (troque pelo email/senha reais; use signUp() na 1a vez pra criar a conta)
        AuthService.AuthResult login = auth.signIn("usuario1@exemplo.com", "senha123");
        System.out.println("Logado como: " + login.email() + " (uid=" + login.uid() + ")");

        // 2) Salvar um gesto — vai parar em users/{uid}/gestures/abrir_downloads
        repo.saveGesture(login.idToken(), login.uid(), "abrir_downloads", Map.of(
                "type", "single",
                "signature", List.of(0.0, 0.1, 0.2), // na prática: os 63 números reais
                "actionType", "OPEN_FOLDER",
                "actionParams", Map.of("path", "C:\\Users\\SeuUsuario\\Downloads")
        ));
        System.out.println("Gesto salvo com sucesso.");

        // 3) Ler de volta
        Map<String, Object> gesto = repo.getGesture(login.idToken(), login.uid(), "abrir_downloads");
        System.out.println("Gesto lido do Firestore: " + gesto);

        // 4) Listar todos os gestos do usuário logado
        List<Map<String, Object>> todos = repo.listGestures(login.idToken(), login.uid());
        System.out.println("Total de gestos cadastrados: " + todos.size());

        // Se outro usuário logasse aqui, ele NÃO conseguiria ler/escrever
        // nesses mesmos gestos — as regras do Firestore bloqueiam antes
        // mesmo de qualquer lógica do app entrar em ação.
    }
}
