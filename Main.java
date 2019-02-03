package ru.ludens.sudoku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    Scene scene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("mainWindow.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Sudoku Wizard");
        scene = new Scene(root, 650, 450);
        primaryStage.setScene(scene);
        GameController mainWindowController = loader.getController();
        mainWindowController.setMain(this);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
