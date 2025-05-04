package org.project.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

import org.project.repositories.BudgetRepository;
import org.project.repositories.JDBC_impl.jdbcBudgetRepository;
import org.project.services.TransactionService;
import org.project.util.FileHandler;
import org.project.util.ManualHandler;
import org.project.models.Transaction;
import org.project.repositories.TransactionRepository;
import org.project.repositories.JDBC_impl.jdbcTransactionRepository;
import static org.project.database.DatabaseConnection.getConnection;

import org.project.services.BudgetService;

public class TransactionsController implements Initializable {

    @FXML private ListView<Transaction> transactionsListView;
    @FXML private DatePicker dateField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> transactTypeField;
    @FXML private TextField customtransactType;
    @FXML private Button deleteSelectedBtn;
    @FXML private Button processFileBtn;

    private TransactionRepository transactionRepository;
    private  BudgetService budgetService;

    private BudgetRepository budgetRepository;
    private int userId = 1; //replace with the actual userid


    private final ObservableList<Transaction> transactionData = FXCollections.observableArrayList();
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



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Connection connection = getConnection();
            this.transactionRepository = new jdbcTransactionRepository(connection);
            this.budgetRepository = new jdbcBudgetRepository(connection);
            this.budgetService= new BudgetService(budgetRepository,transactionRepository);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database");
            e.printStackTrace();
        }
        transactTypeField.setItems(categories);
        transactTypeField.getSelectionModel().selectFirst();

        //list for transactions
        transactionsListView.setItems(transactionData);
        transactionsListView.setCellFactory(param -> new TransactionList());
        transactionsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        transactionsListView.setPlaceholder(new Label("No transactions added yet :'("));

        //hide the 'other' custom type
        customtransactType.setVisible(false);
        customtransactType.setManaged(false);

        //selection updates the delete button
        transactionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            deleteSelectedBtn.setDisable(transactionsListView.getSelectionModel().getSelectedItems().isEmpty());
        });

        //display the custom field when 'other' is seleceted
        transactTypeField.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean displayCustomType = "Other".equals(newVal);
            customtransactType.setVisible(displayCustomType);
            customtransactType.setManaged(displayCustomType);

            if (displayCustomType) {
                customtransactType.requestFocus();  //focus on box when selected
            } else {
                customtransactType.clear();
            }
        });

    }


    public void handleAddTransaction() {       //add transactions, currently only manual
        try {
            int userId=1;
            Transaction newTransaction = Transaction.submitManualTransaction(
                    userId,  //replace with the actual user id
                    dateField.getValue(),
                    amountField.getText(),
                    transactTypeField.getValue(),
                    customtransactType.getText()
            );

            // Add budget check before adding to list
            budgetService.checkTransactionAgainstBudgets(newTransaction);
            // Add to local list
            transactionData.add(newTransaction);

            // Clear the fields
            clearManualEntryFields();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Amount must be a valid number.");
        } catch (IllegalArgumentException e) {
            showAlert("Invalid Input", e.getMessage());
        } catch (Exception e) {
            showAlert("Error", "Could not add transaction: " + e.getMessage());
        }
    }

    @FXML
    private void handleFileUpload() {       //FILECHOOSER moment :D!!
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Transaction File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(transactionsListView.getScene().getWindow());    //opens up file thingy
        if (selectedFile != null) {
            FileHandler handler = new FileHandler();
            handler.addFile(selectedFile.getPath());
            //processFileBtn.setDisable(true);
        }
    }

    /*private List<Transaction> parseFile(File file) {
        // Parsing logic goes here... i think, or maybe services
    }*/

    @FXML
    private void handleDeleteSelected() {       //when click delete, either delete or no option selected
        ObservableList<Transaction> selectedTransactions =
                transactionsListView.getSelectionModel().getSelectedItems();

        if (selectedTransactions.isEmpty()) {
            showAlert("No Selection", "Please select transactions to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);   //confirmation for user deletion
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete " + selectedTransactions.size() + " transaction(s)?");
        confirmation.setContentText("This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                transactionData.removeAll(selectedTransactions);
            }
        });
    }

    @FXML
    private void handleSaveChanges() {
        if (transactionData.isEmpty()) {
            showAlert("No Data!", "There are no transaction to save");
            return;
        }
        try {
            // Save all transactions to database
            for (Transaction transaction : transactionData) {
                transactionRepository.save(transaction);

                budgetService.checkTransactionAgainstBudgets(transaction);
            }

            showAlert("Success", transactionData.size() + " transactions saved successfully");
            transactionData.clear(); // Clear the list after saving

        } catch (Exception e) {
            showAlert("Error", "Failed to save transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearFields() {
        clearManualEntryFields();
    }

    private void clearManualEntryFields() {
        dateField.setValue(null);
        amountField.clear();
        transactTypeField.getSelectionModel().selectFirst();
        customtransactType.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        //alert.initOwner(transactionsListView.getScene().getWindow());
        alert.showAndWait();
    }

    //list for display item, STILL UPDATING DONT MOVE THIS TO ANOTHER CLASS YET,
    private static class TransactionList extends ListCell<Transaction> {
        @Override
        protected void updateItem(Transaction transaction, boolean empty) {
            super.updateItem(transaction, empty);

            if (empty || transaction == null) {
                setText(null);
                setGraphic(null);
            }
            else {  //set up list
                HBox container = new HBox(10);
                Label dateLabel = new Label(transaction.getFormattedDate());
                Label typeLabel = new Label(transaction.getTransactType());
                Label amountLabel = new Label(transaction.getFormattedAmount());

                container.getChildren().addAll(dateLabel, amountLabel, typeLabel);
                setGraphic(container);
            }
        }
    }
}

