package com.bookloop.exchange.notification;



import com.bookloop.mail.service.GmailEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ExchangeEmailNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeEmailNotificationListener.class);
    private final GmailEmailSender emailSender;
    private final String frontendUrl;

    public ExchangeEmailNotificationListener(GmailEmailSender emailSender,
            @Value("${bookloop.mail.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Emails the owner after the request transaction commits; delivery failure leaves in-app activity intact. */
    @TransactionalEventListener
    public void notifyOwner(NewExchangeRequestEvent event) {
        String link = frontendUrl.replaceAll("/$", "") + "/exchanges";
        try {
            emailSender.send(event.ownerEmail(), "New BookLoop exchange request",
                    event.requesterName() + " requested your book \"" + event.requestedTitle()
                            + "\" and offered \"" + event.offeredTitle() + "\".\n\n"
                            + "Sign in to BookLoop to review the request:\n" + link);
        } catch (RuntimeException exception) {
            LOGGER.warn("BookLoop exchange request notification could not be delivered.");
        }
    }
}
