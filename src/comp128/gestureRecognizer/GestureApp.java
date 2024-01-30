package comp128.gestureRecognizer;

import edu.macalester.graphics.*;
import edu.macalester.graphics.ui.Button;
import edu.macalester.graphics.ui.TextField;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.Consumer;
import edu.macalester.graphics.Line;

/**
 * The window and user interface for drawing gestures and automatically recognizing them
 * Created by bjackson on 10/29/2016.
 */
public class GestureApp {

    private CanvasWindow canvas;
    private Recognizer recognizer;
    private IOManager ioManager;
    private GraphicsGroup uiGroup;
    private Button addTemplateButton;
    private TextField templateNameField;
    private GraphicsText matchLabel;
    private Deque<Point> path;
    private Line draw;
    private Match match;


    public GestureApp(){
        canvas = new CanvasWindow("Gesture Recognizer", 600, 600);
        recognizer = new Recognizer();
        path = new ArrayDeque<>();
        ioManager = new IOManager();
        setupUI();
    }

    /**
     * Create the user interface
     */
    private void setupUI(){
        matchLabel = new GraphicsText("Match: ");
        matchLabel.setFont(FontStyle.PLAIN, 24);
        canvas.add(matchLabel, 10, 30);

        uiGroup = new GraphicsGroup();

        templateNameField = new TextField(); 

        addTemplateButton = new Button("Add Template");
        addTemplateButton.onClick( () -> addTemplate() );

        Point center = canvas.getCenter();
        double fieldWidthWithMargin = templateNameField.getSize().getX() + 5;
        double totalWidth = fieldWidthWithMargin + addTemplateButton.getSize().getX();


        uiGroup.add(templateNameField, center.getX() - totalWidth/2.0, 0);
        uiGroup.add(addTemplateButton, templateNameField.getPosition().getX() + fieldWidthWithMargin, 0);
        canvas.add(uiGroup, 0, canvas.getHeight() - uiGroup.getHeight());

        Consumer<Character> handleKeyCommand = ch -> keyTyped(ch);
        canvas.onCharacterTyped(handleKeyCommand);

        canvas.onMouseDown((event) -> paint(event.getPosition()));
        canvas.onDrag((event) -> paint(event.getPosition()));
        canvas.onDrag(event -> {
            Point oldPosition = event.getPreviousPosition();
            Point newPosition = event.getPosition();
            Line line = new Line(newPosition.getX(), newPosition.getY(), oldPosition.getX(), oldPosition.getY());
            path.add(newPosition);
            canvas.add(line);
        }) ;
        canvas.onMouseUp(event -> {
            if (1 < path.size()) {
                Template temp = recognizer.recognition(path);
                if (temp != null) {
                    matchLabel.setText("Match: " + temp.getName() + " " + temp.getScore());
                }
                else {
                    matchLabel.setText("No Match");
                }

            }
        });
        canvas.onMouseDown((event) -> removeAllNonUIGraphicsObjects());
    }

    private void paint(Point position) {
        if (!path.isEmpty()) {
            Point previousPos = path.peek();
            draw = new Line(previousPos, position);
            path.push(position); 
        }
        else {
            path.push(position);
        }
        draw.setStrokeColor(Color.BLACK);
        draw.setStrokeWidth(3);
        canvas.add(draw);    
    }

    /**
     * Clears the canvas, but preserves all the UI objects
     */
    private void removeAllNonUIGraphicsObjects() {
        canvas.removeAll();
        canvas.add(matchLabel);
        canvas.add(uiGroup);
    }

    /**
     * Handle what happens when the add template button is pressed. This method adds the points stored in path as a template
     * with the name from the templateNameField textbox. If no text has been entered then the template is named with "no name gesture"
     */
    private void addTemplate() {
        String name = templateNameField.getText();
        if (name.isEmpty()){
            name = "no name gesture";
        }
        recognizer.addTemplate(name, path); // Add the points stored in the path as a template

    }

    /**
     * Handles keyboard commands used to save and load gestures for debugging and to write tests.
     * Note, once you type in the templateNameField, you need to call canvas.requestFocus() in order to get
     * keyboard events. This is best done in the mouseDown callback on the canvas.
     */
    public void keyTyped(Character ch) {
        if (ch.equals('L')){
            String name = templateNameField.getText();
            if (name.isEmpty()){
                name = "gesture";
            }
            Deque<Point> points = ioManager.loadGesture(name+".xml");
            if (points != null){
                recognizer.addTemplate(name, points);
                System.out.println("Loaded "+name);
            }
        }
        else if (ch.equals('s')){
            String name = templateNameField.getText();
            if (name.isEmpty()){
                name = "gesture";
            }
            ioManager.saveGesture(path, name, name+".xml");
            System.out.println("Saved "+name);
        }
    }

    public static void main(String[] args){
        GestureApp window = new GestureApp();
    }
}
