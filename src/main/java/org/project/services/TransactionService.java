package org.project.services;

import org.project.models.Transaction;
import org.project.repositories.TransactionRepository;

public class TransactionService {
    private final TransactionRepository transactionRepo;
    private final BudgetService budgetChecker;

    public TransactionService(TransactionRepository transactionRepo,
                              BudgetService budgetChecker) {
        this.transactionRepo = transactionRepo;
        this.budgetChecker = budgetChecker;
    }

    public Transaction saveTransaction(Transaction transaction) throws Exception {
        // First check budget
        budgetChecker.checkTransactionAgainstBudgets(transaction);

        // If no exception, save the transaction
        return transactionRepo.save(transaction);
    }
}
