package com.bookloop.account.dto;


/** Editable name and public city submitted from the profile page. */
public record ProfileUpdateRequest(String displayName, String city) {
}
