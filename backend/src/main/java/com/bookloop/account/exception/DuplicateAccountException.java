package com.bookloop.account.exception;


/** Signals that a signup email is already in use without exposing it to the API. */
public class DuplicateAccountException extends RuntimeException {
}
