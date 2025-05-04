package org.project.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.net.URL;

import org.project.models.Budget;
import org.project.models.Transaction;
import org.project.repositories.BudgetRepository;
import org.project.repositories.JDBC_impl.jdbcBudgetRepository;
import org.project.services.BudgetService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

import static org.project.database.DatabaseConnection.getConnection;

public class DashboardController implements Initializable {

    //@FXML private ListView<Budget> budgetListView;
    @FXML private VBox goalForm;
    @FXML private ComboBox<String> categoryPicker;
    @FXML private TextField amountField;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Button setGoalButton;
    @FXML private Label errorMessage;
   //temp before implementing alerts


    private boolean goalIsSet = false;
    private BudgetRepository budgetRepository;

    private int userId=1; // replace this with actual userId
    // final ObservableList<Budget> budgetData = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList(    //dropdow menu list of categories
            "Restaurants & Dining",
            "Shopping",
            "Health",
            "Insurance",
            "Transportation",
            "Entertainment",
            "Bills",
            "Other"
    );



    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Connection connection = getConnection();
            this.budgetRepository = new jdbcBudgetRepository(connection);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database");
            e.printStackTrace();
        }

        categoryPicker.setItems(categories);
        categoryPicker.getSelectionModel().selectFirst();

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(1));
    }

    @FXML
    private void openGoalForm() {
        boolean opened = goalForm.isVisible();
        goalForm.setVisible(!opened);
        goalForm.setManaged(!opened);
    }

    @FXML
    private void handleSaveChanges() throws Exception{
        try {
            int userId = 1;
            String category = categoryPicker.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String amountText = amountField.getText();

            BudgetService.validateBudgetInput(category,startDate,endDate, Double.parseDouble(amountText));


            Budget newBudget = Budget.enterYourBudget(userId, category, startDate, endDate, amountText);

            //budgetData.add(newBudget);
            budgetRepository.save(newBudget);

            showAlert("Success","Budget Saved Successfully!");

            clearManualEntryFields();

            //input validation. currently just printing the text bc I want to move displayAlerts in the transaction
            //..controller to an AlertsUtil for reusability
            //errorMessage.setText("");   //temp


            //when goal is saved, change button to edit goals, add logic for saving to database later
            goalIsSet = true;
            setGoalButton.setText("Edit Goal");
            goalForm.setVisible(false);
            goalForm.setManaged(false);

        } catch (NumberFormatException e) {
            errorMessage.setText("Please enter a valid number for limit.");
        }catch (Exception e) {
            showAlert("Error", "Failed to save Budget :(" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearFields() {
        clearManualEntryFields();
    }
    private void clearManualEntryFields() {
        categoryPicker.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        amountField.clear();

    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

