package io.celox.hexpulse.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import io.celox.hexpulse.R;
import io.celox.hexpulse.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up button listeners
        binding.btnPlayerVsPlayer.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("game_mode", "PVP");
            Navigation.findNavController(v).navigate(R.id.nav_gallery, args);
        });
        
        binding.btnPlayerVsAi.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("game_mode", "AI");
            Navigation.findNavController(v).navigate(R.id.nav_gallery, args);
        });
        
        binding.btnOnlineMultiplayer.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_online_game);
        });
        
        binding.btnSettings.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_slideshow);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}