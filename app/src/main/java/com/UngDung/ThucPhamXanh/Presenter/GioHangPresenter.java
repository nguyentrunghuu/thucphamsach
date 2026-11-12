package com.UngDung.ThucPhamXanh.Presenter;

import com.UngDung.ThucPhamXanh.Models.Giohang;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.my_interface.GioHangView;
import com.UngDung.ThucPhamXanh.my_interface.IGioHang;

import java.util.ArrayList;

public class GioHangPresenter implements IGioHang {
    private Giohang giohang;
    private GioHangView callback;

    public GioHangPresenter(GioHangView callback) {
        android.util.Log.d("GioHangPresenter", "📦 Constructor GioHangPresenter được gọi");
        android.util.Log.d("GioHangPresenter", "📦 Callback: " + (callback != null ? "NOT NULL" : "NULL"));
        this.callback = callback;
        giohang = new Giohang(this);
        android.util.Log.d("GioHangPresenter", "📦 Giohang instance: " + (giohang != null ? "NOT NULL" : "NULL"));
    }

    public  void AddCart(String idsp, Long soluong){
        giohang.AddCart(idsp, soluong);
    }

    public  void  HandlegetDataGioHang(){
        giohang.HandlegetDataGioHang();
    }
    public  void  HandleDeleteDataGioHang(String id){
        giohang.HandleDeleteDataGioHang(id);
    }

    public void HandleAddHoaDon(String ghichu, String ngaydat, String diachi, String hoten, String sdt, String phuongthuc, String tongtien, ArrayList<Product> arrayList) {
        giohang.HandleThanhToan(ghichu,ngaydat,diachi,hoten,sdt,phuongthuc,tongtien,arrayList);
    }
    @Override
    public void OnSucess() {
        callback.OnSucess();

    }

    @Override
    public void OnFail() {
        callback.OnFail();
    }

    @Override
    public void getDataSanPham(String id, String idsp,String tensp, Long giatien, String hinhanh, String loaisp, String mota, Long soluong, String hansudung, Long type, String trongluong) {
        android.util.Log.d("GioHangPresenter", "📦 getDataSanPham được gọi từ Giohang");
        android.util.Log.d("GioHangPresenter", "📦 Callback: " + (callback != null ? "NOT NULL" : "NULL"));
        if (callback != null) {
            android.util.Log.d("GioHangPresenter", "📦 Chuyển tiếp đến callback.getDataSanPham");
            callback.getDataSanPham(id,idsp,tensp,giatien,hinhanh,loaisp,mota,soluong,hansudung,type,trongluong);
        } else {
            android.util.Log.e("GioHangPresenter", "❌ Callback là NULL!");
        }
    }

    public void HandlegetDataCTHD(String id) {
        giohang.HandleGetDataCTHD(id);

    }
    public void HandlegetDataCTHD(String id,String uid) {
        try {
            android.util.Log.d("GioHangPresenter", "═══════════════════════════════════════");
            android.util.Log.d("GioHangPresenter", "📦 HandlegetDataCTHD được gọi");
            android.util.Log.d("GioHangPresenter", "📦 ID: " + id + ", UID: " + uid);
            android.util.Log.d("GioHangPresenter", "📦 Giohang: " + (giohang != null ? "NOT NULL" : "NULL"));
            android.util.Log.d("GioHangPresenter", "═══════════════════════════════════════");
            
            if (giohang == null) {
                android.util.Log.e("GioHangPresenter", "❌ Giohang là NULL!");
                return;
            }
            
            android.util.Log.d("GioHangPresenter", "📦 Gọi giohang.HandleGetDataCTHD...");
            giohang.HandleGetDataCTHD(id, uid);
            android.util.Log.d("GioHangPresenter", "📦 Đã gọi giohang.HandleGetDataCTHD");
        } catch (Exception e) {
            android.util.Log.e("GioHangPresenter", "❌ Exception trong HandlegetDataCTHD: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
