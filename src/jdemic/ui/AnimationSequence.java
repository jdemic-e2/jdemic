package jdemic.ui;

import java.util.LinkedList;
import java.util.Queue;
//Animation class to test out animations in the map test scene, not needed in the end, but can be used for testing purposes
public class AnimationSequence {

    private final Queue<Runnable> animations = new LinkedList<>();

    private boolean playing = false;

    public void add(Runnable animation) { animations.add(animation); }

    public void play() {
        if (playing)  return;
        playing = true;
        playNext();
    }

    public void playNext() {
        Runnable next = animations.poll();
        if (next == null) {
            playing = false;
            return;
        }
        next.run();
    }
}