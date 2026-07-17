from ultralytics import YOLO

model = YOLO(r"C:\Users\RIMEH\EyeSense\models\yolo26n.pt")
model.export(format="tflite", imgsz=640)