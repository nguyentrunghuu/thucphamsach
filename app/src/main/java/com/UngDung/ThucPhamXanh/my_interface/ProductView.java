package com.UngDung.ThucPhamXanh.my_interface;

public interface ProductView {
    void OnSucess();

    void OnFail();

    void getDataProduct(String id, String ten, Long gia, String hinhanh, String loaisp, String mota, Long soluong, String hansudung, Long type, String trongluong);
}
