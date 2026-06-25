package com.mycompany.inventory;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.util.Duration;

import ui.LoginUI;
import ui.DashboardUI;
import ui.AddInventoryUI;
import ui.IssueUI;
import ui.ReturnUI;
import ui.DeleteInventoryUI;
import ui.AddOrderUI;
import ui.ManageInventoryUI;

public class Inventory extends Application {

    private Stage stage;

    /**
     *
     * @param primaryStage
     */
    @Override
    public void start(Stage primaryStage) {
    this.stage = primaryStage;
    showLogin();
    primaryStage.setTitle("Inventory Management System");
}

    private void switchSceneWithFade(Scene newScene) {

    newScene.getRoot().setOpacity(0);

    stage.setScene(newScene);

    javafx.animation.FadeTransition fade = 
            new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(400),
                    newScene.getRoot());

    fade.setFromValue(0);
    fade.setToValue(1);
    fade.play();
}

    // Show Login Page
    public void showLogin() {
    LoginUI loginUI = new LoginUI(this);
    switchSceneWithFade(loginUI.getScene());
    stage.show();
}


    // Show Dashboard Page
    public void showDashboard() {
        DashboardUI dashboardUI = new DashboardUI(this);
        switchSceneWithFade(dashboardUI.getScene());
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void showAddInventory() 
    {
        AddInventoryUI ui = new AddInventoryUI(this);
        switchSceneWithFade(ui.getScene());
    }
    
    public void showAddOrder() {
        AddOrderUI ui = new AddOrderUI(this);
        switchSceneWithFade(ui.getScene());
    }
    
    public void showIssue() 
    {
        IssueUI issueUI = new IssueUI(this);
        switchSceneWithFade(issueUI.getScene());
    }
    
    public void showReturn(String type) {
        ReturnUI returnUI = new ReturnUI(this, type);
        switchSceneWithFade(returnUI.getScene());
    }
    
    public void showReturnUI() {
        ReturnUI ui = new ReturnUI(this, ReturnUI.ISSUE_TO_INVENTORY);
        stage.setScene(ui.getScene());
    }

    public void showDeleteInventory() 
    {
        DeleteInventoryUI ui = new DeleteInventoryUI(this);
        switchSceneWithFade(ui.getScene());
    }

    private static String userRole;

    public static void setUserRole(String role) {
        userRole = role;
    }

    public static String getUserRole() {
        return userRole;
    }
    private static String loggedUser;

    public static void setLoggedUser(String user) {
        loggedUser = user;
    }

    public static String getLoggedUser() {
        return loggedUser;
    }
    
    public void showManageInventory() {
        ManageInventoryUI ui = new ManageInventoryUI(this);
        switchSceneWithFade(ui.getScene());
    }
}