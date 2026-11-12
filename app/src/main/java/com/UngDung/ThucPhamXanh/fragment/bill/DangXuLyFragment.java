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

public class DangXuLyFragment extends Fragment implements HoaDonView {

    private TextView tvNullDangxuly;
    private View view;
    private RecyclerView rcvBill;
    private CardView emptyStateCard;
    private View rcvContainer;
    private HoaDonPreSenter hoaDonPreSenter;
    private HoaDonAdapter hoaDonAdapter;
    private ArrayList<HoaDon> listHoadon;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dang_xu_ly, container, false);

        tvNullDangxuly = view.findViewById(R.id.tv_null_dangxuly);
        rcvBill = view.findViewById(R.id.rcv_bill_dxl);
        emptyStateCard = view.findViewById(R.id.empty_state_dangxuly);
        rcvContainer = view.findViewById(R.id.card_recycler_dangxuly);
        hoaDonPreSenter = new HoaDonPreSenter(this);
        listHoadon = new ArrayList<>();
        // Đang xử lý = Đã đặt (1) + Đang xử lý (2) + Đã xác nhận (5)
        hoaDonPreSenter.HandleReadDataHDStatusIn(OrderStatus.DA_DAT, OrderStatus.DANG_XU_LY, OrderStatus.DA_XAC_NHAN);
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
                intent.putExtra("CM", false);
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