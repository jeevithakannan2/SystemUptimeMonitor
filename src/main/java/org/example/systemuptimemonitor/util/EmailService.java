package org.example.systemuptimemonitor.util;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    private static final boolean enabled;
    private static final Session mailSession;
    private static final String fromAddress;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    static {
        Properties envProps = new Properties();
        File envFile = new File(".env");
        if (!envFile.exists()) {
            String catalinaBase = System.getProperty("catalina.base", "");
            envFile = new File(catalinaBase, ".env");
        }
        try {
            if (envFile.exists()) {
                try (FileInputStream fis = new FileInputStream(envFile)) {
                    envProps.load(fis);
                }
            } else {
                if (System.getenv("SMTP_HOST") != null) {
                    envProps.setProperty("SMTP_HOST", System.getenv("SMTP_HOST"));
                }
                if (System.getenv("SMTP_PORT") != null) {
                    envProps.setProperty("SMTP_PORT", System.getenv("SMTP_PORT"));
                }
                if (System.getenv("SMTP_USERNAME") != null) {
                    envProps.setProperty("SMTP_USERNAME", System.getenv("SMTP_USERNAME"));
                }
                if (System.getenv("SMTP_PASSWORD") != null) {
                    envProps.setProperty("SMTP_PASSWORD", System.getenv("SMTP_PASSWORD"));
                }
                if (System.getenv("SMTP_FROM") != null) {
                    envProps.setProperty("SMTP_FROM", System.getenv("SMTP_FROM"));
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load .env file for SMTP config", e);
        }

        String smtpHost = envProps.getProperty("SMTP_HOST", "").trim();
        if (smtpHost.isEmpty()) {
            LOG.warning("SMTP_HOST is not configured — email notifications are disabled");
            enabled = false;
            mailSession = null;
            fromAddress = null;
        } else {
            enabled = true;
            String smtpPort = envProps.getProperty("SMTP_PORT", "587").trim();
            String smtpUsername = envProps.getProperty("SMTP_USERNAME", "").trim();
            String smtpPassword = envProps.getProperty("SMTP_PASSWORD", "");
            fromAddress = envProps.getProperty("SMTP_FROM", smtpUsername).trim();

            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.host", smtpHost);
            mailProps.put("mail.smtp.port", smtpPort);
            mailProps.put("mail.smtp.auth", "true");
            mailProps.put("mail.smtp.starttls.enable", "true");

            final String user = smtpUsername;
            final String pass = smtpPassword;
            mailSession = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            LOG.info("Email notifications enabled via " + smtpHost + ":" + smtpPort);
        }
    }

    public static void sendIncidentCreated(List<String> emails, String monitorName,
                                           String targetUrl, int statusCode,
                                           String expectedCodes, long timestamp) {
        if (!enabled || emails == null || emails.isEmpty()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String subject = "\uD83D\uDD34 " + monitorName + " is DOWN \u2014 Uptime Monitor";
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(timestamp));
                    String body = "<html><body style=\"font-family:sans-serif;color:#333;\">"
                            + "<h2 style=\"color:#dc2626;\">\uD83D\uDD34 " + escapeHtml(monitorName) + " is DOWN</h2>"
                            + "<table style=\"border-collapse:collapse;\">"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Monitor</td>"
                            + "<td>" + escapeHtml(monitorName) + "</td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">URL</td>"
                            + "<td><a href=\"" + escapeHtml(targetUrl) + "\">" + escapeHtml(targetUrl) + "</a></td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Received Status</td>"
                            + "<td>" + statusCode + "</td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Expected Codes</td>"
                            + "<td>" + escapeHtml(expectedCodes) + "</td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Detected At</td>"
                            + "<td>" + time + "</td></tr>"
                            + "</table>"
                            + "<p style=\"margin-top:16px;color:#666;font-size:12px;\">System Uptime Monitor</p>"
                            + "</body></html>";
                    sendHtmlEmail(emails, subject, body);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to send incident-created email for " + monitorName, e);
                }
            }
        });
    }

    public static void sendIncidentResolved(List<String> emails, String monitorName,
                                            String targetUrl, long downTime,
                                            long resolvedTime) {
        if (!enabled || emails == null || emails.isEmpty()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String subject = "\u2705 " + monitorName + " is BACK UP \u2014 Uptime Monitor";
                    String duration = formatDuration(downTime);
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(resolvedTime));
                    String body = "<html><body style=\"font-family:sans-serif;color:#333;\">"
                            + "<h2 style=\"color:#16a34a;\">\u2705 " + escapeHtml(monitorName) + " is BACK UP</h2>"
                            + "<table style=\"border-collapse:collapse;\">"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Monitor</td>"
                            + "<td>" + escapeHtml(monitorName) + "</td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">URL</td>"
                            + "<td><a href=\"" + escapeHtml(targetUrl) + "\">" + escapeHtml(targetUrl) + "</a></td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Downtime Duration</td>"
                            + "<td>" + duration + "</td></tr>"
                            + "<tr><td style=\"padding:4px 12px 4px 0;font-weight:bold;\">Resolved At</td>"
                            + "<td>" + time + "</td></tr>"
                            + "</table>"
                            + "<p style=\"margin-top:16px;color:#666;font-size:12px;\">System Uptime Monitor</p>"
                            + "</body></html>";
                    sendHtmlEmail(emails, subject, body);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to send incident-resolved email for " + monitorName, e);
                }
            }
        });
    }

    private static void sendHtmlEmail(List<String> recipients, String subject, String htmlBody)
            throws MessagingException {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromAddress));
        for (String email : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
        }
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
        LOG.info("Email sent to " + recipients.size() + " recipient(s): " + subject);
    }

    static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
