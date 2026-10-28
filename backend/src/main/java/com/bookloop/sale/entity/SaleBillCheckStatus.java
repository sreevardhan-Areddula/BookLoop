package com.bookloop.sale.entity;


/** OCR outcome for a privately uploaded purchase bill. */
public enum SaleBillCheckStatus {
    UPLOADED,
    PRICE_CHECK_PASSED,
    PRICE_NOT_BELOW_PAID_AMOUNT,
    UNREADABLE
}
