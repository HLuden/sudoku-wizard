package ru.ludens.sudoku;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class GameController {

    @FXML
    private Pane digitContainer;
    @FXML
    private Label[][] digitLabels = new Label[9][9];
    @FXML
    private ChoiceBox<String> savedProblems;
    private ObservableList<String> digitSet = FXCollections.observableArrayList(
            "Clear", "1" , "2", "3", "4", "5", "6", "7", "8", "9");
    private ChoiceBox<String> digits = new ChoiceBox<>(digitSet);
    private Data data = new Data();
    private Alert alert = new Alert(Alert.AlertType.INFORMATION);
    private boolean dataChanged = false;
    private boolean blockForm = false;

    public GameController() {
        AnimationTimer rePaint = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (dataChanged) {
                    update();
                    dataChanged = false;
                }
            }
        };
        rePaint.start();
    }

    void setMain(Main main) {
        alert.setTitle(null);
        alert.setHeaderText(null);
        updateListOfProperties();
        digits.setPrefHeight(40);
        digitContainer.getChildren().add(digits);
        digits.setVisible(false);
        digits.setOnAction(event -> {
            if (blockForm) {
                return;
            }
            int x = (int)(digits.getLayoutX()) / 40;
            int y = (int)(digits.getLayoutY()) / 40;
            if (digits.getValue() != null && !digits.getValue().equals(parseText(digitLabels[x][y].getText()))) {
                data.set(x, y, parseDigit(digits.getValue()));
                digitLabels[x][y].setText(parseText(digits.getValue()));
            }
        });
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                digitLabels[i][j] = (Label) main.scene.lookup("#label" + (i + j * 9));
                digitLabels[i][j].setText("");
                digitLabels[i][j].setOnMouseClicked(event -> {
                    if (blockForm) {
                        return;
                    }
                    double x = event.getSceneX() - digitContainer.getLayoutX() - event.getX();
                    double y = event.getSceneY() - digitContainer.getLayoutY() - event.getY();
                    int intX = (int)(x / 40);
                    int intY = (int)(y / 40);
                    ArrayList<String> items = new ArrayList<>();
                    int[] candidates = data.getCandidates(intX, intY);
                    for (Integer k : candidates) {
                        if (k == 0) {
                            items.add("Clear");
                        } else {
                            items.add(k.toString());
                        }
                    }
                    digits.setItems(FXCollections.observableArrayList(items));
                    digits.setLayoutX(x);
                    digits.setLayoutY(y);
                    digits.setValue(parseText(digitLabels[intX][intY].getText()));
                    digits.show();
                });
            }
        }
    }

    private void update() {
        for (int j = 0; j < 9; j++) {
            for (int i = 0; i < 9; i++) {
                digitLabels[i][j].setText(parseText(data.get(i, j)));
            }
        }
    }

    private int parseDigit(String text) {
        if (text.equals("Clear")) {
            return 0;
        } else {
            return Integer.parseInt(text);
        }
    }

    private String parseText(String text) {
        if (text.equals("Clear")) {
            return "";
        } else if (text.equals("")) {
            return "Clear";
        } else {
            return text;
        }
    }

    private String parseText(int value) {
        if (value == 0) {
            return "";
        } else {
            return Integer.toString(value);
        }
    }

    private void solve() {
        blockForm = true;
        LinkedList<Integer> resultInformation = new LinkedList<>();
        HashSet<Data> resultData = solveHeuristic(data, resultInformation);
        if (resultData.size() == 0) {
            if (resultInformation.getFirst() >= 10000) {
                alert.setContentText("Too few initial digits");
                alert.showAndWait();
            } else {
                alert.setContentText("Sudoku is incorrect");
                alert.showAndWait();
            }
            return;
        }
        if (resultData.size() > 1) {
            alert.setContentText("Sudoku has " + resultData.size() + " solutions, the first one is displayed");
            alert.showAndWait();
        }
        alert.setContentText("There were " + resultInformation.getFirst() + " variants in the analysis");
        alert.showAndWait();
        Iterator<Data> iterator = resultData.iterator();
        data = iterator.next();
        dataChanged = true;
        blockForm = false;
    }

    private Data solveDirect(Data inputData) { // Returns null if data is incorrect
        if (inputData.getStatus() == -1) {
            return null;
        }
        Data data = new Data(inputData);
        while (data.getStatus() == 0) {
            int[] trivialSolution = new int[3];
            trivialLoop:
            for (int j = 0; j < 9; j++) {
                for (int i = 0; i < 9; i++) {
                    if (data.get(i, j) == 0 && data.getCandidates(i, j).length == 2) {
                        trivialSolution[0] = i;
                        trivialSolution[1] = j;
                        trivialSolution[2] = data.getCandidates(i, j)[1];
                        break trivialLoop;
                    }
                }
            }
            if (trivialSolution[2] != 0) {
                data.set(trivialSolution[0], trivialSolution[1], trivialSolution[2]);
            } else { // Start advanced search
                boolean haveSolution = false;
                advancedLoop:
                for (int k = 1; k <= 9; k++) {
                    // Find a single candidate in rows
                    int counter = 0;
                    int lastX = -1;
                    int lastY = -1;
                    for (int j = 0; j < 9; j++) {
                        for (int i = 0; i < 9; i++) {
                            if (data.get(i, j) == 0) {
                                int[] candidates = data.getCandidates(i, j);
                                for (int candidate : candidates) {
                                    if (candidate == k) {
                                        counter++;
                                        lastX = i;
                                    }
                                    lastY = j;
                                }
                            }
                        }
                        if (counter == 1) {
                            data.set(lastX, lastY, k);
                            haveSolution = true;
                            break advancedLoop;
                        }
                    }
                    // Find a single candidate in columns
                    counter = 0;
                    lastX = -1;
                    lastY = -1;
                    for (int i = 0; i < 9; i++) {
                        for (int j = 0; j < 9; j++) {
                            if (data.get(i, j) == 0) {
                                int[] candidates = data.getCandidates(i, j);
                                for (int candidate : candidates) {
                                    if (candidate == k) {
                                        counter++;
                                        lastX = i;
                                        lastY = j;
                                    }
                                }
                            }
                        }
                        if (counter == 1) {
                            data.set(lastX, lastY, k);
                            haveSolution = true;
                            break advancedLoop;
                        }
                    }
                    // Find a single candidate in flats
                    counter = 0;
                    lastX = -1;
                    lastY = -1;
                    for (int j = 0; j < 3; j++) {
                        for (int i = 0; i < 3; i++) {
                            for (int l = 0; l < 9; l++) {
                                int x = l % 3 + i * 3;
                                int y = l / 3 + j * 3;
                                if (data.get(x, y) == 0) {
                                    int[] candidates = data.getCandidates(x, y);
                                    for (int candidate : candidates) {
                                        if (candidate == k) {
                                            counter++;
                                            lastX = x;
                                            lastY = y;
                                        }
                                    }
                                }
                            }
                            if (counter == 1) {
                                data.set(lastX, lastY, k);
                                haveSolution = true;
                                break advancedLoop;
                            }
                        }
                    }
                }
                if (!haveSolution) { // Return partially solved sudoku
                    return data;
                }
            }
        }
        return data;
    }

    // Solve by considering all possible variants
    private HashSet<Data> solveHeuristic(Data data, LinkedList<Integer> infoConsumer) {
        Deque<listOfVariantsEntry> listOfVariants = new LinkedList<>();
        HashSet<Data> listOfSolutions = new HashSet<>();
        HashSet<Data> dataSet = new HashSet<>(10000);
        listOfVariants.add(new listOfVariantsEntry(data));
        dataSet.add(data);
        Iterator<listOfVariantsEntry> iterator = listOfVariants.descendingIterator();
        int variantsCounter = 0;
        for (;iterator.hasNext();) {
            if (variantsCounter >= 10000) {
                infoConsumer.addFirst(variantsCounter);
                return listOfSolutions;
            }
            listOfVariantsEntry entry = iterator.next();
            if (entry.getMarker() == 0) {
                entry.setMarker();
                Data tempData = solveDirect(entry.getData());
                if (tempData != null) {
                    if (tempData.getStatus() == 1) { // Sudoku has solved directly
                        listOfSolutions.add(tempData);
                    } else {
                        int numberOfVariants = 10;
                        int lastX = -1;
                        int lastY = -1;
                        int[] lastCandidates = new int[0];
                        findMinimalNumberOfCandidatesLoop:
                        for (int j = 0; j < 9; j++) {
                            for (int i = 0; i < 9; i++) {
                                if (tempData.get(i, j) == 0) {
                                    if (tempData.getCandidates(i, j).length - 1 < numberOfVariants) {
                                        numberOfVariants = tempData.getCandidates(i, j).length - 1;
                                        lastCandidates = tempData.getCandidates(i, j);
                                        lastX = i;
                                        lastY = j;
                                        if (numberOfVariants == 2) {
                                            break findMinimalNumberOfCandidatesLoop;
                                        }
                                    }
                                }
                            }
                        }
                        //addVariantsLoop:
                        for (int i = 1; i < lastCandidates.length; i++) {
                            Data tempDataVariant = new Data(tempData);
                            tempDataVariant.set(lastX, lastY, lastCandidates[i]);
                            if (dataSet.add(tempDataVariant)) {
                                listOfVariants.add(new listOfVariantsEntry(tempDataVariant));
                                if (variantsCounter == 0) {
                                    infoConsumer.addAll(Arrays.asList(lastX, lastY));
                                }
                                variantsCounter++;
                            }
                        }
                        iterator = listOfVariants.descendingIterator();
                    }
                }
            }
        }
        infoConsumer.addFirst(variantsCounter);
        return listOfSolutions;
    }

    private void updateListOfProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File("resources/sudoku.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ObservableList<String> keys = FXCollections.observableArrayList();
        for (int i = 1; ; i++) {
            if (props.getProperty(Integer.toString(i), "").isEmpty()) {
                break;
            } else {
                String[] parts = props.getProperty(Integer.toString(i)).split(",");
                if (parts.length == 81) {
                    keys.add(Integer.toString(i));
                }
            }
        }
        if (keys.size() > 0) {
            savedProblems.setItems(keys);
            savedProblems.setValue(savedProblems.getItems().get(0));
        }
    }

    @FXML
    private void solveButtonHandler() {
        if (blockForm) {
            return;
        }
        solve();
    }

    @FXML
    private void loadButtonHandler() {
        if (blockForm) {
            return;
        }
        if (savedProblems.getValue() == null) {
            return;
        }
        data.clear();
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File("resources/sudoku.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] parts = props.getProperty(savedProblems.getValue()).split(",");
        for (int i = 0; i < parts.length; i++) {
            if (Integer.parseInt(parts[i]) != 0) {
                data.set(i % 9, i / 9, Integer.parseInt(parts[i]));
            }
        }
        dataChanged = true;
    }

    @FXML
    private void saveButtonHandler() {
        if (blockForm) {
            return;
        }
        StringBuilder newProblem = new StringBuilder();
        for (int i = 0; i < 81; i++) {
            newProblem.append(data.get(i % 9, i / 9));
            if (i < 80) {
                newProblem.append(",");
            }
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File("resources/sudoku.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean haveSameProblem = false;
        int lastIndex;
        for (int i = 1; ; i++) {
            lastIndex = i;
            String tempString = props.getProperty(Integer.toString(i));
            if (tempString == null) {
                break;
            } else if (tempString.equals(newProblem.toString())) {
                haveSameProblem = true;
            }
        }
        if (!haveSameProblem) {
            props.setProperty(Integer.toString(lastIndex), newProblem.toString());
            try {
                props.store(new FileOutputStream(new File("resources/sudoku.txt")), "");
                updateListOfProperties();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void clearButtonHandler() {
        if (blockForm) {
            return;
        }
        data.clear();
        dataChanged = true;
    }

    @FXML
    private void removeButtonHandler() {
        if (blockForm) {
            return;
        }
        if (savedProblems.getValue() == null) {
            return;
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File("resources/sudoku.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        props.remove(savedProblems.getValue());
        try {
            props.store(new FileOutputStream(new File("resources/sudoku.txt")), "");
            updateListOfProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
