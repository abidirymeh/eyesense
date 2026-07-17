import requests
import base64
import cv2
import os

img_path = r"C:\Users\RIMEH\EyeSense\dataset\ddd\test\images"
img_file = os.listdir(img_path)[0]

print(f"Image testée : {img_file}")

img = cv2.imread(os.path.join(img_path, img_file))
_, buffer = cv2.imencode('.jpg', img)
img_base64 = base64.b64encode(buffer).decode('utf-8')

response = requests.post(
    "http://127.0.0.1:5000/detect",
    json={"image": img_base64}
)

print(f"Status : {response.status_code}")
print(f"Résultat : {response.json()}")