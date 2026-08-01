package com.bankingsystem.transactionservice.entity;


/**
 * Transaction LifeCycle
 * Pending --> Processing --> Completed(clean transaction)
 *                        --> Pending_Verification(Suspicious Detected)
 *                                     --> Completed(Verified)
 *                                     --> Flagged (SAGA REFUND)
 *                       ---->Failed
 *                       ---> Flagged

 */

public enum TransactionStatus {

    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FLAGGED 


}
