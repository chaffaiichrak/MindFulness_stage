package utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Service d'envoi d'emails (confirmation de reservation, etc.)
 * Utilise SMTP via jakarta.mail. Pensez a renseigner vos identifiants
 * ci-dessous (idealement via un fichier de config / variables d'environnement
 * plutot qu'en dur, mais on suit ici le meme pattern que OWM_API_KEY dans
 * DashboardController pour rester coherent avec le projet).
 */
public class EmailService {

    // ── Configuration SMTP ──────────────────────────────────────────────
    // Exemple avec Gmail : necessite un "mot de passe d'application"
    // (https://myaccount.google.com/apppasswords), PAS votre mot de passe Gmail normal.
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String SENDER_EMAIL  = "chaffaiichrak298@gmail.com";        // TODO: a remplacer
    private static final String SENDER_PASSWORD = "drze icxf poze sdri";            // https://myaccount.google.com/apppasswords
    private static final String SENDER_NAME   = "MindFullness";

    /** Envoi asynchrone (ne bloque pas l'UI JavaFX). */
    public static void envoyerEmailAsync(String destinataire, String sujet, String contenuHtml) {
        Thread t = new Thread(() -> {
            try {
                envoyerEmail(destinataire, sujet, contenuHtml);
                System.out.println("[Email] Envoye a " + destinataire);
            } catch (Exception e) {
                System.err.println("[Email] Echec d'envoi : " + e.getMessage());
            }
        }, "email-send-thread");
        t.setDaemon(true);
        t.start();
    }

    /** Envoi synchrone bas niveau. */
    public static void envoyerEmail(String destinataire, String sujet, String contenuHtml) throws MessagingException {
        if (destinataire == null || destinataire.isBlank()) {
            throw new MessagingException("Adresse email destinataire vide.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(SENDER_EMAIL));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);
        message.setContent(contenuHtml, "text/html; charset=utf-8");

        Transport.send(message);
    }

    /** Construit le corps HTML de l'email de confirmation de reservation. */
    public static String buildConfirmationReservationHtml(String prenom, String nom, String nomLocal,
                                                            String adresseLocal, LocalDate date,
                                                            LocalTime heureDebut, LocalTime heureFin,
                                                            String motif, int prix) {
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hFmt = DateTimeFormatter.ofPattern("HH:mm");

        return "<html><body style='font-family:Segoe UI,Arial,sans-serif; background:#F4F7F5; padding:24px;'>"
                + "<div style='max-width:520px;margin:auto;background:#FFFFFF;border-radius:16px;overflow:hidden;border:1px solid #E2E8F0;'>"
                + "<div style='background:linear-gradient(to right,#1B4332,#2D6A4F);padding:24px;text-align:center;'>"
                + "<h1 style='color:#FFFFFF;margin:0;font-size:20px;'>MindFullness</h1>"
                + "<p style='color:#D8F3DC;margin:4px 0 0;font-size:13px;'>Confirmation de votre demande de reservation</p>"
                + "</div>"
                + "<div style='padding:24px;color:#1A1A2E;'>"
                + "<p>Bonjour " + prenom + " " + nom + ",</p>"
                + "<p>Votre demande de reservation a bien ete enregistree. Voici le recapitulatif :</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>"
                + row("Local", nomLocal)
                + row("Adresse", adresseLocal)
                + row("Date", date.format(dFmt))
                + row("Horaire", heureDebut.format(hFmt) + " - " + heureFin.format(hFmt))
                + row("Motif", motif)
                + row("Montant estime", prix + " DT")
                + row("Statut", "En attente de confirmation")
                + "</table>"
                + "<p style='font-size:13px;color:#5A6475;'>Notre equipe vous contactera prochainement pour confirmer definitivement votre reservation.</p>"
                + "<p style='font-size:13px;color:#5A6475;'>Merci de votre confiance,<br/>L'equipe MindFullness</p>"
                + "</div></div></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr>"
                + "<td style='padding:6px 0;color:#5A6475;font-size:13px;width:40%;'>" + label + "</td>"
                + "<td style='padding:6px 0;color:#1A1A2E;font-size:13px;font-weight:700;'>" + value + "</td>"
                + "</tr>";
    }
}
