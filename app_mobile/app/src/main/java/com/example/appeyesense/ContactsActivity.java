package com.example.appeyesense;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ContactsActivity extends AppCompatActivity {

    private EditText etName, etEmail;
    private Button btnAddContact, btnUpdateContact, btnDeleteContact, btnCancelEdit;
    private RecyclerView rvContacts;
    private ContactManager contactManager;
    private ContactAdapter adapter;
    private TextView tvEditMode;

    private boolean isEditing = false;
    private String editingEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);


        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        btnAddContact = findViewById(R.id.btnAddContact);
        btnUpdateContact = findViewById(R.id.btnUpdateContact);
        btnDeleteContact = findViewById(R.id.btnDeleteContact);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        rvContacts = findViewById(R.id.rvContacts);
        tvEditMode = findViewById(R.id.tvEditMode);

        contactManager = new ContactManager(this);

        adapter = new ContactAdapter(this, contactManager.getContacts(), this::onContactClick);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);

        btnAddContact.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "❌ Email requis", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty()) {
                name = email.split("@")[0];
            }

            EmergencyContact contact = new EmergencyContact(name, email);
            if (contactManager.addContact(contact)) {
                Toast.makeText(this, " Contact ajouté", Toast.LENGTH_SHORT).show();
                clearFields();
                refreshList();
            } else {
                Toast.makeText(this, " Email déjà existant", Toast.LENGTH_SHORT).show();
            }
        });

        btnUpdateContact.setOnClickListener(v -> {
            if (!isEditing) return;

            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (newEmail.isEmpty()) {
                Toast.makeText(this, " Email requis", Toast.LENGTH_SHORT).show();
                return;
            }

            EmergencyContact updatedContact = new EmergencyContact(newName, newEmail);
            if (contactManager.updateContact(editingEmail, updatedContact)) {
                Toast.makeText(this, " Contact modifié", Toast.LENGTH_SHORT).show();
                clearFields();
                cancelEditMode();
                refreshList();
            }
        });

        btnDeleteContact.setOnClickListener(v -> {
            if (!isEditing) return;

            if (contactManager.removeContact(editingEmail)) {
                Toast.makeText(this, " Contact supprimé", Toast.LENGTH_SHORT).show();
                clearFields();
                cancelEditMode();
                refreshList();
            }
        });

        btnCancelEdit.setOnClickListener(v -> {
            clearFields();
            cancelEditMode();
        });
    }

    private void onContactClick(EmergencyContact contact) {
        isEditing = true;
        editingEmail = contact.getEmail();

        etName.setText(contact.getName());
        etEmail.setText(contact.getEmail());

        tvEditMode.setText(" Modification: " + contact.getName());
        tvEditMode.setVisibility(View.VISIBLE);

        btnAddContact.setVisibility(View.GONE);
        btnUpdateContact.setVisibility(View.VISIBLE);
        btnDeleteContact.setVisibility(View.VISIBLE);
        btnCancelEdit.setVisibility(View.VISIBLE);
    }

    private void cancelEditMode() {
        isEditing = false;
        editingEmail = "";
        tvEditMode.setVisibility(View.GONE);
        btnAddContact.setVisibility(View.VISIBLE);
        btnUpdateContact.setVisibility(View.GONE);
        btnDeleteContact.setVisibility(View.GONE);
        btnCancelEdit.setVisibility(View.GONE);
    }

    private void clearFields() {
        etName.setText("");
        etEmail.setText("");
    }

    private void refreshList() {
        adapter.updateList(contactManager.getContacts());
    }
}