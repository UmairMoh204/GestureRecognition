package comp128.gestureRecognizer;

import java.util.Deque;

import edu.macalester.graphics.Point;

public class Template {
    String name;
    Deque<Point> points;
    double score;
    public Template(String name, Deque<Point> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public Deque<Point> getPoints() {
        return points;
    }

    public void setName(String newName) {
        name = newName;
    }

    public Deque<Point> setPoints(Deque<Point> newPoints) {
        return points = newPoints;
    }

    public void setScore(double newScore) {
        score = newScore;
    }
    
    public double getScore() {
        return score;
    }
}
