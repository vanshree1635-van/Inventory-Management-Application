package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NotificationService {

    private static final ObservableList<String> notifications =
            FXCollections.observableArrayList();

    public static void addNotification(String msg) {
        notifications.add(msg);
    }

    public static ObservableList<String> getNotifications() {
        return notifications;
    }
}