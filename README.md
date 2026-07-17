# 👁️ EyeSense - Détection de somnolence

Système de détection de somnolence en temps réel pour conducteurs, combinant une application mobile Android et une application web.

---

## 📖 Table des matières

- [À propos](#-à-propos)
- [Fonctionnalités](#-fonctionnalités)
- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Utilisation](#-utilisation)
- [Structure du projet](#-structure-du-projet)
- [Performances](#-performances)
- [Auteur](#-auteur)
- [Licence](#-licence)

---

## 📌 À propos

La somnolence au volant est l'une des principales causes d'accidents de la route dans le monde. **EyeSense** est une solution complète qui détecte les signes de fatigue du conducteur en temps réel à l'aide de l'intelligence artificielle.

Le projet repose sur le modèle **YOLO26n** pour la détection des yeux et de la bouche, et se décline en deux applications indépendantes :

- **Application mobile Android** : Détection hors ligne via TensorFlow Lite
- **Application web** : Détection distante via serveur IA

---

## ✨ Fonctionnalités

### Application mobile (Android)
- 📷 Détection en temps réel via la caméra frontale
- 🧠 Inférence locale avec TensorFlow Lite (hors ligne)
- 🔊 Alerte sonore et visuelle
- 📧 Envoi d'emails d'alerte avec localisation GPS
- 📇 Gestion des contacts d'urgence
- 🌙 Mode nuit pour la conduite nocturne

### Application web
- 🌐 Accès depuis n'importe quel navigateur
- 📷 Capture vidéo via WebRTC
- 🧠 Inférence distante via serveur IA
- 📧 Système d'alerte multi-canal

---

## 🛠️ Technologies

| Composant | Technologies |
|---|---|
| **Intelligence Artificielle** | YOLO26n, PyTorch, Ultralytics, TensorFlow Lite |
| **Application mobile** | Java, Android Studio, CameraX, Gson |
| **Application web** | HTML5, CSS3, JavaScript, Node.js, Express, Flask |

---

## ⚙️ Prérequis

### Application mobile
- Android Studio
- SDK Android (minSdk 24, targetSdk 36)
- Smartphone Android avec caméra frontale

### Application web
- Node.js >= 18.0
- Python >= 3.10
- npm ou yarn

---

## 🛠️ Installation

### Application mobile

```bash
# 1. Ouvrir le projet avec Android Studio
cd app_mobile

# 2. Compiler et installer l'APK
# Connecter un smartphone en mode débogage USB
# Cliquer sur "Run" dans Android Studio
```

### Application web

```bash
# 1. Lancer le serveur IA (Python/Flask)
cd ia
pip install -r requirements.txt
python yolo_server.py

# 2. Lancer le serveur web (Node.js)
cd ../app_web
npm install
npm start
```

🚀 Utilisation
Application mobile
Ouvrir l'application EyeSense sur le smartphone

Autoriser l'accès à la caméra et à la localisation

Appuyer sur "Démarrer" pour lancer la détection

En cas de somnolence détectée, une alerte est déclenchée

Application web
Ouvrir https://192.168.1.65:3443 dans un navigateur
Autoriser l'accès à la caméra
Appuyer sur "Start" pour lancer la détection

En cas de somnolence, une alerte sonore est émise

## 📊 Performances

| Métrique | Valeur |
|---|---|
| Accuracy | 98,53 % |
| Précision | 97,8 % |
| Rappel | 98,6 % |
| mAP50 | 99,2 % |
| Taille TFLite (float32) | 9,27 Mo |
| Taille TFLite (float16) | 4,72 Mo |
| Latence mobile | ~125 ms |


## 📬 Contact
**Rimeh ABIDI**  
📧 Email : [rimeh.abidi@enis.tn]  
🔗 GitHub : [github.com/abidirymeh](https://github.com/abidirymeh)  
