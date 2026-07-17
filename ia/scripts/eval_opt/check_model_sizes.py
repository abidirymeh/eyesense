from ultralytics import YOLO
import os

models_dir = r"C:\Users\RIMEH\EyeSense\models"

print(f"\n{'='*45}")
print(f"  TAILLES DES MODÈLES")
print(f"{'='*45}")
print(f"  {'Fichier':<30} {'Taille':>10}")
print(f"  {'-'*42}")

for model_file in sorted(os.listdir(models_dir)):
    if model_file.endswith(".pt"):
        size = os.path.getsize(os.path.join(models_dir, model_file))
        size_mb = size / (1024 * 1024)
        print(f"  {model_file:<30} {size_mb:>8.1f} MB")

print(f"{'='*45}")