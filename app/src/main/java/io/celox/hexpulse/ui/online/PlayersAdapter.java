package io.celox.hexpulse.ui.online;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.celox.hexpulse.R;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {
    private List<OnlineGameViewModel.Player> players = new ArrayList<>();
    private Context context;

    public PlayersAdapter(Context context) {
        this.context = context;
    }

    public void updatePlayers(List<OnlineGameViewModel.Player> newPlayers) {
        this.players = newPlayers != null ? new ArrayList<>(newPlayers) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        OnlineGameViewModel.Player player = players.get(position);
        holder.bind(player);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder {
        private View colorIndicator;
        private TextView playerName;
        private TextView playerStatus;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.player_color_indicator);
            playerName = itemView.findViewById(R.id.tv_player_name);
            playerStatus = itemView.findViewById(R.id.tv_player_status);
        }

        public void bind(OnlineGameViewModel.Player player) {
            // Set player name
            playerName.setText("Player " + player.id.substring(player.id.lastIndexOf("_") + 1));
            
            // Set player status
            if (player.isHost) {
                playerStatus.setText("Host");
                playerStatus.setVisibility(View.VISIBLE);
            } else {
                playerStatus.setText("Player");
                playerStatus.setVisibility(View.VISIBLE);
            }
            
            // Set color indicator based on player color
            int colorRes;
            if ("black".equalsIgnoreCase(player.color)) {
                colorRes = R.color.marble_black;
            } else if ("white".equalsIgnoreCase(player.color)) {
                colorRes = R.color.marble_white;
            } else {
                colorRes = R.color.accent_color;
            }
            
            colorIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, colorRes));
        }
    }
}