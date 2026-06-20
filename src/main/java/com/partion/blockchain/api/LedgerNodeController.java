package com.partion.blockchain.api;

import com.partion.blockchain.ledger.BlockRecord;
import com.partion.blockchain.ledger.LedgerService;
import com.partion.blockchain.ledger.LedgerTransactionRecord;
import com.partion.blockchain.ledger.LedgerVerifyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/node/ledger")
public class LedgerNodeController {

    private final LedgerService ledgerService;

    @GetMapping("/blocks")
    public List<BlockRecord> getBlocks() {
        return ledgerService.getBlocks();
    }

    @GetMapping("/transactions")
    public List<LedgerTransactionRecord> getTransactions(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ledgerService.getRecentTransactions(limit);
    }

    @GetMapping("/verify")
    public LedgerVerifyResponse verify() {
        return ledgerService.verify();
    }
}
