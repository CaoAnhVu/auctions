package com.example.auctions.controller;

import com.example.auctions.model.User;
import com.example.auctions.model.Transaction;
import com.example.auctions.service.TransactionService;
import com.example.auctions.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/history")
    public String viewTransactionHistory(@AuthenticationPrincipal User user, Model model) {
        List<Transaction> transactions;
        
        if (user.getRole().name().equals("BUYER")) {
            transactions = transactionService.getPurchaseHistory(user);
            model.addAttribute("historyType", "Purchase");
        } else {
            transactions = transactionService.getSaleHistory(user);
            model.addAttribute("historyType", "Sale");
        }
        
        model.addAttribute("transactions", transactions);
        return "transaction/history";
    }
} 