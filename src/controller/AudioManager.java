package controller;

import javax.sound.sampled.*;
import javax.swing.Timer;
import java.io.File;

public class AudioManager {
    private static final int FADE_STEPS = 10;
    private static final int FADE_STEP_DELAY_MS = 8;

    private static AudioManager instance;
    private Clip backgroundMusic;
    private FloatControl volumeControl;
    private Timer fadeTimer;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playBGM(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream rawStream = AudioSystem.getAudioInputStream(audioFile);
            AudioInputStream audioStream = toStandardFormat(rawStream);

            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);

            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

            if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN))
                volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);

            backgroundMusic.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioInputStream toStandardFormat(AudioInputStream rawStream) {
        AudioFormat sourceFormat = rawStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                44100,
                false
        );

        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            return AudioSystem.getAudioInputStream(targetFormat, rawStream);
        }

        AudioFormat intermediateFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false
        );
        if (AudioSystem.isConversionSupported(intermediateFormat, sourceFormat)) {
            AudioInputStream intermediateStream = AudioSystem.getAudioInputStream(intermediateFormat, rawStream);
            if (AudioSystem.isConversionSupported(targetFormat, intermediateFormat)) {
                return AudioSystem.getAudioInputStream(targetFormat, intermediateStream);
            }
            return intermediateStream;
        }

        return rawStream;
    }

    public void setVolume(int volume) {
        if (volumeControl == null) return;

        float targetDb = volume <= 0
                ? volumeControl.getMinimum()
                : (float) (Math.log10(volume / 100.0) * 20.0);
        targetDb = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), targetDb));

        rampVolumeTo(targetDb);
    }

    private void rampVolumeTo(float targetDb) {
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        float startDb = volumeControl.getValue();
        float stepSize = (targetDb - startDb) / FADE_STEPS;

        int[] stepCount = {0};
        fadeTimer = new Timer(FADE_STEP_DELAY_MS, null);
        fadeTimer.addActionListener(e -> {
            stepCount[0]++;
            if (stepCount[0] >= FADE_STEPS) {
                volumeControl.setValue(targetDb);
                fadeTimer.stop();
            } else {
                volumeControl.setValue(startDb + stepSize * stepCount[0]);
            }
        });
        fadeTimer.start();
    }
}
