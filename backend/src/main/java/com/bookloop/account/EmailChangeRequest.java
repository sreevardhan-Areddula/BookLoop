package com.bookloop.account;

/** Current-password confirmation and desired address for an email-change request. */
public record EmailChangeRequest(String currentPassword, String newEmail) {
}
