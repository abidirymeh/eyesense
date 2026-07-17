from ultralytics import YOLO

model = YOLO("yolo26n.pt")
model.train(
    data="/content/dataset/ddd/data.yaml",
    epochs=100,
    imgsz=640,
    batch=32,
    device=0,
    project="/content/drive/MyDrive/eyesense/runs",
    name="yolo26n-drowsiness",
    save_period=5,
    patience=20,
    cos_lr=True,
    verbose=True,
)