import java.applet.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundPlayer2 {
    public static void main(String args[]) {
        final JFXPanel fxPanel = new JFXPanel();
        String bip = "test2.wav";
        Media hit = new Media(new File(bip).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(hit);
        mediaPlayer.setBalance(1.0);
        mediaPlayer.play();
    }
}
