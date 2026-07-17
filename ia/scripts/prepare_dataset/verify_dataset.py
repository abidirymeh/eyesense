import os

base_dir = "dataset/ddd_images"

print(" VÉRIFICATION DES IMAGES")
print("="*50)

for split in ['train', 'val', 'test', 'unknown']:
    split_dir = os.path.join(base_dir, split)
    if os.path.exists(split_dir):
        images = [f for f in os.listdir(split_dir) if f.endswith('.jpg')]
        print(f" {split}: {len(images)} images")
    else:
        print(f" {split}: dossier absent")

print("="*50)