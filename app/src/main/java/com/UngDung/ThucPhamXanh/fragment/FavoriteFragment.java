package com.UngDung.ThucPhamXanh.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.FavoriteAdapter;
import com.UngDung.ThucPhamXanh.Models.Favorite;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.Presenter.FavoritePresenter;
import com.UngDung.ThucPhamXanh.Presenter.ProductPresenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.DetailSPActivity;
import com.UngDung.ThucPhamXanh.my_interface.FavoriteView;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.UngDung.ThucPhamXanh.my_interface.ProductView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FavoriteFragment extends Fragment implements ProductView, FavoriteView {
    private View view;
    private ProgressBar progressBar;
    private TextView tvNullFavorite;
    private RecyclerView rcvFavorite;

    private ProductPresenter productPresenter;
    private FavoritePresenter favoritePresenter;
    private ArrayList<Favorite> mlistFavorite;
    private ArrayList<Product> mlistProduct;
    private FavoriteAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    int i = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_favorite, container, false);

        InitWidget();
        DeleteFavorite();

        return view;
    }

    private void InitWidget() {
        progressBar = view.findViewById(R.id.progressBar_favorite);
        tvNullFavorite = view.findViewById(R.id.tv_null_favorite);
        rcvFavorite = view.findViewById(R.id.rcv_favorite);
        mlistProduct = new ArrayList<>();
        mlistFavorite = new ArrayList<>();

//        progressBar.setVisibility(View.VISIBLE);
        productPresenter = new ProductPresenter(this);
        favoritePresenter = new FavoritePresenter(this);
        favoritePresenter.HandleGetFavorite(FirebaseAuth.getInstance().getCurrentUser().getUid());


    }

    private void DeleteFavorite() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return true;
            }
            //chức năng xóa sp trong giỏ hàng
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                AlertDialog.Builder buidler = new AlertDialog.Builder(getContext());
                buidler.setMessage("Bạn có muốn xóa  sản phẩm " + mlistProduct.get(pos).getTensp() + " không?");
                buidler.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        db.collection("Favorite").document(mlistFavorite.get(pos).getIdlove()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                mlistProduct.remove(pos);
                                adapter.notifyDataSetChanged();

                                if (mlistProduct.size() == 0){
                                    tvNullFavorite.setVisibility(View.VISIBLE);
                                }
                                Toast.makeText(getContext(), "Xóa thành công", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
                buidler.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.notifyDataSetChanged();
                    }
                });
                buidler.show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rcvFavorite);
    }

    @Override
    public void getDataFavorite(String idlove, String idproduct, String iduser) {
        mlistFavorite.add(new Favorite(idlove, idproduct, FirebaseAuth.getInstance().getCurrentUser().getUid()));
        productPresenter.HandleGetWithIDProduct(idproduct);
    }

    @Override
    public void OnSucess() {

    }

    @Override
    public void OnFail() {

    }

    @Override
    public void getDataProduct(String id, String ten, Long gia, String hinhanh, String loaisp, String mota, Long soluong, String hansudung, Long type, String trongluong) {
        mlistProduct.add(new Product(id, ten, gia, hinhanh, loaisp, mota, soluong, hansudung, type, trongluong));
        if (mlistFavorite.size() != 0){
            tvNullFavorite.setVisibility(View.GONE);
        } else {
            tvNullFavorite.setVisibility(View.VISIBLE);
        }
//        progressBar.setVisibility(View.GONE);
        adapter = new FavoriteAdapter(getContext(), mlistProduct, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                Product product = mlistProduct.get(pos);
                Intent intent = new Intent(getContext(), DetailSPActivity.class);
                intent.putExtra("search", product);
                startActivity(intent);
            }
        });

        rcvFavorite.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        rcvFavorite.setAdapter(adapter);
    }
}