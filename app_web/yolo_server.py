
from ultralytics import YOLO
import cv2
import numpy as np
import base64
from flask import Flask, request, jsonify
from flask_cors import CORS
import time

print("=" * 60)
print("🚀 SERVEUR YOLO - EyeSense Web")
print("=" * 60)

# ===== Charger le modèle YOLO (.pt) =====
model_path = r"C:\Users\RIMEH\EyeSense\models\yolo26nv.pt"
print(f"📥 Chargement du modèle: {model_path}")

try:
    model = YOLO(model_path)
    print("✅ Modèle YOLO chargé avec succès !")
    print(f"📋 Classes: {model.names}")
except Exception as e:
    print(f"❌ Erreur: {e}")
    exit(1)

app = Flask(__name__)
CORS(app)

CONF_THRESHOLD = 0.05  
AWAKE_CLASS    = 0
DROWSY_CLASS   = 1
INPUT_SIZE     = 640

@app.route('/detect', methods=['POST'])
def detect():
    try:
        data = request.json
        image_base64 = data.get('imageBase64')

        if not image_base64:
            return jsonify({'error': 'Image manquante'}), 400

        image_data = base64.b64decode(image_base64)
        np_arr = np.frombuffer(image_data, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if img is None:
            return jsonify({'error': 'Image invalide'}), 400

        print(f"📸 Image reçue: {img.shape}")

        img = cv2.flip(img, 1)

        img_resized = cv2.resize(img, (INPUT_SIZE, INPUT_SIZE))

        results = model(img_resized, verbose=False)

        max_awake_conf  = 0.0
        max_drowsy_conf = 0.0
        all_detections  = []

        for r in results:
            if r.boxes is not None:
                for box in r.boxes:
                    conf = float(box.conf[0])
                    cls  = int(box.cls[0])
                    name = model.names[cls]

                    all_detections.append({
                        'class': cls,
                        'name': name,
                        'confidence': conf
                    })

                    print(f"   📦 {name} ({cls}) conf={conf:.3f}")

                    if conf > CONF_THRESHOLD:
                        if cls == AWAKE_CLASS:
                            max_awake_conf = max(max_awake_conf, conf)
                        elif cls == DROWSY_CLASS:
                            max_drowsy_conf = max(max_drowsy_conf, conf)

        is_drowsy = False
        status    = "🔍 Aucune détection"
        final_cls  = -1
        final_conf = 0.0

        if max_drowsy_conf > CONF_THRESHOLD and max_drowsy_conf >= max_awake_conf:
            is_drowsy  = True
            final_cls  = DROWSY_CLASS
            final_conf = max_drowsy_conf
            status     = "⚠️ SOMNOLENCE DÉTECTÉE"
        elif max_awake_conf > CONF_THRESHOLD:
            is_drowsy  = False
            final_cls  = AWAKE_CLASS
            final_conf = max_awake_conf
            status     = "✅ CONDUCTEUR ÉVEILLÉ"

        print(f"📊 Résultat: {status} | conf={final_conf:.3f}")

        return jsonify({
            'awake':      float(max_awake_conf),
            'drowsy':     float(max_drowsy_conf),
            'isDrowsy':   is_drowsy,
            'confidence': float(final_conf),
            'class':      final_cls,
            'status':     status,
            'detections': all_detections,
            'timestamp':  time.time()
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status':       'OK',
        'model_loaded': model is not None,
        'classes':      model.names,
        'timestamp':    time.time()
    })


if __name__ == '__main__':
    print("\n🚀 Serveur YOLO démarré sur http://localhost:5000")
    print("📡 Endpoints:")
    print("   POST /detect  - Détection YOLO")
    print("   GET  /health  - État du serveur")
    print("\n" + "=" * 60)
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)

