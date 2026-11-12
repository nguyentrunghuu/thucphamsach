package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.Shipper.ShipperHomeActivity;
import com.UngDung.ThucPhamXanh.View.Shipper.ShipperUpdateOrderActivity;
import com.UngDung.ThucPhamXanh.my_interface.IHoaDon;

import java.util.ArrayList;

public class ShipperOrderAdapter extends RecyclerView.Adapter<ShipperOrderAdapter.ViewHolder> {
    private Context context;
    private ArrayList<HoaDon> listOrders;
    private IHoaDon iHoaDon;

    public ShipperOrderAdapter(Context context, ArrayList<HoaDon> listOrders, IHoaDon iHoaDon) {
        this.context = context;
        this.listOrders = listOrders;
        this.iHoaDon = iHoaDon;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shipper_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HoaDon hoaDon = listOrders.get(position);
        
        holder.tvOrderId.setText("Mã đơn: " + hoaDon.getId());
        holder.tvCustomerName.setText("Người nhận: " + hoaDon.getHoten());
        holder.tvAddress.setText("Địa chỉ: " + hoaDon.getDiachi());
        holder.tvPhone.setText("SĐT: " + hoaDon.getSdt());
        holder.tvTotal.setText("Tổng tiền: " + hoaDon.getTongtien() + " VNĐ");
        holder.tvDate.setText("Ngày đặt: " + hoaDon.getNgaydat());
        
        // Hiển thị trạng thái
        long trangthai = hoaDon.getType();
        String statusText = "";
        int statusColor = 0;
        if (trangthai == 5) {
            statusText = "Đã xác nhận";
            statusColor = android.graphics.Color.parseColor("#9C27B0"); // Purple
        } else if (trangthai == 4) {
            statusText = "Đang giao hàng";
            statusColor = context.getResources().getColor(R.color.blue);
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(statusColor);

        holder.cardOrder.setOnClickListener(view -> {
            Intent intent = new Intent(context, ShipperUpdateOrderActivity.class);
            intent.putExtra("order_id", hoaDon.getId());
            intent.putExtra("order_uid", hoaDon.getUid());
            intent.putExtra("customer_name", hoaDon.getHoten());
            intent.putExtra("address", hoaDon.getDiachi());
            intent.putExtra("phone", hoaDon.getSdt());
            intent.putExtra("total", hoaDon.getTongtien());
            intent.putExtra("date", hoaDon.getNgaydat());
            intent.putExtra("status", trangthai);
            intent.putExtra("payment_method", hoaDon.getPhuongthuc());
            intent.putExtra("note", hoaDon.getGhichu());
            // Pass shipper username from ShipperHomeActivity
            if (context instanceof ShipperHomeActivity) {
                String shipperUsername = ((ShipperHomeActivity) context).getShipperUsername();
                if (shipperUsername != null && !shipperUsername.isEmpty()) {
                    intent.putExtra("shipper_username", shipperUsername);
                }
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listOrders != null ? listOrders.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardOrder;
        TextView tvOrderId, tvCustomerName, tvAddress, tvPhone, tvTotal, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOrder = itemView.findViewById(R.id.card_shipper_order);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}

