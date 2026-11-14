package com.UngDung.ThucPhamXanh.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.BannerAdapter;
import com.UngDung.ThucPhamXanh.Adapter.CategoryTimelineAdapter;
import com.UngDung.ThucPhamXanh.Adapter.LoaiProductAdapter;
import com.UngDung.ThucPhamXanh.Adapter.ProductAdapter;
import com.UngDung.ThucPhamXanh.Adapter.PromotionAdapter;
import com.UngDung.ThucPhamXanh.Models.LoaiProduct;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.CartActivity;
import com.UngDung.ThucPhamXanh.View.CategoryActivity;
import com.UngDung.ThucPhamXanh.View.DetailSPActivity;
import com.UngDung.ThucPhamXanh.View.ProductListActivity;
import com.UngDung.ThucPhamXanh.View.SearchActivity;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.UngDung.ThucPhamXanh.my_interface.IClickLoaiProduct;
import com.UngDung.ThucPhamXanh.my_interface.IClickOpenBottomSheet;
import com.UngDung.ThucPhamXanh.ultil.NetworkUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import me.relex.circleindicator.CircleIndicator;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeHome;
    private Product product;

    // Data Product
    private ArrayList<Product> arr_ds_sp, arr_sp_nb, arr_sp_du, arr_sp_hq, arr_sp_mc, arr_sp_yt, arr_sp_lau, arr_sp_gy;
    private ProductAdapter productDSAdapter, productNBAdapter, productDUAdapter, productHQAdapter, productMCAdapter,
            productYTAdapter, productLauAdapter, productGYAdapter;
    private RecyclerView rcvDSSP, rcvSPNoiBat, rcvSPDoUong, rcvSPHQ, rcvSPMC, rcvSPYT, rcvSPLau, rcvSPGY;

    // Banner
    private ArrayList<String> arrayList;
    private ViewPager viewPager;
    private CircleIndicator circleIndicator;
    private BannerAdapter bannerAdapter;

    // Infor User
    private Toolbar toolbarHome;
    private View view;
    private CircleImageView cirAvatarHome;
    private TextView tvNameHome, tvEmailHome;
    public static final int MY_REQUEST_CODE = 10;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Loai Product
    private RecyclerView rcvLoaiProduct;
    private LoaiProductAdapter loaiProductAdapter;
    private List<LoaiProduct> mlistproduct;
    
    // Timeline Categories
    private RecyclerView rcvTimelineCategories;
    private CategoryTimelineAdapter categoryTimelineAdapter;
    
    // Promotions
    private RecyclerView rcvPromotions;
    private ListenerRegistration promotionListener;
    private PromotionAdapter promotionAdapter;
    private ArrayList<String> promotionList;
    private View layoutPromotionsSection; // Container chứa phần khuyến mãi

    // Search Data
    private EditText edtSearchHome;

    private ImageView imgHomeCart, imgHomeCartTop, imgHomeFavorite;

    private TextView tvNumberCart;
    private TextView tvViewAllProducts;
    private TextView tvCartBadge;
    private TextView tvFlashSaleTimer;

    // Realtime listeners
    private ListenerRegistration productListener;
    private ListenerRegistration favoriteListener;
    private ListenerRegistration cartListener;
    private ListenerRegistration cartBadgeListener;
    
    // Flash sale text marquee
    private String promotionText = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        InitWidget();
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbarHome);
        setHasOptionsMenu(true);
        Event();
        if (NetworkUtil.isNetworkConnected(getContext())) {
            LoadInfor();
            Banner();
            InitProduct();
            // Thay thế các method load data cũ bằng realtime listeners
            setupRealtimeListeners();
        } else {
            Log.w("HomeFragment", "⚠️ No network connection");
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup listeners để tránh memory leak
        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }
        if (favoriteListener != null) {
            favoriteListener.remove();
            favoriteListener = null;
        }
        if (cartListener != null) {
            cartListener.remove();
            cartListener = null;
        }
        if (cartBadgeListener != null) {
            cartBadgeListener.remove();
            cartBadgeListener = null;
        }
        if (promotionListener != null) {
            promotionListener.remove();
            promotionListener = null;
        }
        // No need to stop anything for text marquee
        Log.d("HomeFragment", "Realtime listeners cleaned up");
    }

    private void setupRealtimeListeners() {
        Log.d("HomeFragment", "Setting up realtime listeners...");

        // Setup realtime listener cho tất cả sản phẩm
        setupProductRealtimeListener();

        // Setup realtime listener cho favorite
        setupFavoriteRealtimeListener();
        
        // Setup realtime listener cho giỏ hàng
        setupCartRealtimeListener();
        
        // Setup realtime listener cho thông báo
        setupCartBadgeRealtimeListener();
        
        // Setup flash sale promotion text (marquee)
        setupFlashSalePromotionText();
        
        // Setup promotions listener
        setupPromotionsRealtimeListener();
    }

    private void setupProductRealtimeListener() {
        // Listener cho tất cả sản phẩm - sẽ cập nhật tất cả các loại sản phẩm
        productListener = firestore.collection("SanPham")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("HomeFragment", "Error listening to products: " + e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        Log.d("HomeFragment", "Realtime update: " + queryDocumentSnapshots.size() + " products");

                        // Clear all arrays
                        arr_ds_sp.clear();
                        arr_sp_nb.clear();
                        arr_sp_du.clear();
                        arr_sp_hq.clear();
                        arr_sp_mc.clear();
                        arr_sp_yt.clear();
                        arr_sp_lau.clear();
                        arr_sp_gy.clear();

                        // Process all products
                        for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                            if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                            Product product = new Product(d.getId(), d.getString("tensp"),
                                    d.getLong("giatien"), d.getString("hinhanh"),
                                    d.getString("loaisp"), d.getString("mota"),
                                    d.getLong("soluong"), d.getString("hansudung"),
                                    d.getLong("type"), d.getString("trongluong"));

                            // Add to appropriate array based on type
                            switch ((int) product.getType()) {
                                case 1:
                                    arr_ds_sp.add(product);
                                    break;
                                case 2:
                                    arr_sp_nb.add(product);
                                    break;
                                case 3:
                                    arr_sp_du.add(product);
                                    break;
                                case 4:
                                    arr_sp_hq.add(product);
                                    break;
                                case 5:
                                    arr_sp_mc.add(product);
                                    break;
                                case 6:
                                    arr_sp_yt.add(product);
                                    break;
                                case 7:
                                    arr_sp_lau.add(product);
                                    break;
                                case 8:
                                    arr_sp_gy.add(product);
                                    break;
                            }
                        }

                        // Update all adapters
                        updateAllAdapters();
                        
                        // Update timeline adapter - cần load loại sản phẩm trước
                        // Nếu mlistproduct đã có, cập nhật timeline ngay
                        if (mlistproduct != null && !mlistproduct.isEmpty()) {
                            updateTimelineAdapter();
                        } else {
                            // Nếu chưa có loại sản phẩm, load lại
                            loadLoaiProductAndUpdateTimeline();
                        }

                        // Dừng refresh indicator nếu đang refresh
                        if (swipeHome.isRefreshing()) {
                            swipeHome.setRefreshing(false);
                            Log.d("HomeFragment", "Stopped refresh indicator after data update");
                        }
                    }
                });
    }

    private void setupFavoriteRealtimeListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            favoriteListener = firestore.collection("Favorite")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("ALL")
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.e("HomeFragment", "Error listening to favorites: " + e.getMessage());
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            Log.d("HomeFragment", "Realtime update: " + queryDocumentSnapshots.size() + " favorites");
                            // Favorite count có thể được hiển thị ở nơi khác nếu cần
                        }
                    });
        }
    }
    
    private void setupCartRealtimeListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            cartListener = firestore.collection("GioHang")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("ALL")
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.e("HomeFragment", "Error listening to cart: " + e.getMessage());
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            int cartCount = queryDocumentSnapshots.size();
                            Log.d("HomeFragment", "Realtime update: " + cartCount + " items in cart");

                            if (cartCount > 0) {
                                tvNumberCart.setVisibility(View.VISIBLE);
                                tvNumberCart.setText(String.valueOf(cartCount));
                            } else {
                                tvNumberCart.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }
    
    private void setupCartBadgeRealtimeListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Lắng nghe số lượng sản phẩm trong giỏ hàng
            cartBadgeListener = firestore.collection("GioHang")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("ALL")
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.e("HomeFragment", "Error listening to cart: " + e.getMessage());
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            int cartCount = queryDocumentSnapshots.size();
                            Log.d("HomeFragment", "Realtime update: " + cartCount + " items in cart");

                            if (cartCount > 0 && tvCartBadge != null) {
                                tvCartBadge.setVisibility(View.VISIBLE);
                                if (cartCount > 99) {
                                    tvCartBadge.setText("99+");
                                } else {
                                    tvCartBadge.setText(String.valueOf(cartCount));
                                }
                            } else if (tvCartBadge != null) {
                                tvCartBadge.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }
    
    private void setupFlashSalePromotionText() {
        if (tvFlashSaleTimer == null) {
            Log.w("HomeFragment", "tvFlashSaleTimer is null");
            return;
        }
        
        // Load promotion text from Firestore
        firestore.collection("Promotions")
                .document("current_promotion")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("HomeFragment", "Error loading promotion text: " + e.getMessage());
                        // Set default text
                        if (tvFlashSaleTimer != null && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvFlashSaleTimer.setText("Khuyến mãi đang diễn ra...");
                                tvFlashSaleTimer.setSelected(true);
                            });
                        }
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Lấy text khuyến mãi từ nhiều field có thể có
                        String text = documentSnapshot.getString("promotionText");
                        if (text == null || text.trim().isEmpty()) {
                            text = documentSnapshot.getString("text");
                        }
                        if (text == null || text.trim().isEmpty()) {
                            text = documentSnapshot.getString("thongbao");
                        }
                        if (text == null || text.trim().isEmpty()) {
                            text = documentSnapshot.getString("message");
                        }
                        
                        // Nếu không có text, tạo text từ categoryDiscounts
                        if ((text == null || text.trim().isEmpty())) {
                            Map<String, Object> categoryDiscounts = (Map<String, Object>) documentSnapshot.get("categoryDiscounts");
                            if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("🔥 KHUYẾN MÃI: ");
                                int count = 0;
                                for (Map.Entry<String, Object> entry : categoryDiscounts.entrySet()) {
                                    String categoryName = entry.getKey();
                                    Object discountObj = entry.getValue();
                                    
                                    if (discountObj instanceof Map) {
                                        Map<String, Object> discountMap = (Map<String, Object>) discountObj;
                                        Integer discountPercent = discountMap.get("discountPercent") != null 
                                                ? ((Number) discountMap.get("discountPercent")).intValue() : 0;
                                        Integer minProducts = discountMap.get("minProducts") != null 
                                                ? ((Number) discountMap.get("minProducts")).intValue() : 0;
                                        
                                        if (discountPercent > 0) {
                                            if (count > 0) sb.append(" | ");
                                            sb.append(categoryName).append(" giảm ").append(discountPercent).append("%");
                                            if (minProducts > 1) {
                                                sb.append(" (mua từ ").append(minProducts).append(" SP)");
                                            }
                                            count++;
                                        }
                                    }
                                }
                                if (count > 0) {
                                    text = sb.toString();
                                }
                            }
                        }
                        
                        // Set text với marquee
                        promotionText = (text != null && !text.trim().isEmpty()) ? text.trim() : "Khuyến mãi đang diễn ra...";
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (tvFlashSaleTimer != null) {
                                    tvFlashSaleTimer.setText(promotionText);
                                    // Kích hoạt marquee
                                    tvFlashSaleTimer.setSelected(true);
                                    Log.d("HomeFragment", "✅ Set promotion text: " + promotionText);
                                }
                            });
                    }
                } else {
                        // Document không tồn tại, set text mặc định
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                    if (tvFlashSaleTimer != null) {
                                    tvFlashSaleTimer.setText("Khuyến mãi đang diễn ra...");
                                    tvFlashSaleTimer.setSelected(true);
                                }
                            });
                        }
                    }
                });
    }
    
    private void setupPromotionsRealtimeListener() {
        // Không cần load hình ảnh khuyến mãi nữa, chỉ cần text chạy
        // Text đã được load trong setupFlashSalePromotionText()
        // Ẩn section promotions nếu có
        if (layoutPromotionsSection != null) {
            layoutPromotionsSection.setVisibility(View.GONE);
        }
        if (rcvPromotions != null) {
            rcvPromotions.setVisibility(View.GONE);
        }
        Log.d("HomeFragment", "Promotions section hidden - using text marquee instead");
    }

    private void updateAllAdapters() {
        // Update Danh sách sản phẩm (type 1) - đã được thay thế bằng timeline
        if (rcvDSSP != null) {
        if (productDSAdapter != null) {
            productDSAdapter.notifyDataSetChanged();
        } else {
            productDSAdapter = new ProductAdapter(getContext(), arr_ds_sp, 1, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_ds_sp.get(position);
                    TruyenData();
                }
            });
            rcvDSSP.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvDSSP.setAdapter(productDSAdapter);
            }
        }

        // Update Sản phẩm nổi bật (type 2) - đã được thay thế bằng timeline
        if (rcvSPNoiBat != null) {
        if (productNBAdapter != null) {
            productNBAdapter.notifyDataSetChanged();
        } else {
            productNBAdapter = new ProductAdapter(getContext(), arr_sp_nb, 2, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_nb.get(position);
                    TruyenData();
                }
            });
            rcvSPNoiBat.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPNoiBat.setAdapter(productNBAdapter);
            }
        }

        // Update Sản phẩm đồ uống (type 3) - đã được thay thế bằng timeline
        if (rcvSPDoUong != null) {
        if (productDUAdapter != null) {
            productDUAdapter.notifyDataSetChanged();
        } else {
            productDUAdapter = new ProductAdapter(getContext(), arr_sp_du, 3, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_du.get(position);
                    TruyenData();
                }
            });
            rcvSPDoUong.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPDoUong.setAdapter(productDUAdapter);
            }
        }

        // Update các adapter khác tương tự...
        updateOtherAdapters();
    }

    // ===== AI "Gợi ý cho bạn" =====
    private final List<Product> aiPicks = new ArrayList<>();
    private boolean aiPicksRequested;

    /** Gọi AI một lần cho mỗi lần mở trang chủ (kết quả còn được lưu cả ngày, xem HomeAiPicks). */
    private void requestAiPicks(List<Product> allProducts) {
        if (aiPicksRequested || allProducts.isEmpty() || getContext() == null) return;
        aiPicksRequested = true;
        // Danh mục đang bán, không trùng (cùng sản phẩm có thể nằm trong nhiều danh sách theo type)
        java.util.LinkedHashMap<String, Product> unique = new java.util.LinkedHashMap<>();
        for (Product p : allProducts) {
            if (p.getId() != null && p.getSoluong() > 0) unique.putIfAbsent(p.getId(), p);
        }
        com.UngDung.ThucPhamXanh.Helper.HomeAiPicks.load(getContext(), new ArrayList<>(unique.values()), picks -> {
            if (!isAdded()) return;
            aiPicks.clear();
            aiPicks.addAll(picks);
            updateTimelineAdapter();
        });
    }

    private void updateTimelineAdapter() {
        if (rcvTimelineCategories == null) {
            return;
        }
        
        // Tạo danh sách category sections theo loại sản phẩm (loaisp)
        List<CategoryTimelineAdapter.CategorySection> sections = new ArrayList<>();
        
        // Lấy tất cả sản phẩm từ arr_ds_sp (tất cả sản phẩm)
        ArrayList<Product> allProducts = new ArrayList<>();
        allProducts.addAll(arr_ds_sp);
        allProducts.addAll(arr_sp_nb);
        allProducts.addAll(arr_sp_du);
        allProducts.addAll(arr_sp_hq);
        allProducts.addAll(arr_sp_mc);
        allProducts.addAll(arr_sp_yt);
        allProducts.addAll(arr_sp_lau);
        allProducts.addAll(arr_sp_gy);
        
        // Nhóm sản phẩm theo loại sản phẩm (loaisp)
        // Sử dụng danh sách loại sản phẩm từ mlistproduct
        if (mlistproduct != null && !mlistproduct.isEmpty()) {
            for (LoaiProduct loaiProduct : mlistproduct) {
                String categoryName = loaiProduct.getTenloai();
                if (categoryName != null && !categoryName.trim().isEmpty()) {
                    // Lọc sản phẩm theo loại
                    ArrayList<Product> productsInCategory = new ArrayList<>();
                    for (Product product : allProducts) {
                        if (product.getLoaisp() != null && product.getLoaisp().equals(categoryName)) {
                            productsInCategory.add(product);
                        }
                    }
                    
                    // Chỉ thêm section nếu có sản phẩm
                    if (!productsInCategory.isEmpty()) {
                        // Sử dụng type = 2 (nổi bật) cho tất cả, vì không còn phân biệt theo type nữa
                        sections.add(new CategoryTimelineAdapter.CategorySection(categoryName, productsInCategory, 2));
                    }
                }
            }
        }
        
        // Nếu không có loại sản phẩm nào, hoặc không có sản phẩm nào khớp, 
        // có thể thêm các section mặc định dựa trên type (fallback)
        if (sections.isEmpty()) {
            if (!arr_sp_nb.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Sản phẩm nổi bật", arr_sp_nb, 2));
            }
            if (!arr_sp_du.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Đồ uống", arr_sp_du, 3));
            }
            if (!arr_sp_hq.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Hàn Quốc", arr_sp_hq, 4));
            }
            if (!arr_sp_mc.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Mì cay", arr_sp_mc, 5));
            }
            if (!arr_sp_yt.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Yêu thích", arr_sp_yt, 6));
            }
            if (!arr_sp_lau.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Lẩu", arr_sp_lau, 7));
            }
            if (!arr_sp_gy.isEmpty()) {
                sections.add(new CategoryTimelineAdapter.CategorySection("Gợi ý", arr_sp_gy, 8));
            }
        }
        
        // Mục "Gợi ý cho bạn" (AI) nằm trên cùng khi đã có kết quả
        if (!aiPicks.isEmpty()) {
            sections.add(0, new CategoryTimelineAdapter.CategorySection(
                    CategoryTimelineAdapter.AI_SECTION_TITLE, new ArrayList<>(aiPicks), 2));
        }
        requestAiPicks(allProducts);

        // Cập nhật adapter
        if (categoryTimelineAdapter != null) {
            categoryTimelineAdapter.setData(sections);
        } else {
            categoryTimelineAdapter = new CategoryTimelineAdapter(getContext(), new CategoryTimelineAdapter.OnProductClickListener() {
                @Override
                public void onProductClick(Product clickedProduct) {
                    if (clickedProduct != null) {
                        product = clickedProduct;
                        TruyenData();
                    }
                }
            });
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rcvTimelineCategories.setLayoutManager(layoutManager);
            rcvTimelineCategories.setAdapter(categoryTimelineAdapter);
            categoryTimelineAdapter.setData(sections);
        }
    }

    private void updateOtherAdapters() {
        // Sản phẩm Hàn Quốc (type 4)
        if (productHQAdapter != null) {
            productHQAdapter.notifyDataSetChanged();
        } else if (!arr_sp_hq.isEmpty()) {
            productHQAdapter = new ProductAdapter(getContext(), arr_sp_hq, 4, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_hq.get(position);
                    TruyenData();
                }
            });
            rcvSPHQ.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPHQ.setAdapter(productHQAdapter);
        }

        // Sản phẩm Mì Cay (type 5)
        if (productMCAdapter != null) {
            productMCAdapter.notifyDataSetChanged();
        } else if (!arr_sp_mc.isEmpty()) {
            productMCAdapter = new ProductAdapter(getContext(), arr_sp_mc, 5, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_mc.get(position);
                    TruyenData();
                }
            });
            rcvSPMC.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPMC.setAdapter(productMCAdapter);
        }

        // Sản phẩm Yêu thích (type 6)
        if (productYTAdapter != null) {
            productYTAdapter.notifyDataSetChanged();
        } else if (!arr_sp_yt.isEmpty()) {
            productYTAdapter = new ProductAdapter(getContext(), arr_sp_yt, 6, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_yt.get(position);
                    TruyenData();
                }
            });
            rcvSPYT.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPYT.setAdapter(productYTAdapter);
        }

        // Sản phẩm Lẩu (type 7)
        if (productLauAdapter != null) {
            productLauAdapter.notifyDataSetChanged();
        } else if (!arr_sp_lau.isEmpty()) {
            productLauAdapter = new ProductAdapter(getContext(), arr_sp_lau, 7, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_lau.get(position);
                    TruyenData();
                }
            });
            rcvSPLau.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPLau.setAdapter(productLauAdapter);
        }

        // Sản phẩm Gợi ý (type 8)
        if (productGYAdapter != null) {
            productGYAdapter.notifyDataSetChanged();
        } else if (!arr_sp_gy.isEmpty()) {
            productGYAdapter = new ProductAdapter(getContext(), arr_sp_gy, 8, new IClickOpenBottomSheet() {
                @Override
                public void onClickOpenBottomSheet(int position) {
                    product = arr_sp_gy.get(position);
                    TruyenData();
                }
            });
            rcvSPGY.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            rcvSPGY.setAdapter(productGYAdapter);
        }
    }

    private void LoadFavorite() {
        firestore.collection("Favorite").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("ALL")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            int num = 0;
                            Log.d("numCart", "Number: " + queryDocumentSnapshots.size());
                            for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                num++;
                                tvNumberCart.setVisibility(View.VISIBLE);
                                tvNumberCart.setText(num + "");
                            }
                        } else {
                            tvNumberCart.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void replace(Fragment fragment) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, fragment);
        transaction.commit();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NotNull MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_two);
        View view = MenuItemCompat.getActionView(menuItem);

        CircleImageView cirToolbarProfile = view.findViewById(R.id.cir_toolbar_profile);
        firestore.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            if (documentSnapshot != null) {
                                try {
                                    if (documentSnapshot.getString("avatar").length() > 0) {
                                        Picasso.get().load(documentSnapshot.getString("avatar").trim())
                                                .into(cirToolbarProfile);
                                    }
                                } catch (Exception e) {
                                    Log.d("ERROR", e.getMessage());
                                }
                            }
                        }
                    }
                });
        cirToolbarProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Menu two clicked", Toast.LENGTH_SHORT).show();
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        // switch (item.getItemId()){
        // case R.id.menu_one:
        // Toast.makeText(getContext(), "Menu one", Toast.LENGTH_SHORT).show();
        // break;
        // case R.id.menu_two:
        // replace(new ProfileFragment());
        // break;
        //
        // }
        if (item.getItemId() == R.id.menu_one) {
            Intent intent = new Intent(getContext(), CategoryActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_two) {
            replace(new ProfileFragment());
        }
        return super.onOptionsItemSelected(item);
    }

    private void Event() {

        edtSearchHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        // Click vào icon Yêu thích
        if (imgHomeFavorite != null) {
            imgHomeFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Mở FavoriteFragment thông qua MainActivity
                    if (getActivity() instanceof com.UngDung.ThucPhamXanh.MainActivity) {
                        com.UngDung.ThucPhamXanh.MainActivity mainActivity = (com.UngDung.ThucPhamXanh.MainActivity) getActivity();
                        mainActivity.replace(new FavoriteFragment());
                        // Cập nhật bottom navigation để highlight menu item
                        if (mainActivity.getBottomNavigation() != null) {
                            // Có thể thêm logic để highlight menu item yêu thích nếu có
                        }
                    }
                }
            });
        }
        
        // Click vào icon giỏ hàng ở top bar
        if (imgHomeCartTop != null) {
            imgHomeCartTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), CartActivity.class);
                    startActivity(intent);
                }
            });
        }

        imgHomeCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CartActivity.class);
                startActivity(intent);
            }
        });

        swipeHome.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("HomeFragment", "Swipe refresh triggered");

                // Với realtime listeners, data sẽ tự động cập nhật
                // Chỉ cần dừng refresh indicator sau một thời gian ngắn
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeHome.setRefreshing(false);
                        Log.d("HomeFragment", "Swipe refresh completed - data updated via realtime listeners");
                        // getFragmentManager().beginTransaction().detach(HomeFragment.this).attach(HomeFragment.this).commit();
                        // Toast.makeText(getContext(), "Swipe", Toast.LENGTH_SHORT).show();
                    }
                }, 500);

            }
        });

        // View All Products - đã được thay thế bằng timeline với nút "Xem tất cả" trong mỗi section
        if (tvViewAllProducts != null) {
        tvViewAllProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ProductListActivity.class);
                intent.putExtra(ProductListActivity.EXTRA_TYPE, "all");
                intent.putExtra(ProductListActivity.EXTRA_TITLE, "Tất cả sản phẩm");
                startActivity(intent);
            }
        });
        }
        
        // View All Flash Sale/Promotions - Mở danh sách sản phẩm khuyến mãi
        TextView tvViewAllFlashSale = view.findViewById(R.id.tv_view_all_flash_sale);
        if (tvViewAllFlashSale != null) {
            tvViewAllFlashSale.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ProductListActivity.class);
                    intent.putExtra(ProductListActivity.EXTRA_TYPE, "promotion");
                    intent.putExtra(ProductListActivity.EXTRA_TITLE, "Sản phẩm khuyến mãi");
                    startActivity(intent);
                }
            });
            // Bấm vào cả dải khuyến mãi cũng mở danh sách khuyến mãi
            View layoutFlashSale = view.findViewById(R.id.layout_flash_sale);
            if (layoutFlashSale != null) {
                layoutFlashSale.setOnClickListener(v -> tvViewAllFlashSale.performClick());
            }
        }

    }

    private void InitProduct() {
        arr_ds_sp = new ArrayList<>();
        arr_sp_nb = new ArrayList<>();
        arr_sp_du = new ArrayList<>();
        arr_sp_hq = new ArrayList<>();
        arr_sp_mc = new ArrayList<>();
        arr_sp_yt = new ArrayList<>();
        arr_sp_lau = new ArrayList<>();
        arr_sp_gy = new ArrayList<>();
        promotionList = new ArrayList<>();
    }

    // Danh sách Product
    public void GetDataDSSanPham() {
        firestore.collection("SanPham").whereEqualTo("type", 1).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                arr_ds_sp.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productDSAdapter = new ProductAdapter(getContext(), arr_ds_sp, 1,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_ds_sp.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvDSSP.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvDSSP.setAdapter(productDSAdapter);
                        }

                    }
                });
    }

    // Sản phẩm nổi bật
    public void GetDataSPNoiBat() {
        firestore.collection("SanPham").whereEqualTo("type", 2).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                arr_sp_nb.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productNBAdapter = new ProductAdapter(getContext(), arr_sp_nb, 2,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {
                                            // Do something
                                            product = arr_sp_nb.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPNoiBat.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPNoiBat.setAdapter(productNBAdapter);
                        }

                    }
                });
    }

    // Sản phẩm đồ uống
    public void GetDataSPDoUong() {
        firestore.collection("SanPham").whereEqualTo("type", 3).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                // lấy id trên firebase
                                arr_sp_du.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productDUAdapter = new ProductAdapter(getContext(), arr_sp_du, 3,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_sp_du.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPDoUong.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPDoUong.setAdapter(productDUAdapter);
                        }

                    }
                });
    }

    // Sản phẩm Hàn Quốc
    public void GetDataSPHanQuoc() {
        firestore.collection("SanPham").whereEqualTo("type", 4).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                // lấy id trên firebase
                                arr_sp_hq.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productHQAdapter = new ProductAdapter(getContext(), arr_sp_hq, 4,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_sp_hq.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPHQ.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPHQ.setAdapter(productHQAdapter);
                        }

                    }
                });
    }

    // Sản phẩm Mì cay
    public void GetDataSPMiCay() {
        firestore.collection("SanPham").whereEqualTo("type", 5).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                arr_sp_mc.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productMCAdapter = new ProductAdapter(getContext(), arr_sp_mc, 5,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_sp_mc.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPMC.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPMC.setAdapter(productMCAdapter);
                        }

                    }
                });
    }

    // Sản phẩm Yêu thích
    public void GetDataSPYeuThich() {
        firestore.collection("SanPham").whereEqualTo("type", 6).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                // lấy id trên firebase
                                arr_sp_yt.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productYTAdapter = new ProductAdapter(getContext(), arr_sp_yt, 6,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {
                                            // Do something
                                            product = arr_sp_yt.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPYT.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPYT.setAdapter(productYTAdapter);
                        }

                    }
                });
    }

    // Sản phẩm Lẩu
    public void GetDataSPLau() {
        firestore.collection("SanPham").whereEqualTo("type", 7).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                // lấy id trên firebase
                                arr_sp_lau.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productLauAdapter = new ProductAdapter(getContext(), arr_sp_lau, 7,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_sp_lau.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPLau.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                            rcvSPLau.setAdapter(productLauAdapter);
                        }

                    }
                });
    }

    // Sản phẩm Gợi ý
    public void GetDataSPGoiY() {
        firestore.collection("SanPham").whereEqualTo("type", 8).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                // lấy id trên firebase
                                arr_sp_gy.add(new Product(d.getId(), d.getString("tensp"),
                                        d.getLong("giatien"), d.getString("hinhanh"),
                                        d.getString("loaisp"), d.getString("mota"),
                                        d.getLong("soluong"), d.getString("hansudung"),
                                        d.getLong("type"), d.getString("trongluong")));
                            }
                            productGYAdapter = new ProductAdapter(getContext(), arr_sp_gy, 8,
                                    new IClickOpenBottomSheet() {
                                        @Override
                                        public void onClickOpenBottomSheet(int position) {

                                            // Do something
                                            product = arr_sp_gy.get(position);
                                            TruyenData();
                                        }
                                    });
                            rcvSPGY.setLayoutManager(
                                    new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
                            rcvSPGY.setAdapter(productGYAdapter);
                        }

                    }
                });
    }

    private void Banner() {
        arrayList = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Banner").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                    arrayList.add(d.getString("hinhanh"));
                }
                bannerAdapter = new BannerAdapter(getContext(), arrayList, new IClickCTHD() {
                    @Override
                    public void onClickCTHD(int pos) {
                        // String s = arrayList.get(pos);
                        // Toast.makeText(getContext(), "clicked: " + s, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getContext(), SearchActivity.class);
                        startActivity(intent);
                    }
                });
                viewPager.setAdapter(bannerAdapter);
                circleIndicator.setViewPager(viewPager);
                bannerAdapter.registerDataSetObserver(circleIndicator.getDataSetObserver());
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    // 3s sang 1 banner khác
                    public void run() {
                        int k = viewPager.getCurrentItem();
                        if (k >= arrayList.size() - 1) {
                            k = 0;
                        } else {
                            k++;
                        }
                        handler.postDelayed(this, 3000);
                        viewPager.setCurrentItem(k, true);

                    }
                }, 3000);

            }
        });
    }

    private void LoadInfor() {
        tvEmailHome.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail() == null ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getEmail());
        firestore.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            if (documentSnapshot != null) {
                                try {
                                    tvNameHome.setText(documentSnapshot.getString("hoten").length() > 0
                                            ? documentSnapshot.getString("hoten")
                                            : "");

                                    if (documentSnapshot.getString("avatar").length() > 0) {
                                        Picasso.get().load(documentSnapshot.getString("avatar").trim())
                                                .into(cirAvatarHome);
                                    }
                                } catch (Exception e) {
                                    Log.d("ERROR", e.getMessage());
                                }
                            }
                        }
                    }
                });
    }

    private void InitWidget() {
        imgHomeCart = view.findViewById(R.id.img_home_cart);
        swipeHome = view.findViewById(R.id.swipe_home);
        tvNumberCart = view.findViewById(R.id.tv_number_cart);
        imgHomeFavorite = view.findViewById(R.id.img_home_favorite);
        imgHomeCartTop = view.findViewById(R.id.img_home_cart_top);
        tvCartBadge = view.findViewById(R.id.tv_cart_badge);
        tvFlashSaleTimer = view.findViewById(R.id.tv_flash_sale_timer);
        edtSearchHome = view.findViewById(R.id.edt_search_home);

        toolbarHome = view.findViewById(R.id.toolbar_home);
        cirAvatarHome = view.findViewById(R.id.cir_avatar_home);
        tvNameHome = view.findViewById(R.id.tv_name_home);
        tvEmailHome = view.findViewById(R.id.tv_email_home);
        viewPager = view.findViewById(R.id.viewpager);
        circleIndicator = view.findViewById(R.id.circle_indicator);

        rcvLoaiProduct = view.findViewById(R.id.rcv_loai_product);
        rcvPromotions = view.findViewById(R.id.rcv_promotions);
        
        // Khởi tạo danh sách loại sản phẩm
        if (mlistproduct == null) {
            mlistproduct = new ArrayList<>();
        }
        
        // Setup RecyclerView cho loại sản phẩm
        loaiProductAdapter = new LoaiProductAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rcvLoaiProduct.setLayoutManager(linearLayoutManager);

        // Set click listener cho loại sản phẩm
        loaiProductAdapter.setData(mlistproduct, new IClickLoaiProduct() {
            @Override
            public void onClickItemLoaiProduct(int position) {
                if (mlistproduct != null && position < mlistproduct.size()) {
                    LoaiProduct selectedCategory = mlistproduct.get(position);
                    String categoryName = selectedCategory.getTenloai();
                    
                    // Mở ProductListActivity với category được chọn
                    Intent intent = new Intent(getActivity(), ProductListActivity.class);
                    intent.putExtra(ProductListActivity.EXTRA_TYPE, "category");
                    intent.putExtra(ProductListActivity.EXTRA_CATEGORY, categoryName);
                    intent.putExtra(ProductListActivity.EXTRA_TITLE, categoryName);
                    startActivity(intent);
                }
            }
        });
        rcvLoaiProduct.setAdapter(loaiProductAdapter);

        // Load danh sách loại sản phẩm
        loadLoaiProduct();
        
        // Setup promotions RecyclerView
        if (rcvPromotions != null) {
            LinearLayoutManager promotionLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            rcvPromotions.setLayoutManager(promotionLayoutManager);
            Log.d("HomeFragment", "✅ Promotions RecyclerView layout manager set");
        } else {
            Log.e("HomeFragment", "❌ rcvPromotions is null in InitWidget!");
        }
        
        // Tìm container chứa phần khuyến mãi (CardView)
        layoutPromotionsSection = view.findViewById(R.id.layout_promotions_section);
        if (layoutPromotionsSection == null && rcvPromotions != null) {
            // Nếu không tìm thấy bằng ID, tìm parent của rcvPromotions
            View parent = (View) rcvPromotions.getParent();
            while (parent != null && !(parent instanceof androidx.cardview.widget.CardView)) {
                parent = (View) parent.getParent();
            }
            if (parent != null) {
                layoutPromotionsSection = parent;
                Log.d("HomeFragment", "✅ Found promotions section container by parent");
            } else {
                Log.w("HomeFragment", "⚠️ Could not find promotions section container");
            }
        } else if (layoutPromotionsSection != null) {
            Log.d("HomeFragment", "✅ Found promotions section container by ID");
        } else {
            Log.w("HomeFragment", "⚠️ layoutPromotionsSection is null and rcvPromotions is also null");
        }
        
        // Khởi tạo adapter trước (với list rỗng)
        if (rcvPromotions != null && promotionAdapter == null) {
            promotionAdapter = new PromotionAdapter(getContext(), new ArrayList<>(), new IClickCTHD() {
                @Override
                public void onClickCTHD(int pos) {
                    Intent intent = new Intent(getContext(), ProductListActivity.class);
                    intent.putExtra(ProductListActivity.EXTRA_TYPE, "all");
                    intent.putExtra(ProductListActivity.EXTRA_TITLE, "Sản phẩm khuyến mãi");
                    startActivity(intent);
                }
            });
            rcvPromotions.setAdapter(promotionAdapter);
            Log.d("HomeFragment", "✅ PromotionAdapter initialized early");
        }

        // Các RecyclerView cũ đã được thay thế bằng timeline
        // Giữ lại các RecyclerView này nếu vẫn còn trong layout với visibility="gone"
        // rcvDSSP đã bị xóa khỏi layout, không cần tìm nữa
        rcvDSSP = null;
        rcvSPNoiBat = view.findViewById(R.id.rcv_sp_noibat);
        rcvSPDoUong = view.findViewById(R.id.rcv_sp_douong);
        rcvSPHQ = view.findViewById(R.id.rcv_sp_hanquoc);
        rcvSPMC = view.findViewById(R.id.rcv_sp_micay);
        rcvSPYT = view.findViewById(R.id.rcv_sp_yeuthich);
        rcvSPLau = view.findViewById(R.id.rcv_sp_lau);
        rcvSPGY = view.findViewById(R.id.rcv_sp_goiy);

        // Timeline Categories RecyclerView
        rcvTimelineCategories = view.findViewById(R.id.rcv_timeline_categories);

        // tv_view_all_products đã bị xóa khỏi layout, không cần tìm nữa
        tvViewAllProducts = null;

    }

    private void loadLoaiProduct() {
        if (mlistproduct == null) {
        mlistproduct = new ArrayList<>();
        }
        
        // Load loại sản phẩm từ Firestore
        firestore.collection("LoaiProduct")
                .orderBy("tenloai", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        mlistproduct.clear();
                        if (queryDocumentSnapshots.size() > 0) {
                for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                String tenloai = d.getString("tenloai");
                                // Thử lấy ảnh từ nhiều field có thể có
                                String hinhanh = d.getString("hinhanhloai"); // Thử "hinhanhloai" trước
                                if (hinhanh == null || hinhanh.trim().isEmpty()) {
                                    hinhanh = d.getString("hinhanh"); // Thử "hinhanh"
                                }
                                if (hinhanh == null || hinhanh.trim().isEmpty()) {
                                    hinhanh = d.getString("image"); // Thử "image"
                                }
                                if (hinhanh == null || hinhanh.trim().isEmpty()) {
                                    hinhanh = d.getString("url"); // Thử "url"
                                }
                                
                                if (tenloai != null && !tenloai.trim().isEmpty()) {
                                    String finalImageUrl = (hinhanh != null && !hinhanh.trim().isEmpty()) ? hinhanh : "";
                                    mlistproduct.add(new LoaiProduct(tenloai, finalImageUrl));
                                    Log.d("HomeFragment", "Loaded category - Name: " + tenloai + ", Image URL: " + (finalImageUrl.isEmpty() ? "EMPTY" : finalImageUrl));
                                }
                            }
                            // Cập nhật adapter
                            if (loaiProductAdapter != null) {
                                loaiProductAdapter.setData(mlistproduct, new IClickLoaiProduct() {
                                    @Override
                                    public void onClickItemLoaiProduct(int position) {
                                        if (mlistproduct != null && position < mlistproduct.size()) {
                                            LoaiProduct selectedCategory = mlistproduct.get(position);
                                            String categoryName = selectedCategory.getTenloai();
                                            
                                            // Mở ProductListActivity với category được chọn
                                            Intent intent = new Intent(getActivity(), ProductListActivity.class);
                                            intent.putExtra(ProductListActivity.EXTRA_TYPE, "category");
                                            intent.putExtra(ProductListActivity.EXTRA_CATEGORY, categoryName);
                                            intent.putExtra(ProductListActivity.EXTRA_TITLE, categoryName);
                                            startActivity(intent);
                                        }
                                    }
                                });
                            }
                            Log.d("HomeFragment", "Loaded " + mlistproduct.size() + " categories");
                            
                            // Cập nhật timeline adapter sau khi load xong loại sản phẩm
                            updateTimelineAdapter();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                        Log.e("HomeFragment", "Error loading categories: " + e.getMessage());
                        Toast.makeText(getContext(), "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLoaiProductAndUpdateTimeline() {
        // Method này chỉ để đảm bảo timeline được cập nhật sau khi load loại sản phẩm
        // Thực tế loadLoaiProduct() đã gọi updateTimelineAdapter() rồi
        loadLoaiProduct();
    }

    private void TruyenData() {
        Intent intent = new Intent(getContext(), DetailSPActivity.class);
        intent.putExtra("search", product);
        startActivity(intent);
    }

    public TextView getTvNameHome() {
        return tvNameHome;
    }

    public TextView getTvEmailHome() {
        return tvEmailHome;
    }

    public CircleImageView getCirAvatarHome() {
        return cirAvatarHome;
    }
}