package com.boi.monitor.ui.upi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.Calendar;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.boi.monitor.databinding.FragmentUpiTransactionsBinding;
import com.boi.monitor.model.UpiTransaction;
import com.boi.monitor.ui.dashboard.UpiTransactionAdapter;
import com.boi.monitor.util.FormatUtils;
import com.boi.monitor.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

import android.app.DatePickerDialog;
import java.util.Locale;

/**
 * UpiTransactionFragment
 *
 * Fixed filtering logic for Day, Week, and Month.
 * Added Custom Date selection.
 */
public class UpiTransactionFragment extends Fragment {

    private FragmentUpiTransactionsBinding binding;
    private DashboardViewModel viewModel;
    private UpiTransactionAdapter adapter;
    private String currentFilter = "DAY";
    private final String[] filters = {"Day", "Week", "Month", "Custom Date"};
    private Calendar selectedDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUpiTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        adapter = new UpiTransactionAdapter(new ArrayList<>());
        binding.rvUpiTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpiTransactions.setAdapter(adapter);
        setupFilterSpinner();
        setupCalendar();

        viewModel.getUpiTransactions().observe(getViewLifecycleOwner(), this::updateUI);
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCalendar() {
        binding.btnCalendar.setOnClickListener(v -> {
            Calendar c = selectedDate != null ? selectedDate : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                binding.spinnerFilter.setSelection(3); // "Custom Date"
                if (viewModel != null) updateUI(viewModel.getUpiTransactions().getValue());
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUI(List<UpiTransaction> list) {
        if (binding == null || adapter == null) return;
        if (list == null) return;

        List<UpiTransaction> filtered = new ArrayList<>();
        Calendar now = Calendar.getInstance();

        for (UpiTransaction tx : list) {
            String txDate = tx.getTransactionDate();
            if (txDate == null || txDate.trim().isEmpty()) {
                filtered.add(tx);
                continue;
            }

            try {
                // Support both / and - separators
                String[] parts = txDate.split("[/\\-]");
                if (parts.length != 3) {
                    filtered.add(tx);
                    continue;
                }

                Calendar txCal = Calendar.getInstance();
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int year = Integer.parseInt(parts[2]);
                
                // If year is 2 digits, assume 20xx
                if (year < 100) year += 2000;
                
                txCal.set(year, month, day);

                boolean include = false;
                switch (currentFilter) {
                    case "DAY":
                        include = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                                  now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR);
                        break;
                    case "WEEK":
                        include = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                                  now.get(Calendar.WEEK_OF_YEAR) == txCal.get(Calendar.WEEK_OF_YEAR);
                        break;
                    case "MONTH":
                        include = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                                  now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH);
                        break;
                    case "CUSTOM DATE":
                        if (selectedDate != null) {
                            include = selectedDate.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                                      selectedDate.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR);
                        } else {
                            include = true; // Show all if no date selected but filter is Custom
                        }
                        break;
                }

                if (include) filtered.add(tx);
            } catch (Exception e) {
                filtered.add(tx); // Fallback: show if parse fails
            }
        }

        adapter.updateData(filtered);
        
        if (currentFilter.equals("CUSTOM DATE") && selectedDate != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            binding.tvSelectedDate.setText("Date: " + sdf.format(selectedDate.getTime()));
            binding.tvSelectedDate.setVisibility(View.VISIBLE);
        } else {
            binding.tvSelectedDate.setVisibility(View.GONE);
        }

        long total = 0;
        for (UpiTransaction tx : filtered) total += tx.getAmount();

        binding.tvTotal.setText("Total received: " + FormatUtils.formatCurrencyFromPaise(total));
        binding.tvCount.setText(filtered.size() + " transactions");
        
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvUpiTransactions.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);
        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filters[position].toUpperCase(Locale.ROOT);
                if (viewModel == null) return;
                updateUI(viewModel.getUpiTransactions().getValue());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
