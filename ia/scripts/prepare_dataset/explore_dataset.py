import requests
import os
from PIL import Image
from io import BytesIO
import pandas as pd
import numpy as np

print(" Chargement du fichier...")
df = pd.read_json("dataset/drivers-drowsiness-detection.ndjson", lines=True)

print(f" {len(df)} enregistrements chargés")


df['split'] = df['split'].fillna('unknown')

print(f" Splits disponibles : {df['split'].unique()}")


os.makedirs("dataset/ddd_images/train", exist_ok=True)
os.makedirs("dataset/ddd_images/val", exist_ok=True)
os.makedirs("dataset/ddd_images/test", exist_ok=True)
os.makedirs("dataset/ddd_images/unknown", exist_ok=True)


print("\n Téléchargement des images...")

MAX_IMAGES = 0 

count = 0
errors = 0

for idx, row in df.iterrows():
    if MAX_IMAGES > 0 and count >= MAX_IMAGES:
        break
    
    url = row['url']
    split = str(row['split']) 
    
    img_name = f"img_{idx:04d}.jpg"
    save_path = os.path.join("dataset/ddd_images", split, img_name)
    
    try:
        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            img = Image.open(BytesIO(response.content))
            img.save(save_path)
            count += 1
            if count % 50 == 0:
                print(f"   {count} images téléchargées")
        else:
            errors += 1
            if errors < 5:
                print(f" Erreur {response.status_code} pour l'image {idx}")
    except Exception as e:
        errors += 1
        if errors < 5:
            print(f" Erreur pour l'image {idx} : {e}")

print(f"\n {count} images téléchargées")
print(f" {errors} erreurs")


print("\n Sauvegarde du résumé...")
df_summary = df[['url', 'split']].copy()
df_summary['downloaded'] = df_summary['url'].apply(
    lambda x: os.path.exists(os.path.join("dataset/ddd_images", str(df_summary.loc[df_summary['url'] == x, 'split'].iloc[0]), f"img_{df_summary[df_summary['url'] == x].index[0]:04d}.jpg"))
)
df_summary.to_csv("dataset/ddd_summary_full.csv", index=False)
print(" Résumé sauvegardé dans 'dataset/ddd_summary_full.csv'")