package com.villagelegends.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.villagelegends.VillageLegends;

import java.util.HashMap;
import java.util.Map;

/**
 * AudioManager owns all Music and Sound instances.
 *
 * Music tracks are streamed (LibGDX Music API).
 * Sound effects are buffered (LibGDX Sound API) for low-latency playback.
 *
 * Volume levels are persisted via SaveManager preferences.
 * Cross-fade between tracks is handled via a simple alpha blend
 * updating each frame.
 *
 * Asset paths (android/assets/audio/):
 *   music/menu_theme.ogg
 *   music/village_day.ogg
 *   music/village_night.ogg
 *   music/forest_ambience.ogg
 *   music/lake_calm.ogg
 *   music/town_market.ogg
 *   music/highway_chase.ogg
 *   music/combat_tension.ogg
 *   music/festival_sankranti.ogg
 *   music/festival_ugadi.ogg
 *   music/festival_dasara.ogg
 *   music/festival_deepavali.ogg
 *   sfx/ (all .ogg)
 */
public class AudioManager {

    private final VillageLegends game;

    // ── Volume settings ───────────────────────────────────────
    private float musicVol = com.villagelegends.GameConstants.MUSIC_VOL_DEFAULT;
    private float sfxVol   = com.villagelegends.GameConstants.SFX_VOL_DEFAULT;

    // ── Currently playing track ───────────────────────────────
    private Music  currentMusic  = null;
    private Music  fadeOutMusic  = null;
    private float  fadeOutTimer  = 0f;
    private String currentTrack  = "";

    // ── Loaded assets ─────────────────────────────────────────
    private final Map<String, Music> musicCache = new HashMap<>();
    private final Map<String, Sound> soundCache = new HashMap<>();

    // ─────────────────────────────────────────────────────────
    public AudioManager(VillageLegends game) {
        this.game = game;
        loadSoundEffects();
    }

    // ── Asset loading ─────────────────────────────────────────
    private void loadSoundEffects() {
        // In production all of these .ogg files exist in android/assets/audio/sfx/
        // Here we guard with Gdx.files.internal().exists() so dev builds without
        // audio assets still compile and run.
        loadSfx("attack_swing");
        loadSfx("hit_flesh");
        loadSfx("knockout");
        loadSfx("player_hurt");
        loadSfx("coin_jingle");
        loadSfx("harvest");
        loadSfx("plant_seed");
        loadSfx("water_crops");
        loadSfx("spray");
        loadSfx("plow_soil");
        loadSfx("engine_start");
        loadSfx("engine_off");
        loadSfx("footstep_dirt");
        loadSfx("footstep_grass");
        loadSfx("ui_click");
        loadSfx("ui_open");
        loadSfx("quest_complete");
        loadSfx("level_up");
        loadSfx("chest_open");
        loadSfx("door_open");
    }

    private void loadSfx(String name) {
        String path = "audio/sfx/" + name + ".ogg";
        if (Gdx.files.internal(path).exists()) {
            try {
                soundCache.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
            } catch (Exception e) {
                Gdx.app.log("AudioManager", "Could not load sfx: " + name);
            }
        }
    }

    private Music loadMusic(String name) {
        if (musicCache.containsKey(name)) return musicCache.get(name);
        String path = "audio/music/" + name + ".ogg";
        if (Gdx.files.internal(path).exists()) {
            try {
                Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
                m.setLooping(true);
                musicCache.put(name, m);
                return m;
            } catch (Exception e) {
                Gdx.app.log("AudioManager", "Could not load music: " + name);
            }
        }
        return null;
    }

    // ── Music playback ────────────────────────────────────────
    public void playMenuMusic()   { playTrack("menu_theme"); }

    public void playRegionMusic(String regionId) {
        String track;
        switch (regionId) {
            case "forest":   track = "forest_ambience"; break;
            case "lake":     track = "lake_calm";       break;
            case "town":     track = "town_market";     break;
            case "highway":  track = "highway_chase";   break;
            default:         track = "village_day";     break;
        }
        playTrack(track);
    }

    public void playFestivalMusic(String festival) {
        playTrack("festival_" + festival);
    }

    public void playCombatMusic() { playTrack("combat_tension"); }

    private void playTrack(String trackName) {
        if (trackName.equals(currentTrack)) return;

        // Start fade-out of current track
        if (currentMusic != null && currentMusic.isPlaying()) {
            fadeOutMusic  = currentMusic;
            fadeOutTimer  = 1.0f;
        }

        currentTrack  = trackName;
        currentMusic  = loadMusic(trackName);
        if (currentMusic != null) {
            currentMusic.setVolume(0f);   // fade in
            currentMusic.play();
        }
    }

    public void update(float delta) {
        // Fade out old track
        if (fadeOutMusic != null) {
            fadeOutTimer -= delta;
            float vol = Math.max(0, fadeOutMusic.getVolume() - delta * 2f);
            fadeOutMusic.setVolume(vol);
            if (fadeOutTimer <= 0 || vol <= 0) {
                fadeOutMusic.stop();
                fadeOutMusic = null;
            }
        }
        // Fade in new track
        if (currentMusic != null && currentMusic.isPlaying()) {
            float vol = Math.min(musicVol, currentMusic.getVolume() + delta * 1.5f);
            currentMusic.setVolume(vol);
        }
    }

    // ── SFX playback ──────────────────────────────────────────
    public void playSfx(String name) {
        Sound s = soundCache.get(name);
        if (s != null) s.play(sfxVol);
    }

    public void playSfxAt(String name, float volume) {
        Sound s = soundCache.get(name);
        if (s != null) s.play(Math.min(1f, sfxVol * volume));
    }

    // ── Volume control ────────────────────────────────────────
    public void adjustMusicVol(float delta) {
        musicVol = Math.max(0, Math.min(1f, musicVol + delta));
        if (currentMusic != null) currentMusic.setVolume(musicVol);
    }

    public void adjustSfxVol(float delta) {
        sfxVol = Math.max(0, Math.min(1f, sfxVol + delta));
    }

    // ── Getters ───────────────────────────────────────────────
    public float getMusicVol() { return musicVol; }
    public float getSfxVol()   { return sfxVol; }

    // ── Cleanup ───────────────────────────────────────────────
    public void dispose() {
        for (Music m : musicCache.values()) m.dispose();
        for (Sound s : soundCache.values()) s.dispose();
        musicCache.clear();
        soundCache.clear();
    }
}
