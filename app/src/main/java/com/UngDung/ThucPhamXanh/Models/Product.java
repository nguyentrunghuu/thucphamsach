package com.UngDung.ThucPhamXanh.Models;

import androidx.annotation.NonNull;

import com.UngDung.ThucPhamXanh.my_interface.IProduct;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;

public class Product implements Serializable{

    private  String id;
    private  String idsp;
    private  String tensp;
    private  long giatien;
    private  String hinhanh;
    private  String loaisp;
    private  String mota;
    private  long soluong;
    private  String hansudung;
    private  long type;
    private  String trongluong;

    private IProduct callback;
    private FirebaseFirestore db;

    public Product() {
    }

    public Product(IProduct callback) {
        this.callback=callback;
        db=FirebaseFirestore.getInstance();
    }

    public Product(String tensp) {
        this.tensp = tensp;
    }

    public Product(String id, String idsp, String tensp, long giatien, String hinhanh, String loaisp, String mota, long soluong, String hansudung, long type, String trongluong) {
        this.id = id;
        this.idsp = idsp;
        this.tensp = tensp;
        this.giatien = giatien;
        this.hinhanh = hinhanh;
        this.loaisp = loaisp;
        this.mota = mota;
        this.soluong = soluong;
        this.hansudung = hansudung;
        this.type = type;
        this.trongluong = trongluong;
    }

    public Product(String id, String tensp, long giatien, String hinhanh, String loaisp, String mota, long soluong, String hansudung, long type, String trongluong) {
        this.id = id;
        this.tensp = tensp;
        this.giatien = giatien;
        this.hinhanh = hinhanh;
        this.loaisp = loaisp;
        this.mota = mota;
        this.soluong = soluong;
        this.hansudung = hansudung;
        this.type = type;
        this.trongluong = trongluong;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdsp() {
        return idsp;
    }

    public void setIdsp(String idsp) {
        this.idsp = idsp;
    }

    public String getTensp() {
        return tensp;
    }

    public void setTensp(String tensp) {
        this.tensp = tensp;
    }

    public long getGiatien() {
        return giatien;
    }

    public void setGiatien(long giatien) {
        this.giatien = giatien;
    }

    public String getHinhanh() {
        return hinhanh;
    }

    public void setHinhanh(String hinhanh) {
        this.hinhanh = hinhanh;
    }

    public String getLoaisp() {
        return loaisp;
    }

    public void setLoaisp(String loaisp) {
        this.loaisp = loaisp;
    }

    public String getMota() {
        return mota;
    }

    public void setMota(String mota) {
        this.mota = mota;
    }

    public long getSoluong() {
        return soluong;
    }

    public void setSoluong(long soluong) {
        this.soluong = soluong;
    }

    public String getHansudung() {
        return hansudung;
    }

    public void setHansudung(String hansudung) {
        this.hansudung = hansudung;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public String getTrongluong() {
        return trongluong;
    }

    public void setTrongluong(String trongluong) {
        this.trongluong = trongluong;
    }


    public void HandleGetDataProduct(){
        db.collection("SanPham").
                get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots.size()>0){
                    for(QueryDocumentSnapshot d : queryDocumentSnapshots){
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                        callback.getDataProduct(d.getId(),d.getString("tensp"),
                                d.getLong("giatien"),d.getString("hinhanh"),
                                d.getString("loaisp"),d.getString("mota"),
                                d.getLong("soluong"),d.getString("hansudung"),
                                d.getLong("type"),d.getString("trongluong"));

                    }
                }

            }
        });
    }

    public void HandleGetWithIDProduct(String idproduct){
        db.collection("SanPham").document(idproduct).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot d) {
                callback.getDataProduct(idproduct,d.getString("tensp"),
                        d.getLong("giatien"),d.getString("hinhanh"),
                        d.getString("loaisp"),d.getString("mota"),
                        d.getLong("soluong"),d.getString("hansudung"),
                        d.getLong("type"),d.getString("trongluong"));

            }
        });
    }
}
