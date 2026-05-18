package code._05_InterfaceVsAbstractClass;

/**
 * Demonstrates INTERFACE vs ABSTRACT CLASS decision.
 *
 * Rule of thumb:
 *  - Interface   → capability contract (CAN do something), multiple inheritance needed
 *  - Abstract    → shared state + shared implementation, template method
 *
 * Pattern shown: same List → AbstractList → ArrayList hierarchy from JDK
 */

// -------------------------------------------------------------------------
// INTERFACES — define capabilities (what it CAN do)
// -------------------------------------------------------------------------

interface Playable {
    void play();
    void pause();
    void stop();
}

interface Recordable {
    void startRecording();
    void stopRecording();
}

interface Exportable {
    void exportTo(String format);
}

// -------------------------------------------------------------------------
// ABSTRACT CLASS — shared state + shared implementation
// -------------------------------------------------------------------------
abstract class AbstractMediaPlayer implements Playable {

    // Shared state — all media players track this
    protected String mediaTitle;
    protected boolean isPlaying = false;
    private int playCount = 0;

    protected AbstractMediaPlayer(String mediaTitle) {
        this.mediaTitle = mediaTitle;
    }

    // Shared implementation — all players log play count the same way
    @Override
    public void play() {
        if (isPlaying) {
            System.out.println(mediaTitle + " is already playing");
            return;
        }
        isPlaying = true;
        playCount++;
        System.out.println("Playing: " + mediaTitle + " (play #" + playCount + ")");
        onPlay();  // Template Method — subclass hooks in here
    }

    @Override
    public void stop() {
        isPlaying = false;
        System.out.println("Stopped: " + mediaTitle);
        onStop();
    }

    // Template Method hooks — subclass customizes, base class controls the flow
    protected void onPlay() {}   // optional override
    protected void onStop() {}   // optional override

    public int getPlayCount() { return playCount; }
}

// -------------------------------------------------------------------------
// CONCRETE CLASSES — implement specifics
// -------------------------------------------------------------------------

// AudioPlayer only plays audio
class AudioPlayer extends AbstractMediaPlayer {
    private final String audioFormat;

    public AudioPlayer(String title, String format) {
        super(title);
        this.audioFormat = format;
    }

    @Override
    public void pause() {
        isPlaying = false;
        System.out.println("Audio paused: " + mediaTitle);
    }

    @Override
    protected void onPlay() {
        System.out.println("  [Audio] Decoding " + audioFormat + " stream...");
    }
}

// VideoPlayer plays video AND can record AND export — implements multiple interfaces
class VideoPlayer extends AbstractMediaPlayer implements Recordable, Exportable {
    private boolean isRecording = false;

    public VideoPlayer(String title) { super(title); }

    @Override
    public void pause() {
        isPlaying = false;
        System.out.println("Video paused: " + mediaTitle);
    }

    @Override
    public void startRecording() {
        isRecording = true;
        System.out.println("Recording started: " + mediaTitle);
    }

    @Override
    public void stopRecording() {
        isRecording = false;
        System.out.println("Recording stopped");
    }

    @Override
    public void exportTo(String format) {
        System.out.println("Exporting " + mediaTitle + " to " + format);
    }

    @Override
    protected void onPlay() {
        System.out.println("  [Video] Rendering frames...");
    }
}

// -------------------------------------------------------------------------
// DEMO — shows why the distinction matters
// -------------------------------------------------------------------------
class MediaPlayerDemo {
    public static void main(String[] args) {
        // Using abstract class reference — gets shared behaviour
        AbstractMediaPlayer audio = new AudioPlayer("Imagine - John Lennon", "MP3");
        audio.play();
        audio.pause();
        audio.play();
        System.out.println("Play count: " + audio.getPlayCount());
        System.out.println();

        // VideoPlayer through multiple interfaces — polymorphism across different contracts
        VideoPlayer video = new VideoPlayer("Interstellar");
        video.play();
        video.startRecording();
        video.stop();
        video.exportTo("MP4");
        System.out.println();

        // Working with interface reference — caller only knows it's Playable
        Playable player = new AudioPlayer("Bohemian Rhapsody", "FLAC");
        player.play();
        player.pause();
        // player.getPlayCount() — not accessible through Playable interface
        // This is abstraction: caller only sees what they need

        // Working with Exportable reference
        Exportable exportable = video;
        exportable.exportTo("AVI");
        // exportable.play() — not accessible, only Exportable methods visible
    }
}
