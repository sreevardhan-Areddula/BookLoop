package com.bookloop.account;

/** Editable name and public city submitted from the profile page. */
public record ProfileUpdateRequest(String displayName, String city) {
}
