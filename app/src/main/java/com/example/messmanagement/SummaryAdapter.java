package com.example.messmanagement;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class SummaryAdapter extends ArrayAdapter<Summary> {

    private final Activity context;
    private final ArrayList<Summary> data;
    private final LayoutInflater inflater;

    public SummaryAdapter(Activity context, ArrayList<Summary> data) {
        super(context, R.layout.row_summary, data);
        this.context = context;
        this.data = data;
        this.inflater = LayoutInflater.from(context);
    }

    static class VH {
        TextView tvName, tvMeals, tvMealCost, tvOther, tvPaid, tvTotal, tvBalance;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View row = convertView;
        VH h;

        if (row == null) {
            row = inflater.inflate(R.layout.row_summary, parent, false);
            h = new VH();
            h.tvName = row.findViewById(R.id.tvName);
            h.tvMeals = row.findViewById(R.id.tvMeals);
            h.tvMealCost = row.findViewById(R.id.tvMealCost);
            h.tvOther = row.findViewById(R.id.tvOther);
            h.tvPaid = row.findViewById(R.id.tvPaid);
            h.tvTotal = row.findViewById(R.id.tvTotal);
            h.tvBalance = row.findViewById(R.id.tvBalance);
            row.setTag(h);
        } else {
            h = (VH) row.getTag();
        }

        Summary s = data.get(pos);

        h.tvName.setText(s.memberName);

        // Professional wording
        h.tvMeals.setText(String.format(Locale.getDefault(),
                "Meals: Breakfast %d • Lunch %d • Dinner %d",
                s.breakfast, s.lunch, s.dinner));

        h.tvMealCost.setText(String.format(Locale.getDefault(),
                "Meal cost: %.0f ৳", s.mealCost));

        h.tvOther.setText(String.format(Locale.getDefault(),
                "Other expenses: %.0f ৳", s.otherShare));

        h.tvPaid.setText(String.format(Locale.getDefault(),
                "Paid: %.0f ৳", s.paid));

        h.tvTotal.setText(String.format(Locale.getDefault(),
                "Total cost: %.0f ৳", s.totalCost));

        // Balance wording
        if (s.balance > 0) {
            h.tvBalance.setText(String.format(Locale.getDefault(),
                    "Due: %.0f ৳", s.balance));
            h.tvBalance.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else if (s.balance < 0) {
            h.tvBalance.setText(String.format(Locale.getDefault(),
                    "Advance: %.0f ৳", -s.balance));
            h.tvBalance.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            h.tvBalance.setText("Settled");
            h.tvBalance.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        return row;
    }
}
