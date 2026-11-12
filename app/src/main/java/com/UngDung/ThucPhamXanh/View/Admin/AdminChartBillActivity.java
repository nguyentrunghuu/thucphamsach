package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Presenter.HoaDonPreSenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.HoaDonView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminChartBillActivity extends AppCompatActivity implements HoaDonView {

    private Spinner spinnerDoanhthu;
    private ArrayList<String> mlistYear;
    private ArrayList<HoaDon> mlist;
    private HoaDonPreSenter hoaDonPreSenter;

    private Button btnTinhDoanhThu, btnXuatExcel;
    private EditText edtFromDate, edtToDate;
    private TextView tvDoanhThu;
    private Toolbar toolbar;
    private BarChart barChart;
    private PieChart pieChart;
    private  float dangxuly = 0,danggiaohang=0,giaohangthanhcong=0,huyhang=0;
    long total;
    private Number number;
    long chonngaytinhtien;
    private File filePath;
    private int i = 0;
    private HSSFRow row;
    private String day = "";
    private ArrayList<BarEntry> barEntries;
    private Number numBarChart;
    private int chonBarChart;
    private Date t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chart_bill);

        InitWidget();
        Init();
        Canculator();
        Event();



    }

    private void Canculator() {
        total = 0; // Reset total
        db.collection("HoaDon").whereEqualTo("trangthai", 3)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot q : queryDocumentSnapshots){
                    // đọc tiền an toàn (không phụ thuộc ngôn ngữ máy, không tràn số)
                    total += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(q.get("tongtien"));
                }
                tvDoanhThu.setText(NumberFormat.getInstance().format(total));
            }
        });
    }

    private void Event() {

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        edtFromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                int ngay = calendar.get(Calendar.DATE);
                int thang = calendar.get(Calendar.MONTH);
                int nam = calendar.get(Calendar.YEAR);
                DatePickerDialog datePickerDialog = new DatePickerDialog(AdminChartBillActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        // i:năm - i1:tháng - i2:ngày
                        calendar.set(i, i1, i2);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/YYYY");
                        edtFromDate.setText(simpleDateFormat.format(calendar.getTime()));

                    }
                }, nam, thang, ngay);
                datePickerDialog.show();
            }
        });

        edtToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                int ngay = calendar.get(Calendar.DATE);
                int thang = calendar.get(Calendar.MONTH);
                int nam = calendar.get(Calendar.YEAR);
                DatePickerDialog datePickerDialog = new DatePickerDialog(AdminChartBillActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        // i:năm - i1:tháng - i2:ngày
                        calendar.set(i, i1, i2);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/YYYY");
                        edtToDate.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, nam, thang, ngay);
                datePickerDialog.show();
            }
        });

        btnTinhDoanhThu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chonngaytinhtien=0;
                String startDate = edtFromDate.getText().toString();
                String endDate = edtToDate.getText().toString();
                if (startDate.length() == 0 || endDate.length() == 0){
                    Toast.makeText(AdminChartBillActivity.this, "Vui lòng chọn ngày bắt đầu/kết thúc", Toast.LENGTH_SHORT).show();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    try {
                        Date ngaybatdau = sdf.parse(startDate);
                        Date ngayketthuc = sdf.parse(endDate);
                        if (ngaybatdau.compareTo(ngayketthuc) > 0){
                            Toast.makeText(AdminChartBillActivity.this, "Vui lòng chọn ngày kết thúc sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
                        } else {
                            db.collection("HoaDon").whereEqualTo("trangthai", 3)
                                    .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot q : queryDocumentSnapshots){
                                        String s = q.getString("ngaydat");
                                        String tien = q.getString("tongtien");
                                        try {
                                            Date ngaygiua = sdf.parse(s);
                                            if (ngaygiua.compareTo(ngaybatdau) > 0 && ngaygiua.compareTo(ngayketthuc) < 0){
                                                Log.d("sosanhngay", "Bắt đầu < Giữa < Kết thúc");
                                                chonngaytinhtien += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(tien);
                                                Log.d("why", "UID: " + q.getString("UID"));
                                            } else if (ngaygiua.compareTo(ngaybatdau) == 0 && ngaygiua.compareTo(ngayketthuc) < 0){
                                                Log.d("sosanhngay", "Bắt đầu = Giữa < Kết thúc");
                                                chonngaytinhtien += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(tien);
                                                Log.d("why", "UID: " + q.getString("UID"));
                                            } else if (ngaygiua.compareTo(ngaybatdau) > 0 && ngaygiua.compareTo(ngayketthuc) == 0){
                                                Log.d("sosanhngay", "Bắt đầu < Giữa = Kết thúc");
                                                chonngaytinhtien += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(tien);
                                                Log.d("why", "UID: " + q.getString("UID"));
                                            } else if (ngaygiua.compareTo(ngaybatdau) == 0 && ngaygiua.compareTo(ngayketthuc) == 0){
                                                Log.d("sosanhngay", "Bắt đầu = Giữa = Kết thúc");
                                                chonngaytinhtien += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(tien);
                                                Log.d("why", "UID: " + q.getString("UID"));
                                            }
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    tvDoanhThu.setText(NumberFormat.getInstance().format(chonngaytinhtien));
                                    btnXuatExcel.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }


            }
        });


        btnXuatExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Tên file theo ngày giờ xuất (không trùng giữa các lần xuất)
                String exportFileName = "DoanhThu-" + new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".xls";
                HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
                HSSFSheet hssfSheet = hssfWorkbook.createSheet("Doanh thu"); // Tên sheet

                // Dòng đầu tiên của excel
                HSSFRow rowhead = hssfSheet.createRow((short)0);               // Hàng 1
                rowhead.createCell(0).setCellValue("STT");              // Cột 0
                rowhead.createCell(1).setCellValue("UID");              // Cột 1
                rowhead.createCell(2).setCellValue("Họ tên");           // Cột 2
                rowhead.createCell(3).setCellValue("Địa chỉ");          // Cột 3
                rowhead.createCell(4).setCellValue("SĐT");              // Cột 4
                rowhead.createCell(5).setCellValue("Ngày Đặt");         // Cột 5
                rowhead.createCell(6).setCellValue("Tổng tiền");        // Cột 6
                rowhead.createCell(7).setCellValue("ID Hóa đơn");       // Cột 7


                String startDate = edtFromDate.getText().toString();
                String endDate = edtToDate.getText().toString();
                if (startDate.length() == 0 || endDate.length() == 0){
                    Toast.makeText(AdminChartBillActivity.this, "Vui lòng chọn ngày bắt đầu/kết thúc", Toast.LENGTH_SHORT).show();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    try {
                        Date ngaybatdau = sdf.parse(startDate);
                        Date ngayketthuc = sdf.parse(endDate);
                        if (ngaybatdau.compareTo(ngayketthuc) > 0){
                            Toast.makeText(AdminChartBillActivity.this, "Vui lòng chọn ngày kết thúc sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
                        } else {
                            int rowIndex = 1; // Bắt đầu từ hàng 2 (sau header)
                            for (int j=0; j < mlist.size(); j++){
                                HoaDon hoaDon = mlist.get(j);
                                if (hoaDon == null || hoaDon.getNgaydat() == null) {
                                    continue;
                                }
                                String s = hoaDon.getNgaydat();
                                try {
                                    Date ngaygiua = sdf.parse(s);
                                    // Kiểm tra nếu ngày nằm trong khoảng từ ngaybatdau đến ngayketthuc (bao gồm cả 2 đầu)
                                    if ((ngaygiua.compareTo(ngaybatdau) >= 0 && ngaygiua.compareTo(ngayketthuc) <= 0)) {
                                        row = hssfSheet.createRow((short)rowIndex);
                                        row.createCell(0).setCellValue(rowIndex);
                                        row.createCell(1).setCellValue(hoaDon.getUid() != null ? hoaDon.getUid() : "");
                                        row.createCell(2).setCellValue(hoaDon.getHoten() != null ? hoaDon.getHoten() : "");
                                        row.createCell(3).setCellValue(hoaDon.getDiachi() != null ? hoaDon.getDiachi() : "");
                                        row.createCell(4).setCellValue(hoaDon.getSdt() != null ? hoaDon.getSdt() : "");
                                        row.createCell(5).setCellValue(hoaDon.getNgaydat() != null ? hoaDon.getNgaydat() : "");
                                        row.createCell(6).setCellValue(hoaDon.getTongtien() != null ? hoaDon.getTongtien() : "");
                                        row.createCell(7).setCellValue(hoaDon.getId() != null ? hoaDon.getId() : "");
                                        rowIndex++;
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    saveAndShareWorkbook(hssfWorkbook, exportFileName);
                }


            }
        });

        spinnerDoanhthu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                chonBarChart = 0;
                barEntries.clear();
                String item = adapterView.getItemAtPosition(position).toString();
                BieuDoDoanhThu(item);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                BieuDoDoanhThu("2020");
            }
        });
    }

    private void BieuDoDoanhThu(String item){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String thang1 = "01/01/" + item;
        String thang2 = "01/02/" + item;
        String thang3 = "01/03/" + item;
        String thang4 = "01/04/" + item;
        String thang5 = "01/05/" + item;
        String thang6 = "01/06/" + item;
        String thang7 = "01/07/" + item;
        String thang8 = "01/08/" + item;
        String thang9 = "01/09/" + item;
        String thang10 = "01/10/" + item;
        String thang11 = "01/11/" + item;
        String thang12 = "01/12/" + item;
        String thang13 = "01/01/" + (Integer.parseInt(item) + 1); // Tháng 1 năm sau

        Date t13;
        try {
            t1 = sdf.parse(thang1);
            t2 = sdf.parse(thang2);
            t3 = sdf.parse(thang3);
            t4 = sdf.parse(thang4);
            t5 = sdf.parse(thang5);
            t6 = sdf.parse(thang6);
            t7 = sdf.parse(thang7);
            t8 = sdf.parse(thang8);
            t9 = sdf.parse(thang9);
            t10 = sdf.parse(thang10);
            t11 = sdf.parse(thang11);
            t12 = sdf.parse(thang12);
            t13 = sdf.parse(thang13);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        Log.d("item", "Năm: " + item);

        // Tổng hợp doanh thu theo từng tháng
        float[] monthlyRevenue = new float[12]; // 12 tháng
        for (int k = 0; k < mlist.size(); k++){
            HoaDon hoaDon = mlist.get(k);
            if (hoaDon == null || hoaDon.getNgaydat() == null || hoaDon.getTongtien() == null) {
                continue;
            }
            day = hoaDon.getNgaydat();
            try {
                Date dayHD = sdf.parse(day);
                String tienBarChart = hoaDon.getTongtien();
                if (tienBarChart == null || tienBarChart.isEmpty()) {
                    continue;
                }
                long revenue = com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(tienBarChart);
                
                // Xác định tháng
                int month = -1;
                if (dayHD.compareTo(t1) >= 0 && dayHD.compareTo(t2) < 0) {
                    month = 0; // Tháng 1
                } else if (dayHD.compareTo(t2) >= 0 && dayHD.compareTo(t3) < 0) {
                    month = 1; // Tháng 2
                } else if (dayHD.compareTo(t3) >= 0 && dayHD.compareTo(t4) < 0) {
                    month = 2; // Tháng 3
                } else if (dayHD.compareTo(t4) >= 0 && dayHD.compareTo(t5) < 0) {
                    month = 3; // Tháng 4
                } else if (dayHD.compareTo(t5) >= 0 && dayHD.compareTo(t6) < 0) {
                    month = 4; // Tháng 5
                } else if (dayHD.compareTo(t6) >= 0 && dayHD.compareTo(t7) < 0) {
                    month = 5; // Tháng 6
                } else if (dayHD.compareTo(t7) >= 0 && dayHD.compareTo(t8) < 0) {
                    month = 6; // Tháng 7
                } else if (dayHD.compareTo(t8) >= 0 && dayHD.compareTo(t9) < 0) {
                    month = 7; // Tháng 8
                } else if (dayHD.compareTo(t9) >= 0 && dayHD.compareTo(t10) < 0) {
                    month = 8; // Tháng 9
                } else if (dayHD.compareTo(t10) >= 0 && dayHD.compareTo(t11) < 0) {
                    month = 9; // Tháng 10
                } else if (dayHD.compareTo(t11) >= 0 && dayHD.compareTo(t12) < 0) {
                    month = 10; // Tháng 11
                } else if (dayHD.compareTo(t12) >= 0 && dayHD.compareTo(t13) < 0) {
                    month = 11; // Tháng 12
                }
                
                if (month >= 0 && month < 12) {
                    monthlyRevenue[month] += revenue;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        
        // Tạo BarEntry cho mỗi tháng
        barEntries.clear();
        for (int month = 0; month < 12; month++) {
            if (monthlyRevenue[month] > 0) {
                barEntries.add(new BarEntry(month + 1, monthlyRevenue[month] / 1000));
            } else {
                barEntries.add(new BarEntry(month + 1, 0));
            }
        }
        
        // Initialize bar data set
        BarDataSet barDataSet = new BarDataSet(barEntries, "Doanh thu (nghìn VNĐ)");
        // Set colors
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        // Show draw value
        barDataSet.setDrawValues(true);
        // Set bar data
        barChart.setData(new BarData(barDataSet));
        // Set animation
        barChart.animateY(2000);
        barChart.getDescription().setText("Biểu đồ thống kê doanh thu năm " + item);
        barChart.getDescription().setTextColor(Color.WHITE);
        barChart.invalidate();
    }

    private void Init() {
        mlistYear = new ArrayList<>();
        // Thêm các năm từ 2020 đến năm hiện tại
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        for (int year = 2020; year <= currentYear; year++) {
            mlistYear.add(String.valueOf(year));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, mlistYear);
        spinnerDoanhthu.setAdapter(arrayAdapter);
        // Set default to current year
        int defaultPosition = mlistYear.indexOf(String.valueOf(currentYear));
        if (defaultPosition >= 0) {
            spinnerDoanhthu.setSelection(defaultPosition);
        }
        mlist = new ArrayList<>();
        hoaDonPreSenter = new HoaDonPreSenter(this);
        hoaDonPreSenter.HandleGetDataHDDaGiao();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        
        // Set up back button
        View imgBack = findViewById(R.id.img_back_chart);
        if (imgBack != null) {
            imgBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        barEntries = new ArrayList<>();
        ArrayList<PieEntry> pieEntries = new ArrayList<>();


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("HoaDon").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                for(QueryDocumentSnapshot q : queryDocumentSnapshots){
                    if(q.getLong("trangthai")==1){
                        dangxuly++;
                    }else if(q.getLong("trangthai")==2){
                        danggiaohang++;
                    }else if(q.getLong("trangthai")==3){
                        giaohangthanhcong++;
                    }else{
                        huyhang++;
                    }
                }
                pieEntries.add(new PieEntry(dangxuly,"Đang Xử Lý"));
                pieEntries.add(new PieEntry(danggiaohang,"Đang giao"));
                pieEntries.add(new PieEntry(giaohangthanhcong,"Đã giao"));
                pieEntries.add(new PieEntry(huyhang,"Đã hủy"));
                ArrayList<Integer> arrayList = new ArrayList<>();

                for(int color : ColorTemplate.MATERIAL_COLORS){
                    arrayList.add(color);
                }
                for(int color : ColorTemplate.VORDIPLOM_COLORS){
                    arrayList.add(color);
                }
                Legend l = pieChart.getLegend();

                l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                PieDataSet pieDataSet = new PieDataSet(pieEntries,"Biểu Đồ Trạng Thái Đơn Hàng");
                pieDataSet.setColors(arrayList);
                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(true);
                pieData.setValueFormatter(new PercentFormatter());
                pieData.setValueTextSize(14);
                pieChart.setData(pieData);
                pieChart.invalidate();
                pieChart.setDrawEntryLabels(true);
                pieChart.setUsePercentValues(true);
                pieChart.setEntryLabelTextSize(12);
                pieChart.setEntryLabelColor(Color.WHITE);
                pieChart.animateXY(2000, 2000);
                pieChart.setCenterText("Trạng Thái Đơn Hàng");

                pieChart.getDescription().setEnabled(false);

            }
        });

    }

    private void InitWidget() {
        spinnerDoanhthu = findViewById(R.id.spinner_doanhthu);
        tvDoanhThu = findViewById(R.id.tv_doanh_thu);
        edtFromDate = findViewById(R.id.edt_bill_from_date);
        edtToDate = findViewById(R.id.edt_bill_to_date);
        btnTinhDoanhThu = findViewById(R.id.btn_tinh_doanh_thu);
        btnXuatExcel = findViewById(R.id.btn_xuat_excel);

        toolbar = findViewById(R.id.toolbar);
        pieChart = findViewById(R.id.piechart);
        barChart = findViewById(R.id.barchart);
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien, Long type) {
        mlist.add(new HoaDon(id, uid, ghichu, diachi, hoten, ngaydat, phuongthuc, sdt, tongtien, type));
    }

    @Override
    public void OnFail() {

    }

    @Override
    public void OnSucess() {

    }

    /**
     * Lưu file Excel rồi mở hộp "Chia sẻ".
     * - Android 10+: lưu vào thư mục Download qua MediaStore (không cần quyền bộ nhớ).
     * - Android 9: lưu vào thư mục riêng của app (không cần quyền), chia sẻ qua FileProvider.
     */
    private void saveAndShareWorkbook(HSSFWorkbook workbook, String fileName) {
        final String mime = "application/vnd.ms-excel";
        android.net.Uri uri;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, mime);
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("Không tạo được file trong Download");
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    workbook.write(out);
                }
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }
                uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            }
        } catch (Exception e) {
            Log.e("AdminChartBill", "Xuất Excel thất bại", e);
            Toast.makeText(this, "Xuất file thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
                ? "Đã lưu " + fileName + " vào thư mục Download" : "Đã xuất " + fileName, Toast.LENGTH_LONG).show();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mime);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Gửi file doanh thu"));
    }
}