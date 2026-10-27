package com.bookloop.sale;

import com.bookloop.mail.GmailEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SaleInterestEmailNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaleInterestEmailNotificationListener.class);
    private final GmailEmailSender emailSender;
    private final String frontendUrl;

    public SaleInterestEmailNotificationListener(GmailEmailSender emailSender,
            @Value("${bookloop.mail.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Emails the seller after the request commits; delivery failure does not remove the in-app request. */
    @TransactionalEventListener
    public void notifyOwner(NewSaleInterestEvent event) {
        String link = frontendUrl.replaceAll("/$", "") + "/exchanges";
        try {
            emailSender.send(event.ownerEmail(), "New BookLoop sale interest",
                    event.requesterName() + " is interested in your book \"" + event.listingTitle() + "\".\n\n"
                            + "Sign in to BookLoop to review this request:\n" + link);
        } catch (RuntimeException exception) {
            LOGGER.warn("BookLoop sale interest notification could not be delivered.");
        }
    }
}
