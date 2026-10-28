package com.bookloop.book;

import com.bookloop.mail.GmailEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FreeClaimEmailNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeClaimEmailNotificationListener.class);
    private final GmailEmailSender emailSender;
    private final String frontendUrl;

    public FreeClaimEmailNotificationListener(GmailEmailSender emailSender,
            @Value("${bookloop.mail.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Emails the owner after the claim is saved; delivery failure leaves the in-app request intact. */
    @TransactionalEventListener
    public void notifyOwner(NewFreeClaimEvent event) {
        String link = frontendUrl.replaceAll("/$", "") + "/exchanges";
        try {
            emailSender.send(event.ownerEmail(), "New BookLoop Free-book claim",
                    event.requesterName() + " requested your free book \"" + event.bookTitle() + "\".\n\n"
                            + "Sign in to BookLoop to review the request:\n" + link);
        } catch (RuntimeException exception) {
            LOGGER.warn("BookLoop Free-book claim notification could not be delivered.");
        }
    }
}
