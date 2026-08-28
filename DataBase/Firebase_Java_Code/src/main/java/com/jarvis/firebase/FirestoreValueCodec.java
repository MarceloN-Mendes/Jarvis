package com.jarvis.firebase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A API REST do Firestore não aceita JSON "puro" — cada valor precisa vir
 * envolvido no tipo dele, tipo {"stringValue": "abc"} ou {"doubleValue": 1.5}.
 * Essa classe converte nos dois sentidos, pra o resto do código só lidar
 * com Map/List/String/Double normais do Java.
 */
class FirestoreValueCodec {

    /** Converte um Map<String,Object> Java num objeto "fields" do Firestore. */
    static JsonObject encodeFields(Map<String, Object> data) {
        JsonObject fields = new JsonObject();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            fields.add(entry.getKey(), encodeValue(entry.getValue()));
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("fields", fields);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private static JsonObject encodeValue(Object value) {
        JsonObject wrapped = new JsonObject();

        if (value == null) {
            wrapped.add("nullValue", null);
        } else if (value instanceof String s) {
            wrapped.addProperty("stringValue", s);
        } else if (value instanceof Boolean b) {
            wrapped.addProperty("booleanValue", b);
        } else if (value instanceof Integer || value instanceof Long) {
            wrapped.addProperty("integerValue", value.toString());
        } else if (value instanceof Number n) {
            wrapped.addProperty("doubleValue", n.doubleValue());
        } else if (value instanceof Map) {
            JsonObject inner = encodeFields((Map<String, Object>) value);
            wrapped.add("mapValue", inner);
        } else if (value instanceof List<?> list) {
            JsonArray values = new JsonArray();
            for (Object item : list) {
                values.add(encodeValue(item));
            }
            JsonObject arrayValue = new JsonObject();
            arrayValue.add("values", values);
            wrapped.add("arrayValue", arrayValue);
        } else {
            throw new IllegalArgumentException("Tipo não suportado: " + value.getClass());
        }

        return wrapped;
    }

    /** Converte a resposta bruta de um documento do Firestore de volta pra Map<String,Object>. */
    static Map<String, Object> decodeDocument(JsonObject document) {
        if (document == null || !document.has("fields")) {
            return new LinkedHashMap<>();
        }
        JsonObject fields = document.getAsJsonObject("fields");
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : fields.keySet()) {
            result.put(key, decodeValue(fields.getAsJsonObject(key)));
        }
        return result;
    }

    private static Object decodeValue(JsonObject wrapped) {
        if (wrapped.has("stringValue")) {
            return wrapped.get("stringValue").getAsString();
        }
        if (wrapped.has("booleanValue")) {
            return wrapped.get("booleanValue").getAsBoolean();
        }
        if (wrapped.has("integerValue")) {
            return Long.parseLong(wrapped.get("integerValue").getAsString());
        }
        if (wrapped.has("doubleValue")) {
            return wrapped.get("doubleValue").getAsDouble();
        }
        if (wrapped.has("mapValue")) {
            JsonObject mapValue = wrapped.getAsJsonObject("mapValue");
            return mapValue.has("fields") ? decodeDocument(mapValue) : new LinkedHashMap<>();
        }
        if (wrapped.has("arrayValue")) {
            JsonObject arrayValue = wrapped.getAsJsonObject("arrayValue");
            List<Object> result = new ArrayList<>();
            if (arrayValue.has("values")) {
                for (JsonElement el : arrayValue.getAsJsonArray("values")) {
                    result.add(decodeValue(el.getAsJsonObject()));
                }
            }
            return result;
        }
        return null; // nullValue ou tipo não mapeado
    }
}
