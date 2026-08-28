package com.jarvis.firebase;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CRUD de gestos no Firestore via API REST, autenticado com o idToken do
 * usuário logado (não a chave de admin). Isso é o que permite distribuir o
 * app pra vários usuários com segurança: as Regras do Firestore (ver
 * firestore.rules) garantem que cada usuário só toca nos próprios dados,
 * baseado em quem o token representa — não em confiar no código do app.
 *
 * Estrutura no banco (igual a antes): users/{uid}/gestures/{nomeDoGesto}
 */
public class GestureCloudRepository {

    private static final String BASE_URL =
            "https://firestore.googleapis.com/v1/projects/" + FirebaseSettings.PROJECT_ID
                    + "/databases/(default)/documents";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String gestureDocUrl(String uid, String gestureName) {
        return BASE_URL + "/users/" + uid + "/gestures/" + gestureName;
    }

    public void saveGesture(String idToken, String uid, String gestureName, Map<String, Object> data) {
        JsonObject body = FirestoreValueCodec.encodeFields(data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gestureDocUrl(uid, gestureName)))
                .header("Authorization", "Bearer " + idToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = enviar(request);
        verificarErro(response, "salvar gesto '" + gestureName + "'");
    }

    public Map<String, Object> getGesture(String idToken, String uid, String gestureName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gestureDocUrl(uid, gestureName)))
                .header("Authorization", "Bearer " + idToken)
                .GET()
                .build();

        HttpResponse<String> response = enviar(request);
        if (response.statusCode() == 404) {
            return null;
        }
        verificarErro(response, "buscar gesto '" + gestureName + "'");

        JsonObject document = JsonParser.parseString(response.body()).getAsJsonObject();
        return FirestoreValueCodec.decodeDocument(document);
    }

    public List<Map<String, Object>> listGestures(String idToken, String uid) {
        String url = BASE_URL + "/users/" + uid + "/gestures";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + idToken)
                .GET()
                .build();

        HttpResponse<String> response = enviar(request);
        verificarErro(response, "listar gestos");

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        List<Map<String, Object>> resultado = new ArrayList<>();

        if (json.has("documents")) {
            JsonArray documentos = json.getAsJsonArray("documents");
            for (int i = 0; i < documentos.size(); i++) {
                JsonObject doc = documentos.get(i).getAsJsonObject();
                Map<String, Object> data = FirestoreValueCodec.decodeDocument(doc);
                // "name" do Firestore vem como caminho completo; extrai só o ID do gesto
                String fullName = doc.get("name").getAsString();
                data.put("name", fullName.substring(fullName.lastIndexOf('/') + 1));
                resultado.add(data);
            }
        }

        return resultado;
    }

    public void deleteGesture(String idToken, String uid, String gestureName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gestureDocUrl(uid, gestureName)))
                .header("Authorization", "Bearer " + idToken)
                .DELETE()
                .build();

        HttpResponse<String> response = enviar(request);
        verificarErro(response, "apagar gesto '" + gestureName + "'");
    }

    private HttpResponse<String> enviar(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Erro de conexão com o Firestore", e);
        }
    }

    private void verificarErro(HttpResponse<String> response, String contexto) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new RuntimeException(
                "Falha ao " + contexto + " (HTTP " + response.statusCode() + "): " + response.body());
    }
}
