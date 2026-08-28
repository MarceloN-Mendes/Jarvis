"""
Motor de Visão Computacional - Leitura de Mão
===============================================

Fase A do projeto: captura da câmera, detecção dos 21 landmarks de cada mão,
cadastro de gestos (usando a mão inteira, não só um padrão de dedos) e
reconhecimento em tempo real por vizinho mais próximo.

Esse módulo é a base sobre a qual depois vamos montar:
    - Persistência no MySQL (hoje: gestures.json local)
    - Envio do evento "gesto reconhecido" para o back-end Java via WebSocket

IMPORTANTE: a partir da versão 1.0.0 do MediaPipe (27/07/2026), o módulo legado
`mp.solutions` foi removido. Este script usa a Tasks API atual (HandLandmarker).

Requisitos:
    pip install mediapipe opencv-python

Modelo necessário (baixado automaticamente na primeira execução):
    hand_landmarker.task

Como rodar:
    python hand_tracker.py

Controles:
    'c' -> cadastrar o gesto atual (segure a pose e pressione)
    'q' -> sair
"""

import cv2
import mediapipe as mp
import time
import json
import os
import math
import urllib.request
import webbrowser
from pathlib import Path

from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/"
    "hand_landmarker/float16/1/hand_landmarker.task"
)
MODEL_PATH = "hand_landmarker.task"
GESTURES_FILE = "gestures.json"

# Caminho do index.html do site que deve abrir quando um gesto é reconhecido.
# AJUSTE para o caminho real na sua máquina.
SITE_INDEX_PATH = r"E:\Marcelo\Code\Java\Jarvis\Back-end\temporarioLexy\index.html"

# Atraso (em segundos) entre reconhecer o gesto e abrir o site
OPEN_SITE_DELAY = 1.0

# Limiar de distância pra considerar "é o mesmo gesto". Quanto MENOR, mais
# rígido o reconhecimento (menos falso positivo, mas exige pose mais precisa).
# Ajuste esse valor testando na prática.
MATCH_THRESHOLD = 0.25

# Conexões entre os 21 landmarks da mão, para desenhar o "esqueleto"
HAND_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 4),          # polegar
    (0, 5), (5, 6), (6, 7), (7, 8),          # indicador
    (5, 9), (9, 10), (10, 11), (11, 12),     # médio
    (9, 13), (13, 14), (14, 15), (15, 16),   # anelar
    (13, 17), (17, 18), (18, 19), (19, 20),  # mindinho
    (0, 17),                                  # base da palma
]


# ---------------------------------------------------------------------------
# Assinatura da mão (usa os 21 pontos inteiros, não só um padrão de dedos)
# ---------------------------------------------------------------------------

def normalize_hand(raw_landmarks):
    """
    Transforma os 21 landmarks brutos em uma "assinatura" normalizada:
    uma lista de 63 números (x, y, z de cada ponto) que representa só a
    FORMA da mão, sem depender de onde ela está na tela nem de quão perto
    da câmera está.

    - Translação: usa o pulso (landmark 0) como origem
    - Escala: divide tudo pela distância pulso -> base do dedo médio (landmark 9)

    Isso resolve o problema do polegar: não existe mais uma regra manual
    "esticado/dobrado" — a posição real de cada ponto (incluindo o polegar)
    já fica corretamente representada, em qualquer ângulo.
    """
    wrist = raw_landmarks[0]
    ref = raw_landmarks[9]  # base do dedo médio, usado como referência de escala

    scale = math.sqrt(
        (ref.x - wrist.x) ** 2 + (ref.y - wrist.y) ** 2 + (ref.z - wrist.z) ** 2
    )
    if scale < 1e-6:
        scale = 1e-6  # evita divisão por zero se a mão estiver mal detectada

    signature = []
    for lm in raw_landmarks:
        signature.append((lm.x - wrist.x) / scale)
        signature.append((lm.y - wrist.y) / scale)
        signature.append((lm.z - wrist.z) / scale)

    return signature


def signature_distance(sig_a, sig_b):
    """Distância RMS entre duas assinaturas (quanto menor, mais parecidas)."""
    squared_diffs = [(a - b) ** 2 for a, b in zip(sig_a, sig_b)]
    return math.sqrt(sum(squared_diffs) / len(squared_diffs))


# ---------------------------------------------------------------------------
# Banco de gestos (hoje: arquivo JSON local; amanhã: MySQL)
# ---------------------------------------------------------------------------

def load_gestures():
    if os.path.exists(GESTURES_FILE):
        with open(GESTURES_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"single": [], "composite": []}


def save_gestures(db):
    with open(GESTURES_FILE, "w", encoding="utf-8") as f:
        json.dump(db, f, indent=2, ensure_ascii=False)


def register_gesture(hands_data, db):
    """
    Cadastra o gesto atual. Se 1 mão está visível, cadastra gesto singular.
    Se 2 mãos estão visíveis, cadastra gesto composto (as duas ao mesmo tempo).
    """
    if not hands_data:
        print("Nenhuma mão detectada — não é possível cadastrar.")
        return

    name = input("Digite o nome do gesto a cadastrar: ").strip()
    if not name:
        print("Nome vazio, cadastro cancelado.")
        return

    if len(hands_data) == 1:
        signature = normalize_hand(hands_data[0]["raw_landmarks"])
        db["single"].append({"name": name, "signature": signature})
        print(f"Gesto singular '{name}' cadastrado com sucesso.")

    elif len(hands_data) == 2:
        by_hand = {h["handedness"]: h["raw_landmarks"] for h in hands_data}
        if "Left" not in by_hand or "Right" not in by_hand:
            print("Não foi possível identificar as duas mãos claramente. Tente novamente.")
            return
        left_sig = normalize_hand(by_hand["Left"])
        right_sig = normalize_hand(by_hand["Right"])
        db["composite"].append({
            "name": name,
            "left_signature": left_sig,
            "right_signature": right_sig,
        })
        print(f"Gesto composto '{name}' cadastrado com sucesso.")

    save_gestures(db)


def recognize_gesture(hands_data, db):
    """
    Compara a(s) mão(s) atual(is) com os gestos cadastrados e retorna o
    nome do gesto mais próximo dentro do limiar (ou None se nada bateu).

    Prioridade: gesto composto (2 mãos) > gesto singular (1 mão).
    """
    if len(hands_data) == 2 and db["composite"]:
        by_hand = {h["handedness"]: h["raw_landmarks"] for h in hands_data}
        if "Left" in by_hand and "Right" in by_hand:
            live_left = normalize_hand(by_hand["Left"])
            live_right = normalize_hand(by_hand["Right"])

            best_name, best_dist = None, MATCH_THRESHOLD
            for g in db["composite"]:
                d = (
                    signature_distance(live_left, g["left_signature"])
                    + signature_distance(live_right, g["right_signature"])
                ) / 2
                if d < best_dist:
                    best_dist, best_name = d, g["name"]
            if best_name:
                return best_name

    if db["single"]:
        best_name, best_dist = None, MATCH_THRESHOLD
        for hand in hands_data:
            live_sig = normalize_hand(hand["raw_landmarks"])
            for g in db["single"]:
                d = signature_distance(live_sig, g["signature"])
                if d < best_dist:
                    best_dist, best_name = d, g["name"]
        if best_name:
            return best_name

    return None


# ---------------------------------------------------------------------------
# Motor de visão computacional
# ---------------------------------------------------------------------------

def ensure_model():
    """Baixa o modelo hand_landmarker.task se ele ainda não existir localmente."""
    if not os.path.exists(MODEL_PATH):
        print("Baixando modelo hand_landmarker.task (só acontece na 1ª vez)...")
        urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
        print("Modelo baixado com sucesso.")


class HandTracker:
    def __init__(self, max_hands=2, detection_confidence=0.7, tracking_confidence=0.7):
        ensure_model()
        base_options = mp_python.BaseOptions(model_asset_path=MODEL_PATH)
        options = mp_vision.HandLandmarkerOptions(
            base_options=base_options,
            running_mode=mp_vision.RunningMode.VIDEO,
            num_hands=max_hands,
            min_hand_detection_confidence=detection_confidence,
            min_tracking_confidence=tracking_confidence,
        )
        self.landmarker = mp_vision.HandLandmarker.create_from_options(options)

    def process_frame(self, frame, timestamp_ms):
        """Recebe um frame BGR (OpenCV) e retorna o frame anotado + dados das mãos."""
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_frame)

        result = self.landmarker.detect_for_video(mp_image, timestamp_ms)

        hands_data = []
        h, w, _ = frame.shape

        if result.hand_landmarks:
            for idx_hand, hand_landmarks in enumerate(result.hand_landmarks):
                points = []
                for lm in hand_landmarks:
                    x_px, y_px = int(lm.x * w), int(lm.y * h)
                    points.append((x_px, y_px))
                    cv2.circle(frame, (x_px, y_px), 4, (0, 255, 0), -1)

                for start, end in HAND_CONNECTIONS:
                    cv2.line(frame, points[start], points[end], (255, 255, 255), 2)

                handedness_label = result.handedness[idx_hand][0].category_name

                hands_data.append({
                    "handedness": handedness_label,
                    "raw_landmarks": hand_landmarks,  # usado no cálculo da assinatura
                })

        return frame, hands_data

    def close(self):
        self.landmarker.close()


def open_site():
    """Abre o index.html local no navegador padrão do sistema."""
    caminho = Path(SITE_INDEX_PATH)
    if not caminho.exists():
        print(f"AVISO: não encontrei o site em '{SITE_INDEX_PATH}'. "
              f"Ajuste a constante SITE_INDEX_PATH no topo do script.")
        return
    webbrowser.open(caminho.as_uri())


def open_camera(index=0):
    """Abre a câmera. No Windows, o backend DirectShow evita delay/lentidão na abertura."""
    if os.name == "nt":
        cap = cv2.VideoCapture(index, cv2.CAP_DSHOW)
    else:
        cap = cv2.VideoCapture(index)
    return cap


def main():
    cap = open_camera(0)
    if not cap.isOpened():
        print("Não foi possível acessar a câmera. Verifique:")
        print("  - Se outra aplicação já está usando a webcam")
        print("  - As permissões de câmera do Windows (Configurações > Privacidade)")
        print("  - Se o índice 0 é o correto (tente 1, 2... se tiver mais de uma câmera)")
        return

    tracker = HandTracker(max_hands=2)
    gestures_db = load_gestures()
    prev_time = 0
    start_time = time.time()
    last_gesture = None

    # Estado da contagem regressiva de cadastro
    REGISTRATION_SECONDS = 5
    is_counting_down = False
    countdown_start = None

    # Estado do agendamento de "abrir o site depois do atraso"
    site_open_scheduled_at = None

    print("Motor de leitura de mão iniciado.")
    print("  'c' -> iniciar cadastro (você terá 5s para posicionar o gesto)")
    print("  'q' -> sair")

    try:
        while True:
            success, frame = cap.read()
            if not success:
                print("Falha ao capturar frame da câmera.")
                break

            frame = cv2.flip(frame, 1)
            timestamp_ms = int((time.time() - start_time) * 1000)
            frame, hands_data = tracker.process_frame(frame, timestamp_ms)

            curr_time = time.time()
            fps = 1 / (curr_time - prev_time) if prev_time else 0
            prev_time = curr_time
            cv2.putText(frame, f"FPS: {int(fps)}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

            if is_counting_down:
                elapsed = curr_time - countdown_start
                remaining = REGISTRATION_SECONDS - elapsed

                if remaining > 0:
                    # Mostra a contagem regressiva grande, no centro da tela
                    seconds_left = math.ceil(remaining)
                    text = str(seconds_left)
                    (tw, th), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 4, 6)
                    x = (frame.shape[1] - tw) // 2
                    y = (frame.shape[0] + th) // 2
                    cv2.putText(frame, text, (x, y),
                                cv2.FONT_HERSHEY_SIMPLEX, 4, (0, 0, 255), 6)
                    cv2.putText(frame, "Posicione o gesto...", (10, 110),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 165, 255), 2)
                else:
                    # Tempo esgotado: captura o gesto exatamente nesse frame
                    is_counting_down = False
                    register_gesture(hands_data, gestures_db)
            else:
                gesture = recognize_gesture(hands_data, gestures_db)

                if gesture:
                    cv2.putText(frame, f"GESTO: {gesture}", (10, 70),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

                if gesture != last_gesture:
                    if gesture == "LoveGesture":
                        print(f"Gesto reconhecido: {gesture}")
                        print(f"Abrindo o site em {OPEN_SITE_DELAY:.0f}s...")
                        site_open_scheduled_at = curr_time + OPEN_SITE_DELAY
                        # Futuramente: buscar o GestureActionMapping e enviar o
                        # evento via WebSocket pro back-end Java executar a ação.
                    last_gesture = gesture

                # Dispara a abertura do site quando o atraso termina, sem
                # travar a captura da câmera enquanto espera (não usa sleep)
                if site_open_scheduled_at is not None and curr_time >= site_open_scheduled_at:
                    open_site()
                    site_open_scheduled_at = None

            cv2.imshow("Leitura de Mao - Fase A", frame)

            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break
            elif key == ord('c') and not is_counting_down:
                is_counting_down = True
                countdown_start = time.time()
                print(f"Cadastro iniciado! Posicione o gesto em {REGISTRATION_SECONDS}s...")
    finally:
        cap.release()
        cv2.destroyAllWindows()
        tracker.close()


if __name__ == "__main__":
    main()
