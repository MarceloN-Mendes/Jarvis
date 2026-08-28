package com.jarvis.firebase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Login e cadastro de usuário (email/senha) via API REST do Firebase Auth.
 *
 * Motivo de usar REST em vez do Admin SDK aqui: o Admin SDK é pra
 * administrar contas (criar, listar, deletar), não pra "logar" com senha —
 * essa validação de senha só existe na Identity Toolkit REST API, que é o
 * mesmo endpoint usado pelos SDKs oficiais de Android/iOS/Web por trás dos
 * panos.
 *
 * A WEB_API_KEY é pública por natureza (fica embutida em qualquer app
 * cliente) — quem protege os dados de verdade são as regras de segurança
 * do Firestore, não essa chave.
 */
public class AuthService {

    private static final String SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FirebaseSettings.WEB_API_KEY;
    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FirebaseSettings.WEB_API_KEY;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Resultado de uma autenticação bem-sucedida.
     */
    public record AuthResult(String uid, String idToken, String email) {
    }

    /**
     * Cria uma conta nova. Lança RuntimeException com a mensagem de erro
     * do Firebase se o email já existir, senha fraca, etc.
     */
    public AuthResult signUp(String email, String password) {
        return chamarAuth(SIGN_UP_URL, email, password);
    }

    /**
     * Faz login numa conta existente. Lança RuntimeException se as
     * credenciais estiverem erradas.
     */
    public AuthResult signIn(String email, String password) {
        return chamarAuth(SIGN_IN_URL, email, password);
    }

    private AuthResult chamarAuth(String url, String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("returnSecureToken", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (response.statusCode() != 200) {
                String mensagemErro = json.getAsJsonObject("error")
                        .get("message").getAsString();
                throw new RuntimeException("Falha na autenticação: " + mensagemErro);
            }

            return new AuthResult(
                    json.get("localId").getAsString(),
                    json.get("idToken").getAsString(),
                    json.get("email").getAsString()
            );
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Erro de conexão com o Firebase Auth", e);
        }
    }
}
