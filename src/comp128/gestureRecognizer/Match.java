package comp128.gestureRecognizer;

import java.util.Deque;

import edu.macalester.graphics.Point;

public class Match {
    private String name;
    private Template match;
    private double score;
    public Match(double score, Template match) {
        this.match = match;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public Template getMatch() {
        return match;
    }

    public double getScore() {
        return score;
    }

}
