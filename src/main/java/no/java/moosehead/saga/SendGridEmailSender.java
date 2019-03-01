package no.java.moosehead.saga;

import com.sendgrid.*;
import no.java.moosehead.web.Configuration;
import org.apache.commons.mail.EmailException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SendGridEmailSender extends EmailSender {
    private final String sendgridKey;

    public SendGridEmailSender(String sendgridKey) {
        this.sendgridKey = sendgridKey;
    }

    private void sendSingleMail(String message, String to, String subject) throws EmailException {
        List<String> bccRec = new ArrayList<>();

        String bcc = Configuration.bccTo();
        if (bcc != null) {
            for (String tobc : bcc.split(";")) {
                bccRec.add(tobc);
            }
        }

        SendGrid sg = new SendGrid(this.sendgridKey);
        Request request = new Request();
        try {
            Email from = new Email("program@java.no");
            String contentType = message.contains("<body>") ? "text/html" : "text/plain";
            Content content = new Content(contentType, message);

            Mail mail = new Mail();

            mail.setFrom(from);
            mail.setSubject(subject);
            Personalization personalization = new Personalization();
            bccRec.stream().map(Email::new).forEach(personalization::addBcc);
            personalization.addTo(new Email(to));
            mail.addPersonalization(personalization);
            mail.addContent(content);

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


    }

    @Override
    public void send(EmailType type, String to, Map<String, String> values) {
        values.put("to", to);
        values.put("mooseheadLocation", Configuration.mooseheadLocation());

        String message = readFromTemplate(type, values);
        try {
            sendSingleMail(message,to,type.getSubject());
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }
}
