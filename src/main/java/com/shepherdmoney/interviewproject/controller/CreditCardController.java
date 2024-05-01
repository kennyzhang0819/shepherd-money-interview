package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
public class CreditCardController {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (creditCardRepository.findByNumber(payload.getCardNumber()).isPresent()) {
            return ResponseEntity.badRequest().body(null);
        }

        CreditCard creditCard = new CreditCard();
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setUser(user.get());
        creditCard.setBalanceHistory(new TreeMap<>());
        CreditCard savedCard = creditCardRepository.save(creditCard);
        return ResponseEntity.ok(savedCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.badRequest().body(null);
        }

        List<CreditCard> creditCards = creditCardRepository.findByUserId(userId);
        // Convert List of CreditCard to List of CreditCardView
        List<CreditCardView> creditCardViews = creditCards.stream()
                .map(card -> new CreditCardView(card.getNumber(), card.getIssuanceBank()))
                .toList();
        return ResponseEntity.ok(creditCardViews);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCard.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        } else {
            return ResponseEntity.ok(creditCard.get().getUser().getId());
        }
    }

    @GetMapping("/credit-card:balance")
    public ResponseEntity<Map<LocalDate, BalanceHistory>> getBalanceForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCard.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        } else {
            SortedMap<LocalDate, BalanceHistory> balanceHistory = creditCard.get().getBalanceHistory();
            return ResponseEntity.ok(balanceHistory);
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        for (UpdateBalancePayload update : payload) {
            Optional<CreditCard> creditCard = creditCardRepository.findByNumber(update.getCreditCardNumber());
            if (creditCard.isEmpty()) {
                return ResponseEntity.badRequest().body("Credit card not found");
            }
            CreditCard card = creditCard.get();
            SortedMap<LocalDate, BalanceHistory> cardBalanceHistory = card.getBalanceHistory();

            // Processing dates before the requested update date
            if (!cardBalanceHistory.containsKey(update.getBalanceDate())) {
                // If the requested update is the first balance history or
                // before the first date in the balance history, fill the gap with the balance of the updated date
                if (cardBalanceHistory.isEmpty() || cardBalanceHistory.firstKey().isAfter(update.getBalanceDate())) {
                    cardBalanceHistory.put(update.getBalanceDate(), new BalanceHistory(update.getBalanceDate(), update.getBalanceAmount()));
                    balanceHistoryRepository.save(cardBalanceHistory.get(update.getBalanceDate()));
                }
                LocalDate lastDate = cardBalanceHistory.firstKey();
                this.propagateBalanceDifference(cardBalanceHistory, lastDate, update.getBalanceDate().minusDays(1), 0);
            }

            // Processing dates after the requested update date with calculated difference
            double existingBalance = cardBalanceHistory.getOrDefault(update.getBalanceDate(), new BalanceHistory(update.getBalanceDate(), 0)).getBalance();
            double difference = update.getBalanceAmount() - existingBalance;
            this.propagateBalanceDifference(cardBalanceHistory, update.getBalanceDate(), LocalDate.now(), difference);

            creditCardRepository.save(card);
            for (BalanceHistory balance : cardBalanceHistory.values()) {
                System.out.println(balance.getDate() + " " + balance.getBalance());
            }
        }
        return ResponseEntity.ok().body("Balance updated successfully");
    }

    private void propagateBalanceDifference(SortedMap<LocalDate, BalanceHistory> balanceHistory, LocalDate startDate, LocalDate endDate, double amount) {
        assert !startDate.isAfter(endDate);
        assert balanceHistory.containsKey(startDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!balanceHistory.containsKey(date)) {
                balanceHistory.put(date, new BalanceHistory(date, balanceHistory.get(date.minusDays(1)).getBalance()));
                balanceHistoryRepository.save(balanceHistory.get(date));
            }
            if (amount != 0) {
                BalanceHistory currentBalance = balanceHistory.get(date);
                double newBalance = currentBalance.getBalance() + amount;
                balanceHistory.put(date, new BalanceHistory(date, newBalance));
                balanceHistoryRepository.save(balanceHistory.get(date));
            }
        }
    }

}
