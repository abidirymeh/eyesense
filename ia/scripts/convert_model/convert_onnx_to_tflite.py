import onnx
from onnx2tf import convert
import os

print(" Conversion ONNX → TFLite (manuelle)")
print("=" * 40)

onnx_path = r"C:\Users\RIMEH\EyeSense\models\yolo26nv.onnx"
output_path = r"C:\Users\RIMEH\EyeSense\models\yolo26nv_float32.tflite"

if not os.path.exists(onnx_path):
    print(f" Fichier ONNX non trouvé: {onnx_path}")
    print("   Générez d'abord le fichier ONNX avec:")
    print("   model.export(format='onnx', imgsz=640)")
    exit()

print(f" Fichier ONNX trouvé: {onnx_path}")

try:
    onnx_model = onnx.load(onnx_path)
    print(" Modèle ONNX chargé")
except Exception as e:
    print(f" Erreur chargement ONNX: {e}")
    exit()

try:
    convert(
        onnx_model,
        output_path=output_path,
        output_signaturedefs=True,
        non_verbose=True,
        int8=False,  
    )
    print(f" Conversion réussie !")
    print(f" Fichier: {output_path}")
except Exception as e:
    print(f" Erreur conversion: {e}")
    print("\n Solutions:")
    print("   1. Installez: pip install onnx2tf")
    print("   2. Mettez à jour protobuf: pip install protobuf==3.20.3")