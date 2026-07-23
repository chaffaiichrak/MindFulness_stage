package utils;

import entities.utilisateurs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utilitaire d'export CSV et PDF (pur Java, sans dépendances tierces).
 * PDF généré via PostScript simplifié → fichier texte structuré.
 */
public class ExportUtils {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat SDF_FILE = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT CSV - UTILISATEURS
    // ─────────────────────────────────────────────────────────────────────────

    public static File exportUsersCSV(List<utilisateurs> users, File dest) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(dest), StandardCharsets.UTF_8))) {
            // BOM UTF-8 pour Excel
            pw.print('\uFEFF');
            pw.println("Prenom,Nom,Email,Telephone,Role,Statut,Date Inscription");
            for (utilisateurs u : users) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    safe(u.getPrenom_utilisateur()),
                    safe(u.getNom_utilisateur()),
                    safe(u.getEmail_utilisateur()),
                    safe(u.getTelephone_utilisateur()),
                    u.getRole_utilisateur() != null ? u.getRole_utilisateur().getLibelle() : "",
                    u.isEst_actif_utilisateur() ? "Actif" : "Inactif",
                    u.getDate_inscription_utilisateur() != null ?
                        SDF_DATE.format(u.getDate_inscription_utilisateur()) : "");
            }
        }
        return dest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT PDF (HTML→ fichier .html ouvert dans navigateur, ou txt structuré)
    //  Ici on génère un HTML auto-imprimable (pas de dépendance iText)
    // ─────────────────────────────────────────────────────────────────────────

    public static File exportUsersPDF(List<utilisateurs> users, File dest) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>")
          .append("<title>Rapport Utilisateurs MindFullness</title><style>")
          .append(getPdfStyles())
          .append("</style></head><body>")
          .append("<div class='header'><h1>&#129504; MindFullness</h1>")
          .append("<h2>Rapport des Utilisateurs</h2>")
          .append("<p class='date'>Généré le : ").append(SDF.format(new Date())).append("</p>")
          .append("<p class='count'>Nombre total : <strong>").append(users.size()).append("</strong></p></div>")
          .append("<table><thead><tr>")
          .append("<th>Photo</th><th>Prénom</th><th>Nom</th><th>Email</th>")
          .append("<th>Téléphone</th><th>Rôle</th><th>Statut</th></tr></thead><tbody>");

        for (utilisateurs u : users) {
            String initials = getInitials(u.getPrenom_utilisateur(), u.getNom_utilisateur());
            String statusClass = u.isEst_actif_utilisateur() ? "badge-actif" : "badge-inactif";
            String statusTxt   = u.isEst_actif_utilisateur() ? "Actif" : "Inactif";
            sb.append("<tr>")
              .append("<td><div class='avatar'>").append(initials).append("</div></td>")
              .append("<td>").append(safe(u.getPrenom_utilisateur())).append("</td>")
              .append("<td>").append(safe(u.getNom_utilisateur())).append("</td>")
              .append("<td>").append(safe(u.getEmail_utilisateur())).append("</td>")
              .append("<td>").append(safe(u.getTelephone_utilisateur())).append("</td>")
              .append("<td><span class='role'>").append(u.getRole_utilisateur() != null ?
                  u.getRole_utilisateur().getLibelle() : "").append("</span></td>")
              .append("<td><span class='badge ").append(statusClass).append("'>")
              .append(statusTxt).append("</span></td>")
              .append("</tr>");
        }

        sb.append("</tbody></table>")
          .append("<div class='footer'>MindFullness — Psychologie &amp; Développement Personnel</div>")
          .append("<script>window.onload=()=>window.print();</script>")
          .append("</body></html>");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(dest), StandardCharsets.UTF_8))) {
            pw.print(sb);
        }
        return dest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public static String getTimestampedName(String prefix) {
        return prefix + "_" + SDF_FILE.format(new Date());
    }

    private static String safe(String s) { return s != null ? s.replace("\"", "'") : ""; }

    private static String getInitials(String prenom, String nom) {
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        String n = (nom != null && !nom.isEmpty()) ? String.valueOf(nom.charAt(0)).toUpperCase() : "";
        return p + n;
    }

    private static String getPdfStyles() {
        return "body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:20px;background:#F0F4F8;color:#1A1A2E;}"
             + ".header{background:linear-gradient(135deg,#1B4332,#2D6A4F);color:#fff;padding:30px 36px;border-radius:12px;margin-bottom:24px;}"
             + ".header h1{margin:0 0 4px;font-size:28px;} .header h2{margin:0 0 12px;font-weight:400;opacity:.85;}"
             + ".date{margin:0;font-size:12px;opacity:.7;} .count{margin:4px 0 0;font-size:14px;}"
             + "table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,.07);}"
             + "th{background:#1B4332;color:#fff;padding:12px 14px;text-align:left;font-size:12px;font-weight:600;}"
             + "td{padding:11px 14px;border-bottom:1px solid #EDF1F5;font-size:13px;vertical-align:middle;}"
             + "tr:hover td{background:#F0FFF4;}"
             + ".avatar{width:32px;height:32px;border-radius:50%;background:linear-gradient(135deg,#2D6A4F,#1B4332);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:13px;margin:auto;}"
             + ".badge{padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;}"
             + ".badge-actif{background:#d1fae5;color:#065f46;} .badge-inactif{background:#fee2e2;color:#991b1b;}"
             + ".badge-encours{background:#dbeafe;color:#1e40af;} .badge-termine{background:#d1fae5;color:#065f46;} .badge-suspendu{background:#fef9c3;color:#854d0e;}"
             + ".role{background:#ede9fe;color:#6d28d9;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600;}"
             + ".footer{text-align:center;margin-top:24px;font-size:11px;color:#95C9B4;padding:16px;}"
             + "@media print{body{background:#fff;} .header{-webkit-print-color-adjust:exact;}}";
    }
}
