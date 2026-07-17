import express from 'express';
import cors from 'cors';
import https from 'https';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import forge from 'node-forge';
import nodemailer from 'nodemailer';
import dotenv from 'dotenv';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('🔐 Génération du certificat SSL...');

function generateCertificate() {
    const pki = forge.pki;
    const keys = pki.rsa.generateKeyPair(2048);
    const cert = pki.createCertificate();

    cert.publicKey = keys.publicKey;
    cert.serialNumber = '01';
    cert.validity.notBefore = new Date();
    cert.validity.notAfter = new Date();
    cert.validity.notAfter.setFullYear(cert.validity.notBefore.getFullYear() + 1);

    const attrs = [
        { name: 'commonName', value: '192.168.1.65' },
        { name: 'countryName', value: 'FR' },
        { name: 'organizationName', value: 'EyeSense' },
        { name: 'organizationalUnitName', value: 'Security' }
    ];

    cert.setSubject(attrs);
    cert.setIssuer(attrs);
    
    cert.setExtensions([
        {
            name: 'subjectAltName',
            altNames: [
                { type: 2, value: '192.168.1.65' },
                { type: 2, value: 'localhost' },
                { type: 2, value: '127.0.0.1' }
            ]
        },
        {
            name: 'basicConstraints',
            cA: true
        },
        {
            name: 'keyUsage',
            keyCertSign: true,
            digitalSignature: true,
            nonRepudiation: true,
            keyEncipherment: true,
            dataEncipherment: true
        },
        {
            name: 'extKeyUsage',
            serverAuth: true,
            clientAuth: true
        }
    ]);

    cert.sign(keys.privateKey);

    return {
        key: pki.privateKeyToPem(keys.privateKey),
        cert: pki.certificateToPem(cert)
    };
}

const cert = generateCertificate();
console.log('✅ Certificat généré avec succès');

console.log('📧 Configuration email...');

const transporter = nodemailer.createTransport({
    host: 'smtp.gmail.com',
    port: 587,
    secure: false,
    auth: {
        user: process.env.EMAIL_USER || 'appeyesense@gmail.com',
        pass: process.env.EMAIL_PASS || 'gvxa kzqt usrc cvbb'
    }
});

transporter.verify((error, success) => {
    if (error) {
        console.error('❌ Erreur email:', error);
    } else {
        console.log('✅ Email configuré avec succès');
    }
});

const app = express();
app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json({ limit: '50mb' }));
app.use(express.static('.'));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

app.get('/health', (req, res) => {
    res.json({
        status: '✅ Serveur HTTPS Sécurisé',
        timestamp: new Date().toISOString(),
        server: 'Node.js HTTPS',
        port: 3444,
        secure: true
    });
});

const CONTACTS_FILE = path.join(__dirname, 'contacts.json');

function getContacts() {
    try {
        const data = fs.readFileSync(CONTACTS_FILE, 'utf8');
        return JSON.parse(data);
    } catch (error) {
        return [];
    }
}

function saveContacts(contacts) {
    fs.writeFileSync(CONTACTS_FILE, JSON.stringify(contacts, null, 2));
}

if (!fs.existsSync(CONTACTS_FILE)) {
    saveContacts([]);
}

app.get('/api/contacts', (req, res) => {
    res.json(getContacts());
});

app.post('/api/contacts', (req, res) => {
    const { name, email } = req.body;
    if (!name || !email) {
        return res.status(400).json({ error: 'Nom et email requis' });
    }
    const contacts = getContacts();
    if (contacts.some(c => c.email === email)) {
        return res.status(400).json({ error: 'Cet email existe déjà' });
    }
    contacts.push({ name, email });
    saveContacts(contacts);
    res.json({ success: true, contact: { name, email } });
});

app.delete('/api/contacts/:id', (req, res) => {
    const id = parseInt(req.params.id);
    const contacts = getContacts();
    if (id < 0 || id >= contacts.length) {
        return res.status(404).json({ error: 'Contact non trouvé' });
    }
    contacts.splice(id, 1);
    saveContacts(contacts);
    res.json({ success: true });
});

app.delete('/api/contacts', (req, res) => {
    saveContacts([]);
    res.json({ success: true });
});

app.post('/api/detect', async (req, res) => {
    try {
        console.log('🔄 Proxy vers YOLO sur http://localhost:5000/detect...');
        
        const response = await fetch('http://localhost:5000/detect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(req.body)
        });
        
        const data = await response.json();
        console.log('✅ Réponse YOLO reçue');
        res.json(data);
        
    } catch (error) {
        console.error('❌ Erreur proxy YOLO:', error.message);
        res.json({
            awake: 0.5,
            drowsy: 0.5,
            isDrowsy: false,
            confidence: 0.5,
            class: -1,
            status: '⚠️ Erreur serveur YOLO'
        });
    }
});

app.post('/api/send-alert', async (req, res) => {
    try {
        const { to, location, timestamp, test, latitude, longitude, mapUrl } = req.body;
        const contacts = getContacts();
        
        let recipients = to;
        if (!recipients || recipients === '') {
            recipients = contacts.map(c => c.email).join(', ');
        }

        if (!recipients) {
            return res.status(400).json({ error: 'Aucun destinataire configuré' });
        }

        console.log(`📧 Envoi d'email à: ${recipients}`);
        console.log(`📍 Localisation: ${location}`);

        const subject = test ? '🧪 TEST - EyeSense Alerte' : '🚨 ALERTE SOMMEIL - EyeSense';
        
        const html = `
            <h2>🚨 Alerte EyeSense</h2>
            <p><strong>${test ? '🧪 TEST' : '⚠️ ALERTE SOMMEIL DÉTECTÉE'}</strong></p>
            <hr>
            <p><strong>📍 Localisation :</strong> ${location || 'Non disponible'}</p>
            ${latitude && longitude ? `<p><strong>🗺️ GPS :</strong> ${latitude}, ${longitude}</p>` : ''}
            ${mapUrl ? `<p><a href="${mapUrl}" target="_blank">🗺️ Voir sur Google Maps</a></p>` : ''}
            <p><strong>🕐 Date/Heure :</strong> ${timestamp || new Date().toLocaleString('fr-FR')}</p>
            <hr>
            <p style="color: red; font-weight: bold;">⚠️ Le conducteur présente des signes de somnolence.</p>
            <p>Veuillez contacter le conducteur immédiatement.</p>
            <br>
            <p style="font-size: 12px; color: gray;">EyeSense - Système de détection de somnolence</p>
        `;

        const info = await transporter.sendMail({
            from: `"EyeSense" <${process.env.EMAIL_USER}>`,
            to: recipients,
            subject: subject,
            html: html
        });

        console.log(`✅ Email envoyé avec succès !`);
        console.log(`📧 Message ID: ${info.messageId}`);

        res.json({
            success: true,
            message: `Alerte envoyée à ${recipients}`,
            messageId: info.messageId,
            location: location
        });

    } catch (error) {
        console.error('❌ Erreur envoi alerte:', error);
        res.status(500).json({ 
            success: false,
            error: error.message,
            details: 'Vérifiez votre connexion internet et les identifiants email'
        });
    }
});


const HTTPS_PORT = 3444;

https.createServer({ key: cert.key, cert: cert.cert }, app).listen(HTTPS_PORT, '0.0.0.0', () => {
    console.log('');
    console.log('='.repeat(60));
    console.log('🔒 SERVEUR HTTPS SÉCURISÉ');
    console.log('='.repeat(60));
    console.log(`📡 HTTPS: https://192.168.1.65:${HTTPS_PORT}`);
    console.log(`📱 Téléphone: https://192.168.1.65:${HTTPS_PORT}`);
    console.log(`📧 Email: ${process.env.EMAIL_USER}`);
    console.log('');
    console.log('⚠️  Acceptez l\'avertissement de sécurité');
    console.log('    (certificat auto-signé)');
    console.log('='.repeat(60));
    console.log('');
    console.log('🔄 Proxy YOLO: http://localhost:5000');
    console.log('');
});

console.log('✅ Serveur prêt !');