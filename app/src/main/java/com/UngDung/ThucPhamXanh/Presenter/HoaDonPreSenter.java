package com.UngDung.ThucPhamXanh.Presenter;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.my_interface.HoaDonView;
import com.UngDung.ThucPhamXanh.my_interface.IHoaDon;

public class HoaDonPreSenter implements IHoaDon {

    private HoaDon hoaDonModels;
    private HoaDonView callback;

    public HoaDonPreSenter(HoaDonView callback) {
        this.callback = callback;
        hoaDonModels = new HoaDon(this);
    }
    public  void HandleReadDataHD(){
        hoaDonModels.HandleReadData();
    }
    public void HandleReadDataHDStatus(int i){
        hoaDonModels.HandleReadDataStatus(i);
    }

    /** Lấy đơn của user hiện tại thuộc một trong các trạng thái. */
    public void HandleReadDataHDStatusIn(Integer... statuses){
        java.util.List<Long> list = new java.util.ArrayList<>();
        for (Integer s : statuses) list.add(s.longValue());
        hoaDonModels.HandleReadDataStatusIn(list);
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien,Long type) {
        callback.getDataHD(id,uid,ghichu,diachi,hoten,ngaydat,phuongthuc,sdt,tongtien,type);
    }

    public void HandleGetDataHDDaGiao(){
        hoaDonModels.HandleGetHoaDonDaGiao();
    }

    @Override
    public void OnSucess() {
        callback.OnSucess();

    }

    @Override
    public void OnFail() {
      callback.OnFail();
    }

    public void CapNhatTrangThai(int i,String id) {
        hoaDonModels.HandleUpdateStatusBill(i,id);
    }

    public void HandleReadDataHD(String iduser, int positionStatus) {
        hoaDonModels.HandleReadData(iduser, positionStatus);
    }
}
