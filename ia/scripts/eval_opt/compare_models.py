from ultralytics import YOLO
import os
import pandas as pd

models_dir = r"C:\Users\RIMEH\EyeSense\models\yolo26v"
data_yaml  = r"C:\Users\RIMEH\EyeSense\dataset\ddd\data.yaml"

results = []

for model_file in os.listdir(models_dir):
    if not model_file.endswith(".pt"):
        continue

    print(f"\n Évaluation de {model_file}...")
    try:
        model = YOLO(os.path.join(models_dir, model_file))
        
        if list(model.names.values()) != ['awake', 'drowsy']:
            print(f"    Ignoré → classes : {list(model.names.values())}")
            continue

        metrics = model.val(
            data=data_yaml,
            split="test",
            device="cpu",
            imgsz=640,
            batch=8,
            verbose=False,
        )

        results.append({
            "Modèle"    : model_file,
            "mAP50"     : f"{metrics.box.map50*100:.1f}%",
            "mAP50-95"  : f"{metrics.box.map*100:.1f}%",
            "Precision" : f"{metrics.box.mp*100:.1f}%",
            "Recall"    : f"{metrics.box.mr*100:.1f}%",
        })

    except Exception as e:
        print(f"   Erreur : {e}")

print("\n" + "="*70)
print("  COMPARAISON DES MODÈLES")
print("="*70)
df = pd.DataFrame(results)
print(df.to_string(index=False))

df.to_csv(r"C:\Users\RIMEH\EyeSense\output\comparaison_modeles.csv", index=False)
print("\n Résultats sauvegardés dans output/comparaison_modeles.csv")