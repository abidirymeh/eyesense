from ultralytics import YOLO

model = YOLO(r"C:\Users\RIMEH\EyeSense\models\archive\yolo11nv2.pt")

metrics = model.val(
    data=r"C:\Users\RIMEH\EyeSense\dataset\ddd\data.yaml",
    split="test",    
    device="cpu",
    imgsz=640,
    batch=8,
)

print("\n" + "="*50)
print("  RÉSULTATS SUR TON DATASET")
print("="*50)
print(f"  mAP50     : {metrics.box.map50:.4f}  ({metrics.box.map50*100:.1f}%)")
print(f"  mAP50-95  : {metrics.box.map:.4f}  ({metrics.box.map*100:.1f}%)")
print(f"  Precision : {metrics.box.mp:.4f}  ({metrics.box.mp*100:.1f}%)")
print(f"  Recall    : {metrics.box.mr:.4f}  ({metrics.box.mr*100:.1f}%)")
print("="*50)

# Par classe
print("\n  Par classe :")
for i, name in enumerate(model.names.values()):
    print(f"  {name:<10} : mAP50 = {metrics.box.ap50[i]*100:.1f}%")