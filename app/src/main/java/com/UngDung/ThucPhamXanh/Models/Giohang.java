package com.UngDung.ThucPhamXanh.Models;

import android.util.Log;

import androidx.annotation.NonNull;

import com.UngDung.ThucPhamXanh.my_interface.IGioHang;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class Giohang {
    private String id;
    private String id_product;
    private long soluong;

    private IGioHang callback;
    private FirebaseFirestore db;

    public Giohang() {
    }

    public Giohang(String id, String id_product, long soluong) {
        this.id = id;
        this.id_product = id_product;
        this.soluong = soluong;
    }

    public Giohang(String id_product, long soluong) {
        this.id_product = id_product;
        this.soluong = soluong;
    }

    public Giohang(IGioHang callback) {
        Log.d("Giohang", "📦 Constructor Giohang được gọi với callback: " + (callback != null ? "NOT NULL" : "NULL"));
        this.callback = callback;
        db = FirebaseFirestore.getInstance();
        Log.d("Giohang", "📦 FirebaseFirestore instance: " + (db != null ? "NOT NULL" : "NULL"));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId_product() {
        return id_product;
    }

    public void setId_product(String id_product) {
        this.id_product = id_product;
    }

    public long getSoluong() {
        return soluong;
    }

    public void setSoluong(long soluong) {
        this.soluong = soluong;
    }

    public void HandleThanhToan(String ghichu, String ngaydat, String diachi, String hoten, String sdt,
            String phuongthuc, String tongtien, ArrayList<Product> arrayList) {

        Log.d("Giohang", "📦 HandleThanhToan: Bắt đầu thanh toán");
        Log.d("Giohang", "📦 Số lượng sản phẩm: " + arrayList.size());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("ghichu", ghichu);
        hashMap.put("ngaydat", ngaydat);
        hashMap.put("diachi", diachi);
        hashMap.put("sdt", sdt);
        hashMap.put("hoten", hoten);
        hashMap.put("phuongthuc", phuongthuc);
        hashMap.put("tongtien", tongtien);
        hashMap.put("trangthai", 1);
        hashMap.put("UID", FirebaseAuth.getInstance().getCurrentUser().getUid());
        
        db.collection("HoaDon")
                .add(hashMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            String hoaDonId = task.getResult().getId();
                            Log.d("Giohang", "📦 Tạo hóa đơn thành công: " + hoaDonId);
                            
                            // Đếm số lượng chi tiết hóa đơn đã thêm thành công
                            final int[] completedCount = {0};
                            final int totalProducts = arrayList.size();
                            
                            if (totalProducts == 0) {
                                Log.w("Giohang", "⚠️ Danh sách sản phẩm trống");
                                callback.OnFail();
                                return;
                            }
                            
                            for (Product sanPhamModels : arrayList) {
                                HashMap<String, Object> map_chitiet = new HashMap<>();
                                map_chitiet.put("id_hoadon", hoaDonId);
                                map_chitiet.put("id_product", sanPhamModels.getIdsp());
                                map_chitiet.put("soluong", sanPhamModels.getSoluong());
                                
                                Log.d("Giohang", "📦 Thêm chi tiết hóa đơn cho sản phẩm: " + sanPhamModels.getTensp());
                                
                                db.collection("ChitietHoaDon")
                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .collection("ALL").add(map_chitiet)
                                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if (task.isSuccessful()) {
                                                    completedCount[0]++;
                                                    Log.d("Giohang", "📦 Đã thêm chi tiết hóa đơn: " + completedCount[0] + "/" + totalProducts);
                                                    
                                                    // Chỉ xóa giỏ hàng khi TẤT CẢ chi tiết hóa đơn đã được thêm thành công
                                                    if (completedCount[0] == totalProducts) {
                                                        Log.d("Giohang", "📦 Tất cả chi tiết hóa đơn đã được thêm, bắt đầu xóa giỏ hàng...");
                                                        deleteAllCartItems();
                                                    }
                                                } else {
                                                    Log.e("Giohang", "❌ Lỗi thêm chi tiết hóa đơn: " + task.getException().getMessage());
                                                    callback.OnFail();
                                                }
                                            }
                                        });
                            }

                        } else {
                            Log.e("Giohang", "❌ Lỗi tạo hóa đơn: " + task.getException().getMessage());
                            callback.OnFail();
                        }
                    }
                });
    }
    
    private void deleteAllCartItems() {
        Log.d("Giohang", "📦 Bắt đầu xóa tất cả sản phẩm trong giỏ hàng...");
        db.collection("GioHang")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.d("Giohang", "📦 Giỏ hàng đã trống");
                            callback.OnSucess();
                            return;
                        }
                        
                        Log.d("Giohang", "📦 Tìm thấy " + queryDocumentSnapshots.size() + " sản phẩm trong giỏ hàng");
                        final int[] deletedCount = {0};
                        final int totalItems = queryDocumentSnapshots.size();
                        
                        for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                            db.collection("GioHang")
                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("ALL")
                                    .document(q.getId())
                                    .delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                deletedCount[0]++;
                                                Log.d("Giohang", "📦 Đã xóa sản phẩm: " + deletedCount[0] + "/" + totalItems);
                                                
                                                // Chỉ gọi callback khi TẤT CẢ sản phẩm đã được xóa
                                                if (deletedCount[0] == totalItems) {
                                                    Log.d("Giohang", "✅ Đã xóa tất cả sản phẩm trong giỏ hàng");
                                                    callback.OnSucess();
                                                }
                                            } else {
                                                Log.e("Giohang", "❌ Lỗi xóa sản phẩm: " + task.getException().getMessage());
                                                // Vẫn tiếp tục xóa các sản phẩm khác
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Giohang", "❌ Lỗi query giỏ hàng: " + e.getMessage());
                    callback.OnFail();
                });
    }

    // check giỏ hàng đúng id user
    public void AddCart(String idsp, Long soluong) {
        db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL").whereEqualTo("id_product", idsp).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() != 0) {
                            if (queryDocumentSnapshots.size() > 0) {
                                for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                    Long soluongCu = d.getLong("soluong");
                                    // Số lượng cũ lỗi (null/0/âm) thì coi như 0 rồi cộng thêm
                                    long soluong_sp = (soluongCu != null && soluongCu > 0) ? soluongCu : 0;
                                    {
                                        soluong_sp = soluong_sp + soluong;
                                        db.collection("GioHang")
                                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .collection("ALL").document(d.getId()).update("soluong", soluong_sp)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            callback.OnSucess();
                                                        } else {
                                                            callback.OnFail();
                                                        }
                                                    }
                                                });
                                    }
                                }
                            }
                        } else {
                            Giohang hangModels = new Giohang(idsp, soluong);
                            db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("ALL").add(hangModels)
                                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                callback.OnSucess();
                                            } else {
                                                callback.OnFail();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    public void HandlegetDataGioHang() {
        Log.d("Giohang", "Bắt đầu load dữ liệu giỏ hàng...");
        db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        Log.d("Giohang", "Load giỏ hàng thành công, số lượng: " + queryDocumentSnapshots.size());
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                                Log.d("Giohang", "Đang load sản phẩm: " + s.getString("id_product"));
                                db.collection("SanPham").document(s.getString("id_product"))
                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(@NonNull DocumentSnapshot d) {
                                                Log.d("Giohang", "Load sản phẩm thành công: " + d.getString("tensp"));
                                                callback.getDataSanPham(s.getId(), s.getString("id_product"),
                                                        // Sản phẩm đã bị ẩn -> tên null để CartActivity tự gỡ khỏi giỏ
                                                        com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHidden(d) ? null : d.getString("tensp"),
                                                        d.getLong("giatien"), d.getString("hinhanh"),
                                                        d.getString("loaisp"),
                                                        d.getString("mota"),
                                                        s.getLong("soluong"), d.getString("hansudung"),
                                                        d.getLong("type"), d.getString("trongluong"));
                                            }
                                        });
                            }
                        } else {
                            Log.d("Giohang", "Giỏ hàng trống");
                        }
                    }
                });
    }

    public void HandleDeleteDataGioHang(String id) {
        db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL").document(id).delete();
    }

    public void HandleGetDataCTHD(String id) {

        db.collection("ChitietHoaDon").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL").whereEqualTo("id_hoadon", id).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                                Log.d("CHECKED", s.getString("id_product"));
                                db.collection("SanPham").document(s.getString("id_product"))
                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(@NonNull DocumentSnapshot d) {
                                                callback.getDataSanPham(s.getId(), s.getString("id_product"),
                                                        d.getString("tensp"),
                                                        d.getLong("giatien"), d.getString("hinhanh"),
                                                        d.getString("loaisp"),
                                                        d.getString("mota"),
                                                        s.getLong("soluong"), d.getString("hansudung"),
                                                        1l, d.getString("trongluong"));
                                            }
                                        });
                            }
                        }

                    }
                });
    }

    public void HandleGetDataCTHD(String id, String uid) {
        try {
            Log.d("Giohang", "═══════════════════════════════════════");
            Log.d("Giohang", "📦 HandleGetDataCTHD: Bắt đầu load chi tiết hóa đơn");
            Log.d("Giohang", "📦 ID hóa đơn: " + id);
            Log.d("Giohang", "📦 UID: " + uid);
            Log.d("Giohang", "📦 Callback: " + (callback != null ? "NOT NULL" : "NULL"));
            Log.d("Giohang", "📦 DB: " + (db != null ? "NOT NULL" : "NULL"));
            Log.d("Giohang", "═══════════════════════════════════════");

        if (uid == null || uid.isEmpty()) {
            Log.e("Giohang", "❌ UID là null hoặc rỗng, không thể load chi tiết hóa đơn");
            return;
        }

        if (id == null || id.isEmpty()) {
            Log.e("Giohang", "❌ ID hóa đơn là null hoặc rỗng, không thể load chi tiết hóa đơn");
            return;
        }

        Log.d("Giohang", "📦 Querying ChitietHoaDon collection...");
        Log.d("Giohang", "📦 Path: ChitietHoaDon/" + uid + "/ALL where id_hoadon=" + id);
        
        db.collection("ChitietHoaDon").document(uid)
                .collection("ALL").whereEqualTo("id_hoadon", id).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        Log.d("Giohang", "📦 Query thành công, tìm thấy " + queryDocumentSnapshots.size()
                                + " chi tiết hóa đơn");
                        
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.w("Giohang", "⚠️ Không tìm thấy chi tiết hóa đơn nào với id_hoadon=" + id);
                            Log.w("Giohang", "⚠️ Kiểm tra lại đường dẫn: ChitietHoaDon/" + uid + "/ALL");
                            return;
                        }
                        
                        Log.d("Giohang", "📦 Bắt đầu load " + queryDocumentSnapshots.size() + " sản phẩm...");
                        int count = 0;
                        for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                            count++;
                            Log.d("Giohang", "📦 [" + count + "] Chi tiết hóa đơn: " + s.getString("id_product")
                                    + " - Số lượng: " + s.getLong("soluong"));
                            String productId = s.getString("id_product");
                            if (productId != null && !productId.isEmpty()) {
                                Log.d("Giohang", "📦 [" + count + "] Đang load sản phẩm: " + productId);
                                db.collection("SanPham").document(productId)
                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(@NonNull DocumentSnapshot d) {
                                                if (d.exists() && d.getData() != null) {
                                                    Log.d("Giohang",
                                                            "📦 Load sản phẩm thành công: " + d.getString("tensp"));
                                                    Log.d("Giohang", "📦 Gọi callback.getDataSanPham...");
                                                    if (callback != null) {
                                                        callback.getDataSanPham(s.getId(), productId,
                                                                d.getString("tensp"),
                                                                d.getLong("giatien"), d.getString("hinhanh"),
                                                                d.getString("loaisp"),
                                                                d.getString("mota"),
                                                                s.getLong("soluong"), d.getString("hansudung"),
                                                                1l, d.getString("trongluong"));
                                                        Log.d("Giohang", "📦 Đã gọi callback.getDataSanPham");
                                                    } else {
                                                        Log.e("Giohang", "❌ Callback là NULL khi gọi getDataSanPham!");
                                                    }
                                                } else {
                                                    Log.e("Giohang", "❌ Sản phẩm không tồn tại: " + productId);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Giohang", "❌ Lỗi load sản phẩm " + productId + ": " + e.getMessage());
                                            e.printStackTrace();
                                        });
                            } else {
                                Log.e("Giohang", "❌ ID sản phẩm là null hoặc rỗng trong chi tiết hóa đơn");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Giohang", "❌ Lỗi query chi tiết hóa đơn: " + e.getMessage());
                    e.printStackTrace();
                });
        } catch (Exception e) {
            Log.e("Giohang", "❌ Exception trong HandleGetDataCTHD: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
