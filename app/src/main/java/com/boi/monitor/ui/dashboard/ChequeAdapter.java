package com.boi.monitor.ui.dashboard;

import android.graphics.Color;
import com.boi.monitor.util.FormatUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.boi.monitor.R;
import com.boi.monitor.model.ChequeTransaction;

import java.util.List;

/**
 * RecyclerView adapter for cheque transactions in the dashboard.
 */
public class ChequeAdapter extends RecyclerView.Adapter<ChequeAdapter.ViewHolder> {

    private List<ChequeTransaction> data;

    public ChequeAdapter(List<ChequeTransaction> data) {
        this.data = data;
    }

    public void updateData(List<ChequeTransaction> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cheque_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChequeTransaction tx = data.get(position);
        holder.tvDateTime.setText(
                FormatUtils.formatDateTime(
                        tx.getTimestamp()
                )
        );
        holder.tvChequeNo.setText("Cheque #" + tx.getChequeNumber());
        holder.tvAmount.setText(FormatUtils.formatCurrencyFromPaise(tx.getAmount()));

        holder.tvStatus.setText(tx.getStatus());

        // Color-code status
        int color;
        switch (tx.getStatus()) {
            case ChequeTransaction.STATUS_CLEARED:
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cleared);
                break;
            case ChequeTransaction.STATUS_RETURNED:
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_returned);
                break;
            default: // PRESENTED
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_presented);
                break;
        }
        holder.tvStatus.setTextColor(color);

        if (tx.getAvailableBalance() > 0) {
            holder.tvBalance.setVisibility(View.VISIBLE);
            holder.tvBalance.setText("Avl: " + FormatUtils.formatCurrencyFromPaise(tx.getAvailableBalance()));
        } else {
            holder.tvBalance.setVisibility(View.GONE);
        }

        holder.tvDate.setText(tx.getTransactionDate() != null ? tx.getTransactionDate() : "");
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChequeNo, tvAmount, tvStatus, tvBalance, tvDate, tvDateTime;

        ViewHolder(View v) {
            super(v);
            tvChequeNo = v.findViewById(R.id.tv_cheque_number);
            tvAmount   = v.findViewById(R.id.tv_cheque_amount);
            tvStatus   = v.findViewById(R.id.tv_cheque_status);
            tvBalance  = v.findViewById(R.id.tv_cheque_balance);
            tvDate     = v.findViewById(R.id.tv_cheque_date);
            tvDateTime = v.findViewById(R.id.tvDateTime);
        }
    }
}
