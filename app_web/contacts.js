
const API_URL = 'http://localhost:5000';

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.style.display = 'block';
    
    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}

async function getContacts() {
    try {
        const response = await fetch('/api/contacts');
        if (response.ok) {
            return await response.json();
        }
        return [];
    } catch (error) {
        console.error('❌ Erreur chargement contacts:', error);
        return [];
    }
}

async function addContact() {
    const name = document.getElementById('contactName').value.trim();
    const email = document.getElementById('contactEmail').value.trim();

    if (!name || !email) {
        showToast('❌ Veuillez remplir tous les champs', 'error');
        return;
    }

    if (!email.includes('@')) {
        showToast('❌ Email invalide', 'error');
        return;
    }

    try {
        const response = await fetch('/api/contacts', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email })
        });

        if (response.ok) {
            showToast(`✅ Contact "${name}" ajouté !`);
            document.getElementById('contactName').value = '';
            document.getElementById('contactEmail').value = '';
            loadContacts();
        } else {
            const data = await response.json();
            showToast(`❌ ${data.error || 'Erreur'}`, 'error');
        }
    } catch (error) {
        showToast('❌ Erreur de connexion', 'error');
        console.error('❌ Erreur:', error);
    }
}

async function deleteContact(id) {
    if (!confirm('Supprimer ce contact ?')) return;

    try {
        const response = await fetch(`/api/contacts/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showToast('✅ Contact supprimé');
            loadContacts();
        } else {
            showToast('❌ Erreur lors de la suppression', 'error');
        }
    } catch (error) {
        showToast('❌ Erreur de connexion', 'error');
        console.error('❌ Erreur:', error);
    }
}

async function loadContacts() {
    const contacts = await getContacts();
    const container = document.getElementById('contactList');

    if (contacts.length === 0) {
        container.innerHTML = `
            <div class="empty-message">
                <div class="icon">📭</div>
                <p>Aucun contact d'urgence</p>
                <p style="font-size:12px;color:#7b8fa3;">Ajoutez des contacts pour recevoir des alertes</p>
            </div>
        `;
        return;
    }

    let html = '<ul class="contact-list">';
    contacts.forEach((contact, index) => {
        html += `
            <li class="contact-item">
                <div class="info">
                    <div class="name">${escapeHtml(contact.name)}</div>
                    <div class="email">📧 ${escapeHtml(contact.email)}</div>
                </div>
                <div class="actions">
                    <button class="btn btn-danger btn-sm" onclick="deleteContact(${index})">
                        🗑️
                    </button>
                </div>
            </li>
        `;
    });
    html += '</ul>';
    container.innerHTML = html;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function sendTestAlert() {
    const contacts = await getContacts();
    if (contacts.length === 0) {
        showToast('❌ Ajoutez d\'abord des contacts', 'error');
        return;
    }

    const emails = contacts.map(c => c.email).join(', ');
    if (!confirm(`Envoyer une alerte test à ${contacts.length} contact(s) ?`)) return;

    try {
        showToast('📧 Envoi en cours...', 'success');
        
        const response = await fetch('/api/send-alert', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                to: emails,
                location: 'Test - Web',
                timestamp: new Date().toLocaleString('fr-FR'),
                test: true
            })
        });

        if (response.ok) {
            showToast(`✅ Alerte test envoyée à ${contacts.length} contact(s)`);
        } else {
            showToast('❌ Erreur lors de l\'envoi', 'error');
        }
    } catch (error) {
        showToast('❌ Erreur de connexion', 'error');
        console.error('❌ Erreur:', error);
    }
}

async function clearAllContacts() {
    const contacts = await getContacts();
    if (contacts.length === 0) {
        showToast('📭 Aucun contact à supprimer');
        return;
    }

    if (!confirm(`Supprimer tous les ${contacts.length} contacts ?`)) return;

    try {
        const response = await fetch('/api/contacts', {
            method: 'DELETE'
        });

        if (response.ok) {
            showToast('✅ Tous les contacts ont été supprimés');
            loadContacts();
        } else {
            showToast('❌ Erreur', 'error');
        }
    } catch (error) {
        showToast('❌ Erreur de connexion', 'error');
        console.error('❌ Erreur:', error);
    }
}

document.addEventListener('DOMContentLoaded', loadContacts);

document.getElementById('contactEmail').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') addContact();
});
document.getElementById('contactName').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') document.getElementById('contactEmail').focus();
});