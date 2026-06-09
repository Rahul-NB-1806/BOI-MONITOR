package com.boi.monitor.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boi.monitor.R;
import com.boi.monitor.model.UpiTransaction;
import com.boi.monitor.util.FormatUtils;

import java.util.List;

/**
 * RecyclerView adapter for UPI transactions in the dashboard.
 */
public class UpiTransactionAdapter extends RecyclerView.Adapter<UpiTransactionAdapter.ViewHolder> {

    private List<UpiTransaction> data;

    public UpiTransactionAdapter(List<UpiTransaction> data) {
        this.data = data;
    }

    public void updateData(List<UpiTransaction> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upi_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpiTransaction tx = data.get(position);
        holder.tvAmount.setText(FormatUtils.formatCurrencyFromPaise(tx.getAmount()));
        holder.tvRef.setText("Ref: " + (tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "—"));
        holder.tvDate.setText(tx.getTransactionDate() != null ? tx.getTransactionDate() : "");
        holder.tvType.setText("UPI CREDIT");
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvRef, tvDate, tvType;

        ViewHolder(View v) {
            super(v);
            tvAmount = v.findViewById(R.id.tv_upi_amount);
            tvRef    = v.findViewById(R.id.tv_upi_ref);
            tvDate   = v.findViewById(R.id.tv_upi_date);
            tvType   = v.findViewById(R.id.tv_upi_type);
        }
    }
}
