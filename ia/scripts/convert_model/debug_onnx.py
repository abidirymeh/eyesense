import onnxruntime as ort
import numpy as np
import cv2
import os

session = ort.InferenceSession(r"C:\Users\RIMEH\EyeSense\models\yolo26n.onnx")

print("Inputs:")
for inp in session.get_inputs():
    print(f"  {inp.name} → {inp.shape}")

print("Outputs:")
for out in session.get_outputs():
    print(f"  {out.name} → {out.shape}")

img_path = r"C:\Users\RIMEH\EyeSense\dataset\ddd\test\images"
img_file = os.listdir(img_path)[0]
img = cv2.imread(os.path.join(img_path, img_file))
img = cv2.resize(img, (640, 640))
img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
img = img.astype(np.float32) / 255.0
img = np.transpose(img, (2, 0, 1))
img = np.expand_dims(img, axis=0)

outputs = session.run(None, {session.get_inputs()[0].name: img})

print(f"\nNombre de sorties : {len(outputs)}")
for i, out in enumerate(outputs):
    print(f"  Output {i} → shape: {out.shape}")