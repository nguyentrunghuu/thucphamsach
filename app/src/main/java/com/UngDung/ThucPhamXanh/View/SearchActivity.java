package com.UngDung.ThucPhamXanh.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.LichSuSearchAdapter;
import com.UngDung.ThucPhamXanh.Adapter.SearchAdapter;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.Presenter.StoryPresenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.StoryView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/** Tìm kiếm sản phẩm: tìm không phân biệt dấu + bộ lọc (danh mục, giá, còn hàng) + sắp xếp (phù hợp, bán chạy, giá). */
public class SearchActivity extends AppCompatActivity implements StoryView {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int MAX_HISTORY = 10;

    // Khoảng giá chọn nhanh (đồng)
    private static final long[][] PRICE_RANGES = {
            {0, Long.MAX_VALUE},      // Tất cả
            {0, 19_999},              // Dưới 20k
            {20_000, 50_000},
            {50_000, 100_000},
            {100_001, Long.MAX_VALUE} // Trên 100k
    };
    private static final int[] PRICE_CHIP_IDS = {
            R.id.chip_price_all, R.id.chip_price_1, R.id.chip_price_2, R.id.chip_price_3, R.id.chip_price_4
    };
    private static final String[] SORT_LABELS = {"Phù hợp nhất", "Bán chạy", "Giá thấp → cao", "Giá cao → thấp"};
    private static final int[] SORT_CHIP_IDS = {
            R.id.chip_sort_relevant, R.id.chip_sort_bestseller, R.id.chip_sort_price_asc, R.id.chip_sort_price_desc
    };

    private SwipeRefreshLayout swipeSearch;
    private ImageView imgMic, imgQRCode, imgBackSearch;
    private SearchView searchView;
    private RecyclerView rcvSearch, rcvLichSuSearch;
    private TextView btnSort, btnFilter, tvResultCount;
    private ChipGroup chipGroupQuickSort;
    private View layoutHistory, layoutEmpty;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private SearchAdapter adapter;
    private StoryPresenter storyPresenter;
    private LichSuSearchAdapter lichSuSearchAdapter;
    private final ArrayList<String> mlistStory = new ArrayList<>();
    private final LinkedHashSet<String> storySet = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initWidget();
        initEvents();
        loadProducts();
        loadHistory();
    }

    // ================== KHỞI TẠO ==================

    private void initWidget() {
        swipeSearch = findViewById(R.id.swipe_search);
        imgQRCode = findViewById(R.id.img_qrcode);
        imgMic = findViewById(R.id.img_mic);
        imgBackSearch = findViewById(R.id.img_back_search);
        searchView = findViewById(R.id.search_view);
        rcvSearch = findViewById(R.id.rcv_search_monan);
        rcvLichSuSearch = findViewById(R.id.rcv_lichsu);
        btnSort = findViewById(R.id.btn_sort_search);
        btnFilter = findViewById(R.id.btn_filter_search);
        tvResultCount = findViewById(R.id.tv_result_count);
        chipGroupQuickSort = findViewById(R.id.chip_group_quick_sort);
        layoutHistory = findViewById(R.id.layout_history_search);
        layoutEmpty = findViewById(R.id.layout_search_empty);

        adapter = new SearchAdapter(this, product -> {
            Intent intent = new Intent(SearchActivity.this, DetailSPActivity.class);
            intent.putExtra("search", product);
            startActivity(intent);
        });
        rcvSearch.setLayoutManager(new LinearLayoutManager(this));
        rcvSearch.setAdapter(adapter);

        lichSuSearchAdapter = new LichSuSearchAdapter();
        lichSuSearchAdapter.setdata(this, mlistStory, pos -> {
            if (pos >= 0 && pos < mlistStory.size()) searchView.setQuery(mlistStory.get(pos), true);
        });
        rcvLichSuSearch.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rcvLichSuSearch.setAdapter(lichSuSearchAdapter);

        storyPresenter = new StoryPresenter(this);
        updateUi();
    }

    private void initEvents() {
        imgBackSearch.setOnClickListener(v -> finish());

        imgMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bạn muốn tìm gì?");
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            } catch (Exception e) {
                Toast.makeText(this, "Thiết bị không hỗ trợ nhận giọng nói", Toast.LENGTH_SHORT).show();
            }
        });

        imgQRCode.setOnClickListener(v -> {
            ScanOptions scanOptions = new ScanOptions();
            scanOptions.setPrompt("Đưa mã QR của sản phẩm vào khung hình");
            scanOptions.setBeepEnabled(true);
            scanOptions.setOrientationLocked(true);
            scanOptions.setCaptureActivity(CaptureAct.class);
            barLauncher.launch(scanOptions);
        });

        swipeSearch.setOnRefreshListener(() -> {
            loadProducts();
            loadHistory();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                saveHistory(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.setQuery(newText);
                updateUi();
                return true;
            }
        });

        // Sắp xếp nhanh bằng chip
        chipGroupQuickSort.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < SORT_CHIP_IDS.length; i++) {
                if (SORT_CHIP_IDS[i] == checkedId) {
                    applySort(i);
                    break;
                }
            }
        });

        // Nút "Sắp xếp" -> menu
        btnSort.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, btnSort);
            for (int i = 0; i < SORT_LABELS.length; i++) menu.getMenu().add(0, i, i, SORT_LABELS[i]);
            menu.setOnMenuItemClickListener(item -> {
                chipGroupQuickSort.check(SORT_CHIP_IDS[item.getItemId()]); // gọi applySort qua listener
                return true;
            });
            menu.show();
        });

        btnFilter.setOnClickListener(v -> showFilterSheet());

        View btnClearEmpty = findViewById(R.id.btn_clear_filter_empty);
        btnClearEmpty.setOnClickListener(v -> {
            adapter.setFilters(null, 0, Long.MAX_VALUE, false);
            updateUi();
        });
    }

    // ================== DỮ LIỆU ==================

    /** Tải tất cả sản phẩm đang bán + số đã bán (field "daban") trong MỘT lần đọc. */
    private void loadProducts() {
        firestore.collection("SanPham").get()
                .addOnSuccessListener(snapshots -> {
                    List<Product> products = new ArrayList<>();
                    Map<String, Long> sold = new HashMap<>();
                    for (QueryDocumentSnapshot d : snapshots) {
                        if (ProductVisibility.isHiddenFromShop(d)) continue;
                        products.add(new Product(d.getId(), d.getString("tensp"),
                                longOrZero(d.getLong("giatien")), d.getString("hinhanh"),
                                d.getString("loaisp"), d.getString("mota"),
                                longOrZero(d.getLong("soluong")), d.getString("hansudung"),
                                longOrZero(d.getLong("type")), d.getString("trongluong")));
                        Long daban = d.getLong("daban");
                        if (daban != null) sold.put(d.getId(), daban);
                    }
                    adapter.setProducts(products, sold);
                    swipeSearch.setRefreshing(false);
                    updateUi();
                })
                .addOnFailureListener(e -> {
                    swipeSearch.setRefreshing(false);
                    Toast.makeText(this, "Không tải được sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static long longOrZero(Long v) {
        return v == null ? 0 : v;
    }

    private void loadHistory() {
        storySet.clear();
        mlistStory.clear();
        lichSuSearchAdapter.notifyDataSetChanged();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            storyPresenter.HandleGetStory(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }

    private void saveHistory(String text) {
        if (TextUtils.isEmpty(text) || text.trim().isEmpty()) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("noidungtimkiem", text.trim());
        firestore.collection("LichSuTimKiem").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Story").add(hashMap);
        addHistoryItem(text.trim(), true);
    }

    /** Lịch sử: không trùng lặp, mới nhất đứng đầu, tối đa MAX_HISTORY mục. */
    private void addHistoryItem(String text, boolean newest) {
        if (TextUtils.isEmpty(text)) return;
        storySet.remove(text);
        List<String> ordered = new ArrayList<>(storySet);
        if (newest) ordered.add(0, text); else ordered.add(text);
        storySet.clear();
        for (String s : ordered) {
            if (storySet.size() >= MAX_HISTORY) break;
            storySet.add(s);
        }
        mlistStory.clear();
        mlistStory.addAll(storySet);
        lichSuSearchAdapter.notifyDataSetChanged();
        updateUi();
    }

    @Override
    public void getDataStory(String noidung) {
        // Firestore trả theo thứ tự thêm vào (cũ -> mới) => mỗi mục mới đẩy lên đầu
        addHistoryItem(noidung, true);
    }

    // ================== SẮP XẾP / LỌC ==================

    private void applySort(int sort) {
        adapter.setSort(sort);
        btnSort.setText(SORT_LABELS[sort]);
        btnSort.setActivated(sort != SearchAdapter.SORT_RELEVANT);
        updateUi();
    }

    private void showFilterSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(R.layout.bottom_sheet_search_filter);
        View container = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (container != null) container.setBackgroundResource(android.R.color.transparent);

        ChipGroup groupCategory = sheet.findViewById(R.id.chip_group_category);
        ChipGroup groupPrice = sheet.findViewById(R.id.chip_group_price);
        View layoutCustom = sheet.findViewById(R.id.layout_price_custom);
        EditText edtMin = sheet.findViewById(R.id.edt_price_min);
        EditText edtMax = sheet.findViewById(R.id.edt_price_max);
        SwitchMaterial switchStock = sheet.findViewById(R.id.switch_in_stock);
        Button btnReset = sheet.findViewById(R.id.btn_filter_reset);
        Button btnApply = sheet.findViewById(R.id.btn_filter_apply);

        // --- Danh mục: lấy từ chính danh sách sản phẩm (chỉ hiện danh mục có hàng) ---
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product p : adapter.getAllProducts()) {
            if (!TextUtils.isEmpty(p.getLoaisp())) categories.add(p.getLoaisp());
        }
        Chip all = makeChip("Tất cả");
        groupCategory.addView(all);
        Chip toCheck = all;
        for (String c : categories) {
            Chip chip = makeChip(c);
            chip.setTag(c);
            groupCategory.addView(chip);
            if (c.equals(adapter.getCategory())) toCheck = chip;
        }
        groupCategory.check(toCheck.getId());

        // --- Khoảng giá ---
        long curMin = adapter.getMinPrice(), curMax = adapter.getMaxPrice();
        int priceChip = R.id.chip_price_custom;
        for (int i = 0; i < PRICE_RANGES.length; i++) {
            if (PRICE_RANGES[i][0] == curMin && PRICE_RANGES[i][1] == curMax) priceChip = PRICE_CHIP_IDS[i];
        }
        groupPrice.check(priceChip);
        if (priceChip == R.id.chip_price_custom) {
            layoutCustom.setVisibility(View.VISIBLE);
            if (curMin > 0) edtMin.setText(String.valueOf(curMin));
            if (curMax != Long.MAX_VALUE) edtMax.setText(String.valueOf(curMax));
        }
        groupPrice.setOnCheckedChangeListener((g, id) ->
                layoutCustom.setVisibility(id == R.id.chip_price_custom ? View.VISIBLE : View.GONE));

        switchStock.setChecked(adapter.isInStockOnly());

        btnReset.setOnClickListener(v -> {
            groupCategory.check(all.getId());
            groupPrice.check(R.id.chip_price_all);
            edtMin.setText("");
            edtMax.setText("");
            switchStock.setChecked(false);
        });

        btnApply.setOnClickListener(v -> {
            Chip checkedCat = groupCategory.findViewById(groupCategory.getCheckedChipId());
            String category = checkedCat != null && checkedCat.getTag() != null ? (String) checkedCat.getTag() : null;

            long min = 0, max = Long.MAX_VALUE;
            int checkedPrice = groupPrice.getCheckedChipId();
            if (checkedPrice == R.id.chip_price_custom) {
                min = parseLongOr(edtMin.getText().toString(), 0);
                max = parseLongOr(edtMax.getText().toString(), Long.MAX_VALUE);
                if (min > max) {
                    edtMax.setError("Giá đến phải lớn hơn giá từ");
                    return;
                }
            } else {
                for (int i = 0; i < PRICE_CHIP_IDS.length; i++) {
                    if (PRICE_CHIP_IDS[i] == checkedPrice) {
                        min = PRICE_RANGES[i][0];
                        max = PRICE_RANGES[i][1];
                    }
                }
            }
            adapter.setFilters(category, min, max, switchStock.isChecked());
            updateUi();
            sheet.dismiss();
        });

        sheet.show();
    }

    private Chip makeChip(String text) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(R.color.chip_bg);
        chip.setTextColor(getResources().getColorStateList(R.color.chip_text, getTheme()));
        chip.setChipStrokeColorResource(R.color.chip_stroke);
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        chip.setChipCornerRadius(100 * getResources().getDisplayMetrics().density);
        chip.setTextAppearance(R.style.SearchChipText);
        return chip;
    }

    private static long parseLongOr(String s, long fallback) {
        String digits = s == null ? "" : s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return fallback;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ================== GIAO DIỆN ==================

    /** Hiện lịch sử khi chưa tìm/lọc gì; ngược lại hiện kết quả (hoặc "không tìm thấy"). */
    private void updateUi() {
        int filters = adapter.activeFilterCount();
        btnFilter.setText(filters > 0 ? "Bộ lọc (" + filters + ")" : "Bộ lọc");
        btnFilter.setActivated(filters > 0);

        boolean searching = adapter.hasQueryOrFilter();
        int count = adapter.getShownCount();
        layoutHistory.setVisibility(!searching && !mlistStory.isEmpty() ? View.VISIBLE : View.GONE);
        rcvSearch.setVisibility(searching || mlistStory.isEmpty() ? View.VISIBLE : View.GONE);
        layoutEmpty.setVisibility(searching && count == 0 ? View.VISIBLE : View.GONE);
        tvResultCount.setText(searching || mlistStory.isEmpty()
                ? NumberFormat.getInstance().format(count) + " sản phẩm" : "");
    }

    // ================== QR / GIỌNG NÓI ==================

    private final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Hủy quét mã", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Product product : adapter.getAllProducts()) {
            if (result.getContents().equals(product.getId())) {
                Intent intent = new Intent(SearchActivity.this, DetailSPActivity.class);
                intent.putExtra("search", product);
                startActivity(intent);
                return;
            }
        }
        Toast.makeText(this, "Không tìm thấy sản phẩm với mã này", Toast.LENGTH_SHORT).show();
    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) searchView.setQuery(result.get(0), true);
        }
    }
}
