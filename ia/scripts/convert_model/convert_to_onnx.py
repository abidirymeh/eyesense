from ultralytics import YOLO
import os
import subprocess
import sys

print("=" * 60)
print(" EXPORT YOLO → TFLite (via ONNX) sur Windows")
print("=" * 60)

model_path = "models/yolo26n.pt"

if not os.path.exists(model_path):
    print(f" Modèle non trouvé: {model_path}")
    sys.exit(1)

print(f" Modèle trouvé: {model_path}")
print(f" Taille: {os.path.getsize(model_path) / (1024*1024):.2f} MB")

print("\n" + "=" * 60)
print(" ÉTAPE 1 : Export ONNX")
print("=" * 60)

model = YOLO(model_path)

try:
    model.export(
        format="onnx",
        imgsz=640,
        opset=12
    )
    print(" Export ONNX réussi !")
    
except Exception as e:
    print(f" Erreur export ONNX: {e}")
    sys.exit(1)

onnx_file = None

for f in os.listdir("."):
    if f.endswith(".onnx"):
        onnx_file = f
        break

if not onnx_file:
    for f in os.listdir("models"):
        if f.endswith(".onnx"):
            onnx_file = os.path.join("models", f)
            break

if not onnx_file:
    print(" Fichier ONNX non trouvé")
    sys.exit(1)

print(f" Fichier ONNX: {onnx_file}")
print(f" Taille: {os.path.getsize(onnx_file) / (1024*1024):.2f} MB")

print("\n" + "=" * 60)
print(" ÉTAPE 2 : Conversion ONNX → TFLite")
print("=" * 60)

try:
    import onnx2tf
    print("onnx2tf déjà installé")
except ImportError:
    print(" Installation de onnx2tf...")
    subprocess.run([sys.executable, "-m", "pip", "install", "onnx2tf", "-q"])
    print(" onnx2tf installé")

print("\n Conversion en cours...")

try:
    for f in os.listdir("."):
        if f.endswith(".tflite"):
            os.remove(f)
            print(f"  Ancien fichier supprimé: {f}")
    
    result = subprocess.run([
        sys.executable, "-m", "onnx2tf",
        "-i", onnx_file,
        "-o", "."
    ], capture_output=True, text=True)
    
    if result.returncode != 0:
        print(f" Erreur conversion: {result.stderr}")
        sys.exit(1)
    
    print(" Conversion TFLite réussie !")
    
except Exception as e:
    print(f" Erreur conversion: {e}")
    sys.exit(1)

tflite_files = [f for f in os.listdir(".") if f.endswith(".tflite")]

if tflite_files:
    print("\n" + "=" * 60)
    print(" FICHIERS GÉNÉRÉS")
    print("=" * 60)
    
    for f in tflite_files:
        size = os.path.getsize(f) / (1024*1024)
        print(f" {f} ({size:.2f} MB)")
    
    print("\n" + "=" * 60)
    print(" TEST DU MODÈLE TFLite")
    print("=" * 60)
    
    try:
        import tensorflow as tf
        
        interpreter = tf.lite.Interpreter(model_path=tflite_files[0])
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(" Modèle chargé avec succès !")
        print(f" Input shape : {input_details[0]['shape']}")
        print(f" Input dtype : {input_details[0]['dtype']}")
        print(f" Output shape: {output_details[0]['shape']}")
        print(f" Output dtype: {output_details[0]['dtype']}")
        
        print("\n Export TFLite terminé avec succès !")
        print(f" Modèle prêt : {tflite_files[0]}")
        
    except Exception as e:
        print(f" Erreur test: {e}")
        print("   Le modèle a été généré mais n'a pas pu être chargé.")
else:
    print(" Aucun fichier TFLite généré")
    print("   Vérifiez que onnx2tf est correctement installé")
    sys.exit(1)

print("\n Export terminé ! ")