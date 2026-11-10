package com.fabiankevin.app.schedulers;

import com.fabiankevin.app.services.RecurringTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@EnableScheduling
@Component
public class TemporaryScheduler {
    private final RecurringTransactionService recurringTransactionService;

    public TemporaryScheduler(
            @Qualifier("syncChangeAwareRecurringTransactionService")
            RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @Scheduled(cron = "0 0 * * * *")
    void run(){
        log.info("Running temporary scheduler");
        recurringTransactionService.processDueRecurringTransactions();
        log.info("Done temporary scheduler");
    }
}
