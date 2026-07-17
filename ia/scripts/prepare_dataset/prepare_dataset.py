import os
import json
import requests
import concurrent.futures
from pathlib import Path
import yaml
import time
from collections import Counter

# ─── CONFIG ───────────────────────────────────────────────────────────────────
NDJSON_PATH = r"C:\Users\RIMEH\EyeSense\dataset\drivers-drowsiness-detection.ndjson"
DEST_DIR    = r"C:\Users\RIMEH\EyeSense\dataset\ddd"
MAX_WORKERS = 8    
TIMEOUT     = 30    
# ──────────────────────────────────────────────────────────────────────────────


def setup_dirs():
    for split in ["train", "val", "test"]:
        os.makedirs(f"{DEST_DIR}/{split}/images", exist_ok=True)
        os.makedirs(f"{DEST_DIR}/{split}/labels", exist_ok=True)
    print(" Dossiers créés")


def parse_ndjson(path):
    metadata = None
    images = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            obj = json.loads(line)
            if obj["type"] == "dataset":
                metadata = obj
            elif obj["type"] == "image":
                images.append(obj)
    return metadata, images


def download_and_save(img_data):
    url       = img_data["url"]
    filename  = img_data["file"]
    split     = img_data.get("split", "train")
    boxes     = img_data.get("annotations", {}).get("boxes", [])

    if split not in ["train", "val", "test"]:
        split = "train"

    stem       = Path(filename).stem
    img_path   = os.path.join(DEST_DIR, split, "images", stem + ".jpg")
    label_path = os.path.join(DEST_DIR, split, "labels", stem + ".txt")

    if os.path.exists(img_path) and os.path.exists(label_path):
        return "skip"

    try:
        resp = requests.get(url, timeout=TIMEOUT)
        if resp.status_code != 200:
            return f"error_{resp.status_code}"
        with open(img_path, "wb") as f:
            f.write(resp.content)
    except Exception as e:
        return f"error_{e}"

    with open(label_path, "w") as f:
        for box in boxes:
            class_id = int(box[0])
            x, y, w, h = box[1], box[2], box[3], box[4]
            f.write(f"{class_id} {x:.6f} {y:.6f} {w:.6f} {h:.6f}\n")

    return "ok"


def create_yaml(metadata, dest_dir):
    class_names = metadata.get("class_names", {"0": "awake", "1": "drowsy"})
    names = [class_names[str(i)] for i in range(len(class_names))]

    data = {
        "path": dest_dir.replace("\\", "/"),
        "train": "train/images",
        "val":   "val/images",
        "test":  "test/images",
        "nc":    len(names),
        "names": names,
    }

    yaml_path = os.path.join(dest_dir, "data.yaml")
    with open(yaml_path, "w") as f:
        yaml.dump(data, f, default_flow_style=False, allow_unicode=True)

    print(f" data.yaml créé → classes : {names}")
    return yaml_path


def print_stats(dest_dir):
    print(f"\n{'='*50}")
    print("  STATISTIQUES DU DATASET")
    print(f"{'='*50}")
    for split in ["train", "val", "test"]:
        img_dir   = os.path.join(dest_dir, split, "images")
        label_dir = os.path.join(dest_dir, split, "labels")
        if os.path.exists(img_dir):
            n_img   = len(list(Path(img_dir).glob("*.jpg")))
            n_label = len(list(Path(label_dir).glob("*.txt")))
            print(f"  {split:<6} : {n_img:>5} images   {n_label:>5} labels")
    print(f"{'='*50}")


if __name__ == "__main__":
    print("=" * 50)
    print("  PRÉPARATION DATASET DROWSINESS DETECTION")
    print("=" * 50)

    if not os.path.exists(NDJSON_PATH):
        print(f" Fichier introuvable : {NDJSON_PATH}")
        exit(1)

    setup_dirs()

    print(f"\n Lecture du fichier NDJSON...")
    metadata, images = parse_ndjson(NDJSON_PATH)
    print(f" {len(images)} images trouvées")

    splits = Counter(img.get("split", "train") for img in images)
    print(f"   Distribution : {dict(splits)}")

    create_yaml(metadata, DEST_DIR)

    print(f"\n  Téléchargement en cours ({MAX_WORKERS} threads parallèles)...")
    print(f"     Les URLs expirent — lance ce script immédiatement après téléchargement du NDJSON !")
    start = time.time()

    ok = skip = errors = 0

    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(download_and_save, img): img for img in images}

        for i, future in enumerate(concurrent.futures.as_completed(futures)):
            result = future.result()
            if result == "ok":
                ok += 1
            elif result == "skip":
                skip += 1
            else:
                errors += 1

            if (i + 1) % 100 == 0:
                elapsed = time.time() - start
                speed = (i + 1) / elapsed
                remaining = (len(images) - i - 1) / speed
                print(f"  {i+1}/{len(images)} | {ok} ⏭️{skip} {errors} | ~{remaining/60:.1f} min restantes")

    elapsed = time.time() - start
    print(f"\n Terminé en {elapsed/60:.1f} minutes")
    print(f"   Téléchargées : {ok} | Ignorées : {skip} | Erreurs : {errors}")

    print_stats(DEST_DIR)
    print(f"\n Dataset prêt !")
    print(f"   data.yaml : {os.path.join(DEST_DIR, 'data.yaml')}")
