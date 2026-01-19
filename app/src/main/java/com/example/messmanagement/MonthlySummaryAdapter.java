package com.example.messmanagement;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MonthlySummaryAdapter extends ArrayAdapter<Member> {

    public interface OnMemberClickListener {
        void onMemberClick(Member member);
    }

    private Context context;
    private List<Member> members;
    private double mealRate;
    private OnMemberClickListener listener;

    public MonthlySummaryAdapter(Context context, List<Member> members,
                                 double mealRate, OnMemberClickListener listener) {
        super(context, 0, members);
        this.context = context;
        this.members = members;
        this.mealRate = mealRate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView,
                        @NonNull ViewGroup parent) {

        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(context)
                    .inflate(R.layout.item_monthly_summary, parent, false);
        }

        TextView tvMemberName = row.findViewById(R.id.tvMemberName);
        TextView tvSummaryLine = row.findViewById(R.id.tvSummaryLine);

        Member m = members.get(position);

        tvMemberName.setText(m.name);

        double shouldPay = mealRate * m.totalMeals;
        double due = shouldPay - m.totalPaid;
        if (due < 0) due = 0;

        tvSummaryLine.setText("Meals: " + m.totalMeals +
                " · Paid: " + (int) m.totalPaid + "৳" +
                " · Due: " + (int) due + "৳");

        tvMemberName.setOnClickListener(v -> {
            if (listener != null) listener.onMemberClick(m);
        });

        return row;
    }
}
