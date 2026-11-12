package com.UngDung.ThucPhamXanh.fragment.bill;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.UngDung.ThucPhamXanh.Adapter.HoaDonAdapter;
import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Presenter.HoaDonPreSenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.CTHDActivity;
import com.UngDung.ThucPhamXanh.my_interface.HoaDonView;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;

import java.util.ArrayList;


public class DaGiaoFragment extends Fragment implements HoaDonView {

    private TextView tvNullDaGiao;
    private View view;
    private RecyclerView rcvBill;
    private CardView emptyStateCard;
    private View rcvContainer;
    private HoaDonPreSenter hoaDonPreSenter;
    private HoaDonAdapter hoaDonAdapter;
    private ArrayList<HoaDon> listHoadon;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_da_giao, container, false);

        tvNullDaGiao = view.findViewById(R.id.tv_null_dagiao);
        rcvBill = view.findViewById(R.id.rcv_bill_dagiao);
        emptyStateCard = view.findViewById(R.id.empty_state_dagiao);
        rcvContainer = view.findViewById(R.id.card_recycler_dagiao);
        hoaDonPreSenter = new HoaDonPreSenter(this);
        listHoadon = new ArrayList<>();
        hoaDonPreSenter.HandleReadDataHDStatus(OrderStatus.DA_GIAO);

        return view;
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien, Long type) {
        listHoadon.add(new HoaDon(id,uid,ghichu,diachi,hoten,ngaydat,phuongthuc,sdt,tongtien,type));
        if (listHoadon.size() != 0){
            emptyStateCard.setVisibility(View.GONE);
            if (rcvContainer != null) rcvContainer.setVisibility(View.VISIBLE);
        } else {
            emptyStateCard.setVisibility(View.VISIBLE);
            if (rcvContainer != null) rcvContainer.setVisibility(View.GONE);
        }
        hoaDonAdapter = new HoaDonAdapter();
        hoaDonAdapter.setDataHoadon(listHoadon, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                HoaDon hoaDon = listHoadon.get(pos);
                Intent intent = new Intent(getContext(), CTHDActivity.class);
                intent.putExtra("HD",hoaDon);
                intent.putExtra("CM", true);
                startActivity(intent);
            }
        });
        rcvBill.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvBill.setAdapter(hoaDonAdapter);
    }

    @Override
    public void OnFail() {

    }

    @Override
    public void OnSucess() {

    }
}