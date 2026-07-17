package com.example.appeyesense;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private Context context;
    private List<EmergencyContact> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(EmergencyContact contact);
    }

    public ContactAdapter(Context context, List<EmergencyContact> contacts, OnContactClickListener listener) {
        this.context = context;
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvEmail.setText(contact.getEmail());

        String initial = contact.getName().isEmpty() ? "?" : String.valueOf(contact.getName().charAt(0));
        holder.tvAvatar.setText(initial.toUpperCase());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateList(List<EmergencyContact> newList) {
        this.contacts = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvEmail = itemView.findViewById(R.id.tvContactEmail);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
        }
    }
}
