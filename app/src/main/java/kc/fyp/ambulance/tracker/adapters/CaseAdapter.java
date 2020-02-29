package kc.fyp.ambulance.tracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.activities.CasesActivity;
import kc.fyp.ambulance.tracker.model.Case;

public class CaseAdapter extends RecyclerView.Adapter<CaseAdapter.CaseHolder> {
    private List<Case> data;
    private CasesActivity casesActivity;


    public CaseAdapter(CasesActivity ca) {
        data = new ArrayList<>();
        casesActivity = ca;
    }

    public void setData(List<Case> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_case, parent, false);
        return new CaseHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseHolder holder, int position) {
        final Case c = data.get(position);
        holder.date.setText(c.getDate());
        holder.type.setText(c.getType());
        holder.status.setText(c.getStatus());
        holder.address.setText(c.getAddress());

        holder.mainCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                casesActivity.showBottomSheet(c);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class CaseHolder extends RecyclerView.ViewHolder {
        TextView date, type, status, address;
        CardView mainCard;

        CaseHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            type = itemView.findViewById(R.id.type);
            mainCard = itemView.findViewById(R.id.mainCard);
            status = itemView.findViewById(R.id.status);
            address = itemView.findViewById(R.id.address);
        }
    }
}
