package io.celox.hexpulse.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.celox.hexpulse.databinding.FragmentSlideshowBinding;
import io.celox.hexpulse.game.AIDifficulty;
import io.celox.hexpulse.game.Theme;
import io.celox.hexpulse.settings.GameSettings;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private GameSettings gameSettings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize game settings
        gameSettings = GameSettings.getInstance(requireContext());

        setupAIDifficultySpinner();
        setupThemeSpinner();
        setupSoundSwitch();
        setupDebugModeSwitch();
        setupResetButton();

        return root;
    }

    private void setupAIDifficultySpinner() {
        // Create adapter for AI difficulties
        AIDifficulty[] difficulties = AIDifficulty.values();
        String[] difficultyNames = new String[difficulties.length];
        for (int i = 0; i < difficulties.length; i++) {
            difficultyNames[i] = GameSettings.getDifficultyDisplayName(difficulties[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                io.celox.hexpulse.R.layout.spinner_item,
                difficultyNames
        );
        adapter.setDropDownViewResource(io.celox.hexpulse.R.layout.spinner_dropdown_item);
        binding.spinnerAiDifficulty.setAdapter(adapter);

        // Set current selection
        AIDifficulty currentDifficulty = gameSettings.getAIDifficulty();
        int currentPosition = java.util.Arrays.asList(difficulties).indexOf(currentDifficulty);
        binding.spinnerAiDifficulty.setSelection(currentPosition);

        // Set up listener
        binding.spinnerAiDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AIDifficulty selectedDifficulty = difficulties[position];
                gameSettings.setAIDifficulty(selectedDifficulty);
                Toast.makeText(getContext(), 
                    "AI Difficulty set to " + GameSettings.getDifficultyDisplayName(selectedDifficulty), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupThemeSpinner() {
        // Create adapter for themes
        Theme[] themes = Theme.values();
        String[] themeNames = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            themeNames[i] = GameSettings.getThemeDisplayName(themes[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                io.celox.hexpulse.R.layout.spinner_item,
                themeNames
        );
        adapter.setDropDownViewResource(io.celox.hexpulse.R.layout.spinner_dropdown_item);
        binding.spinnerTheme.setAdapter(adapter);

        // Set current selection
        Theme currentTheme = gameSettings.getTheme();
        int currentPosition = java.util.Arrays.asList(themes).indexOf(currentTheme);
        binding.spinnerTheme.setSelection(currentPosition);

        // Set up listener
        binding.spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Theme selectedTheme = themes[position];
                gameSettings.setTheme(selectedTheme);
                Toast.makeText(getContext(), 
                    "Theme set to " + GameSettings.getThemeDisplayName(selectedTheme), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupSoundSwitch() {
        // Set current state
        binding.switchSoundEnabled.setChecked(gameSettings.isSoundEnabled());

        // Set up listener
        binding.switchSoundEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gameSettings.setSoundEnabled(isChecked);
            Toast.makeText(getContext(), 
                "Sound " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDebugModeSwitch() {
        // Set current state
        binding.switchDebugMode.setChecked(gameSettings.isDebugModeEnabled());

        // Set up listener
        binding.switchDebugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gameSettings.setDebugModeEnabled(isChecked);
            Toast.makeText(getContext(), 
                "Debug Mode " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void setupResetButton() {
        binding.btnResetSettings.setOnClickListener(v -> {
            // Reset to defaults
            gameSettings.setAIDifficulty(AIDifficulty.MEDIUM);
            gameSettings.setTheme(Theme.CLASSIC);
            gameSettings.setSoundEnabled(true);
            gameSettings.setDebugModeEnabled(false);

            // Update UI
            updateUIFromSettings();
            
            Toast.makeText(getContext(), "Settings reset to defaults", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUIFromSettings() {
        // Update AI Difficulty spinner
        AIDifficulty[] difficulties = AIDifficulty.values();
        AIDifficulty currentDifficulty = gameSettings.getAIDifficulty();
        int difficultyPosition = java.util.Arrays.asList(difficulties).indexOf(currentDifficulty);
        binding.spinnerAiDifficulty.setSelection(difficultyPosition);

        // Update Theme spinner
        Theme[] themes = Theme.values();
        Theme currentTheme = gameSettings.getTheme();
        int themePosition = java.util.Arrays.asList(themes).indexOf(currentTheme);
        binding.spinnerTheme.setSelection(themePosition);

        // Update Sound switch
        binding.switchSoundEnabled.setChecked(gameSettings.isSoundEnabled());

        // Update Debug Mode switch
        binding.switchDebugMode.setChecked(gameSettings.isDebugModeEnabled());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}