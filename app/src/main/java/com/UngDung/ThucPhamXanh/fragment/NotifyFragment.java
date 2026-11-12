package com.UngDung.ThucPhamXanh.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

public class NotifyFragment extends Fragment implements OnMapReadyCallback {
    private Toolbar toolbar;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();;
    private TextView txtdiachi,txtsdt,txtnoidung;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_notify, container, false);

        toolbar = v.findViewById(R.id.toolbar);
        txtdiachi = v.findViewById(R.id.txtdiachi);
        txtsdt = v.findViewById(R.id.txtsdt);
        txtnoidung = v.findViewById(R.id.txtnoidung);

        db.collection("ThongTinCuaHang").document("bj3byvtYZKYTj8BJiBf1")
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(@NonNull DocumentSnapshot documentSnapshot) {

                txtdiachi.setText("Địa chỉ : "+documentSnapshot.getString("diachi"));
                txtsdt.setText("Liên hệ : "+documentSnapshot.getString("sdt"));
                txtnoidung.setText("Nội Dung : "+documentSnapshot.getString("noidung"));


            }
        });

        // GG maps

        Log.d("state", "onCreateView");
        return v;
    }

    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {
        db.collection("ThongTinCuaHang").document("bj3byvtYZKYTj8BJiBf1").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                txtdiachi.setText("Địa chỉ : "+documentSnapshot.getString("diachi"));
                txtsdt.setText("Liên hệ : "+documentSnapshot.getString("sdt"));
                txtnoidung.setText("Nội Dung : "+documentSnapshot.getString("noidung"));
                //đọc vị trí gg map

            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("state", "onSave");
    }
}