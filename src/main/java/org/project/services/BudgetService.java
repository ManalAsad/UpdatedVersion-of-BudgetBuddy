package org.project.services;

import javafx.scene.control.Alert;
import org.project.models.Budget;
import org.project.models.Transaction;
import org.project.repositories.BudgetRepository;
import org.project.repositories.TransactionRepository;
import org.project.repositories.NotificationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.project.repositories.BudgetRepository;

public class BudgetService {
    private final BudgetRepository budgetRepo;
    private final TransactionRepository transactionRepo;
    //private final NotificationRepository notificationRepo;

    public BudgetService(BudgetRepository budgetRepo,
                         TransactionRepository transactionRepo) {
        this.budgetRepo = budgetRepo;
        this.transactionRepo = transactionRepo;
        //this.notificationRepo = notificationRepo;
    }

    public static void validateBudgetInput(String category,
                                            LocalDate startDate, LocalDate endDate, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (startDate == null || endDate == null) throw new IllegalArgumentException("Dates cannot be null");
        if (startDate.isAfter(endDate)) throw new IllegalArgumentException("End date must be after start date");
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Category is required");
    }

    public void checkTransactionAgainstBudgets(Transaction transaction) throws SQLException {
        try{
            // Get all active budgets for this category and user
            List<Budget> budgets = budgetRepo.findByUserAndCategory(
                    transaction.getUserId(),
                    transaction.getTransactType()
            );
            for (Budget budget : budgets) {
                // Check if transaction falls within budget period
                if (!isWithinBudgetPeriod(transaction, budget)){
                        continue;
                }
                // Calculate total spent during budget period
                double totalSpentForTransaction= budgetRepo.getTotalSpentForBudget(
                        transaction.getUserId(),
                        transaction.getTransactType(),
                        budget.getStartDate(),
                        budget.getEndDate()
                );

                // Add the new transaction amount
                double newTotal = totalSpentForTransaction + transaction.getAmount();

                // Check if exceeds budget
                if (newTotal > budget.getAmount()) {
                    throw new Exception(
                            String.format("Budget exceeded for %s! (%.2f/%.2f)",
                                    budget.getCategory(),
                                    newTotal,
                                    budget.getAmount())
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Budget check Failed  " +e.getMessage());
        }
    }
    private boolean isWithinBudgetPeriod(Transaction transaction, Budget budget) {
        return !transaction.getDate().isBefore(budget.getStartDate()) &&
                !transaction.getDate().isAfter(budget.getEndDate());
    }

}

