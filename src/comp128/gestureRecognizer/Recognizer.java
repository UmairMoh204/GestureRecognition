package comp128.gestureRecognizer;

import edu.macalester.graphics.CanvasWindow;
import edu.macalester.graphics.Ellipse;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.Point;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Recognizer to recognize 2D gestures. Uses the $1 gesture recognition algorithm.
 */
public class Recognizer {

    private static final int NUM = 64;
    private static final int SIZE = 50;
    private static final Point K = new Point(0, 0);
    private Deque<Point> translated;
    private Deque<Point> scaledPoints;
    private Deque<Point> resample;
    private Deque<Point> rotatedPoints;
    private Deque<Point> bestScore;
    private Match match;
    private CanvasWindow canvas;
    private List<Template> savedTemp;

    /**
     * Constructs a recognizer object
     */
    public Recognizer(){
        savedTemp = new ArrayList<>();
    }


    /**
     * Create a template to use for matching
     * @param name of the template
     * @param points in the template gesture's path
     */
    public void addTemplate(String name, Deque<Point> points){
        Deque<Point> processedPoints = resample(points, NUM);
        double indicativeAngle = indicativeAngle(processedPoints);
        processedPoints = rotateBy(processedPoints, -indicativeAngle);
        processedPoints = scaleTo(processedPoints, SIZE);
        processedPoints = translateTo(processedPoints, K);
        Template newTemp = new Template(name, processedPoints);
        savedTemp.add(newTemp);
    }

    public Template recognition(Deque<Point> points) {
        Template bestTemplate = null;
        double max = Double.MAX_VALUE;
        Deque<Point> processedPoints = gestureRecognize(points);

        for (Template temp : savedTemp) {
            double bestDistance = distanceAtBestAngle(processedPoints, temp.getPoints());
            if (bestDistance < max) {
                max = bestDistance;
                bestTemplate = temp;
        }
        }
        double score = 1 - (max * 2 / (Math.sqrt(2) * SIZE));
        bestTemplate.setScore(score);
        match = new Match(score, bestTemplate);
        return bestTemplate;

    }

    public Deque<Point> gestureRecognize(Deque<Point> points) {
        Deque<Point> processedPoints = resample(points, NUM);
        processedPoints = rotateBy(resample, -indicativeAngle(processedPoints));
        processedPoints = scaleTo(rotatedPoints, SIZE);
        processedPoints = translateTo(scaledPoints, K);
        return processedPoints;
    }


    /**
     * Uses a golden section search to calculate rotation that minimizes the distance between the gesture and the template points.
     * @param points
     * @param templatePoints
     * @return best distance
     */
    private double distanceAtBestAngle(Deque<Point> points, Deque<Point> templatePoints){
        double thetaA = -Math.toRadians(45);
        double thetaB = Math.toRadians(45);
        final double deltaTheta = Math.toRadians(2);
        double phi = 0.5*(-1.0 + Math.sqrt(5.0));// golden ratio
        double x1 = phi*thetaA + (1-phi)*thetaB;
        double f1 = distanceAtAngle(points, templatePoints, x1);
        double x2 = (1 - phi)*thetaA + phi*thetaB;
        double f2 = distanceAtAngle(points, templatePoints, x2);
        while(Math.abs(thetaB-thetaA) > deltaTheta){
            if (f1 < f2){
                thetaB = x2;
                x2 = x1;
                f2 = f1;
                x1 = phi*thetaA + (1-phi)*thetaB;
                f1 = distanceAtAngle(points, templatePoints, x1);
            }
            else{
                thetaA = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1-phi)*thetaA + phi*thetaB;
                f2 = distanceAtAngle(points, templatePoints, x2);
            }
        }
        return Math.min(f1, f2);
    }

    public Deque<Point> resample(Deque<Point> a, int b){
        Iterator<Point> points = a.iterator();
        resample = new ArrayDeque<Point>();
        Point prevPoint = points.next();
        Point currentPoint = points.next();
        double pathLength = totalPathLength(a);
        double gain = pathLength / (b - 1);
        resample.add(prevPoint);
        double d = 0.0;
        while (points.hasNext()) {
            double distance = pointToPoint(prevPoint, currentPoint);
            if ((d + distance) >= gain) {
                double fraction = (gain - d) / distance;
                Point reformatted = Point.interpolate(prevPoint, currentPoint, fraction);
                resample.add(reformatted);
                prevPoint = reformatted;
                d = 0.0;
            }
            else {
                prevPoint = currentPoint;
                currentPoint = points.next();
                d += distance;
            }
        }
        if (resample.size() != a.size()) {
            resample.add(currentPoint);
        }
        return resample;

    }

    private double distanceAtAngle(Deque<Point> points, Deque<Point> templatePoints, double theta){
        Deque<Point> rotatedPoints = null;
        rotatedPoints = rotateBy(points, theta);
        return pathDistance(rotatedPoints, templatePoints);
    }


    public Deque<Point> rotateBy(Deque<Point> points, double theta) {
        rotatedPoints = new ArrayDeque<>();
        for (Point p : points){
            Point rotate = p.rotate(theta, centroid(points));
            rotatedPoints.add(rotate);
        }
        return rotatedPoints;
    }
    


    public double pathDistance(Deque<Point> rotatedPoints, Deque<Point> templatePoints) {
        double distance = 0;
        int size = rotatedPoints.size();
        Iterator<Point> a = rotatedPoints.iterator();
        Iterator <Point> b = templatePoints.iterator();
        while (a.hasNext() && b.hasNext()) {
            Point pointA = a.next();
            Point pointB = b.next();
            distance += pointToPoint(pointA, pointB);
        }
    
        return distance/size;
    }


    public double totalPathLength(Deque<Point> a) {
        double point = 0;
        Point prevPoint = a.element();
        for (Point p : a) {
            Point currPoint = p;
            point += pointToPoint(currPoint, prevPoint);
            prevPoint = currPoint;
        }
        return point;

    }

    private double pointToPoint(Point pointA, Point pointB) {
        double compX = pointB.getX() - pointA.getX();
        double compY = pointB.getY() - pointA.getY();
        return Math.sqrt(compX * compX + compY * compY);
    }


    public double indicativeAngle(Deque<Point> points) {
        Point updatedPoint = centroid(points).subtract(points.peekFirst());
        return updatedPoint.angle();
    }

    public Point centroid(Deque<Point> points) {
        int numX = 0;
        int numY = 0;
        double avX = 0;
        double avY = 0;
        for (Point p : points) {
            avX += p.getX();
            numX++;
            avY += p.getY();
            numY++;
        }
        double centroidX = avX / numX;
        double centroidY = avY / numY;
        Point point = new Point(centroidX, centroidY);
        return point;
    }

    public Deque<Point> scaleTo (Deque<Point> point, int num) {
        double maxX = point.peek().getX();
        double maxY = point.peek().getY();
        double minX = point.peek().getX();
        double minY = point.peek().getY();
        for (Point p : point) {
            if (p.getX() > maxX) {
                maxX = p.getX();
            }
            if (p.getY() > maxY) {
                maxY = p.getY();
            }
            if (p.getX() < minX) {
                minX = p.getX();
            }
            if (p.getY() < minY) {
                minY = p.getY();
            }
        }
        double width = (maxX - minX);
        double height = (maxY - minY);
        double scaleX = num / width;
        double scaleY = num / height;

        scaledPoints = new ArrayDeque<>();

        for (Point p : point) {
            Point scalePoint = p.scale(scaleX, scaleY);
            scaledPoints.add(scalePoint);
        }
        return scaledPoints;
    }

    public Deque<Point> translateTo (Deque<Point> points, Point point) {
        Point centroid = centroid(points);
        double differenceX = point.getX() - centroid.getX();
        double differenceY = point.getY() - centroid.getY();

        translated = new ArrayDeque<>();

        for (Point p : points) {
            double newX = differenceX + p.getX();
            double newY = differenceY + p.getY();
            translated.add(new Point(newX, newY));
        }

        return translated;

    }

}