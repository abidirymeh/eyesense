import { exec } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('🔄 Conversion du modèle YOLO pour le Web...');

const modelPath = path.join(__dirname, 'models', 'yolo26nv_float32.tflite');
if (!fs.existsSync(modelPath)) {
    console.error(`❌ Modèle non trouvé: ${modelPath}`);
    process.exit(1);
}

const destDir = path.join(__dirname, 'public', 'models', 'yolo26n_web');
if (!fs.existsSync(destDir)) {
    fs.mkdirSync(destDir, { recursive: true });
}

const cmd = `npx @tensorflow/tfjs-converter --input_format=tfjs_lite "${modelPath}" "${destDir}"`;

exec(cmd, (error, stdout, stderr) => {
    if (error) {
        console.error(`❌ Erreur: ${error}`);
        console.log('\n📝 Alternative: Utilisez ce script Python:');
        console.log('```python');
        console.log('import tensorflow as tf');
        console.log('import tensorflowjs as tfjs');
        console.log('');
        console.log('# Convertir TFLite en TF.js');
        console.log('converter = tf.lite.TFLiteConverter.from_saved_model("models/yolo26nv_float32.tflite")');
        console.log('tflite_model = converter.convert()');
        console.log('');
        console.log('with open("public/models/yolo26n_web/model.tflite", "wb") as f:');
        console.log('    f.write(tflite_model)');
        console.log('```');
        return;
    }
    console.log('✅ Conversion réussie !');
    console.log(`📁 Fichiers dans: ${destDir}`);
    
    const files = fs.readdirSync(destDir);
    files.forEach(f => {
        const size = fs.statSync(path.join(destDir, f)).size / 1024;
        console.log(`   📄 ${f} (${size.toFixed(1)} KB)`);
    });
});