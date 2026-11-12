package com.UngDung.ThucPhamXanh.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.UngDung.ThucPhamXanh.BotAI.ChatBotActivity;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.ChatActivity;

public class ChatFragment extends Fragment {

    private CardView cardChatbot;
    private CardView cardChatAdmin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        initViews(view);
        setupClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        cardChatbot = view.findViewById(R.id.card_chatbot);
        cardChatAdmin = view.findViewById(R.id.card_chat_admin);
    }

    private void setupClickListeners() {
        // Click vào ChatBot card
        cardChatbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Animation khi click
                Animation scaleAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
                v.startAnimation(scaleAnimation);
                
                Intent intent = new Intent(getContext(), ChatBotActivity.class);
                startActivity(intent);
            }
        });

        // Click vào Chat với Admin card
        cardChatAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Animation khi click
                Animation scaleAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
                v.startAnimation(scaleAnimation);
                
                Intent intent = new Intent(getContext(), ChatActivity.class);
                startActivity(intent);
            }
        });
    }
}
