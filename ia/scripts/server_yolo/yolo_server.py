from flask import Flask, request, jsonify
import cv2
import numpy as np
import onnxruntime as ort
import base64

app = Flask(__name__)

session = ort.InferenceSession(r"C:\Users\RIMEH\EyeSense\models\yolo26n.onnx")
input_name = session.get_inputs()[0].name
CLASSES = ["awake", "drowsy"]

def preprocess(img):
    img = cv2.resize(img, (640, 640))
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = img.astype(np.float32) / 255.0
    img = np.transpose(img, (2, 0, 1))
    img = np.expand_dims(img, axis=0)
    return img

def postprocess(outputs, conf_threshold=0.35):
    predictions = outputs[0][0] 
    detections = []
    for pred in predictions:
        x1, y1, x2, y2, confidence, class_id = pred
        if confidence > conf_threshold:
            cid = int(class_id)
            if 0 <= cid < len(CLASSES):
                detections.append({
                    "class": CLASSES[cid],
                    "confidence": round(float(confidence), 3),
                    "bbox": [float(x1), float(y1), float(x2), float(y2)]
                })
    return detections

@app.route("/detect", methods=["POST"])
def detect():
    data = request.json
    img_data = base64.b64decode(data["image"])
    img_array = np.frombuffer(img_data, np.uint8)
    img = cv2.imdecode(img_array, cv2.IMREAD_COLOR)

    input_tensor = preprocess(img)
    outputs = session.run(None, {input_name: input_tensor})
    detections = postprocess(outputs)

    status = "drowsy" if any(d["class"] == "drowsy" for d in detections) else "awake"

    return jsonify({
        "detections": detections,
        "status": status,
        "count": len(detections)
    })

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

if __name__ == "__main__":
    print("🚀 API démarrée sur http://0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)