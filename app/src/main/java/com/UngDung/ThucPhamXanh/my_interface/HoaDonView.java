package com.UngDung.ThucPhamXanh.my_interface;

public interface HoaDonView {
    void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien,Long type);

    void OnFail();

    void OnSucess();
}
