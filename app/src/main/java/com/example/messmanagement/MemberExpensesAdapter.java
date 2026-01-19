package com.example.messmanagement;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MemberExpensesAdapter extends ArrayAdapter<Expense> {

    private final Activity context;
    private final List<Expense> data;

    public MemberExpensesAdapter(Activity context, List<Expense> data) {
        super(context, R.layout.row_member_expense, data);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = context.getLayoutInflater();
        View row = (convertView != null) ?
                convertView : inflater.inflate(R.layout.row_member_expense, parent, false);

        Expense e = data.get(position);

        ((TextView) row.findViewById(R.id.tvExpenseTitle)).setText(e.title);
        ((TextView) row.findViewById(R.id.tvExpenseAmount))
                .setText(String.format(Locale.getDefault(), "%.0f ৳", e.amount));
        ((TextView) row.findViewById(R.id.tvExpenseCategory)).setText(e.category);
        ((TextView) row.findViewById(R.id.tvExpenseDate)).setText(e.date);

        return row;
    }
}
