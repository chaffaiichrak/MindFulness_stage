package controllers;

import entities.local_psychiatrie;
import entities.reservation_local;
import enums.MotifReservation;
import enums.StatusReservation;
import enums.TypeL;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.local_psychiatrie_SERVICE;
import services.reservation_local_SERVICE;
import utils.EmailService;
import utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class ReservationLocalController implements Initializable {

    @FXML private HBox navbar;

    // Recapitulatif du local
    @FXML private StackPane localImagePane;
    @FXML private ImageView localImageView;
    @FXML private Label     localTypeBadge;
    @FXML private Label     localNomLabel;
    @FXML private Label     localAdresseLabel;
    @FXML private Label     localCapaciteLabel;
    @FXML private Label     localTelLabel;
    @FXML private Label     localDescriptionLabel;
    @FXML private Label     localTarifLabel;

    // Formulaire
    @FXML private DatePicker           datePicker;
    @FXML private ComboBox<String>     motifCombo;
    @FXML private ComboBox<String>     heureDebutCombo;
    @FXML private ComboBox<String>     heureFinCombo;
    @FXML private TextField            nomField;
    @FXML private TextField            prenomField;
    @FXML private Label                errorLabel;
    @FXML private Label                montantLabel;
    @FXML private Label                dureeLabel;
    @FXML private Button               confirmButton;

    private final reservation_local_SERVICE reservationService = new reservation_local_SERVICE();
    private final local_psychiatrie_SERVICE localService = new local_psychiatrie_SERVICE();

    private local_psychiatrie local;
    private double hourlyRate = 90;

    private static final List<String> HEURES = buildHeures();

    private static List<String> buildHeures() {
        List<String> list = new ArrayList<>();
        for (int h = 8; h <= 20; h++) {
            list.add(String.format("%02d:00", h));
            if (h < 20) list.add(String.format("%02d:30", h));
        }
        return list;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        motifCombo.setItems(FXCollections.observableArrayList(motifLibelles()));
        heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES));
        heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        String nomComplet = SessionManager.getNomComplet();
        if (nomComplet != null && !nomComplet.isBlank()) {
            String[] parts = nomComplet.trim().split(" ", 2);
            prenomField.setText(parts[0]);
            if (parts.length > 1) nomField.setText(parts[1]);
        }

        heureDebutCombo.valueProperty().addListener((o, ov, nv) -> updateMontant());
        heureFinCombo.valueProperty().addListener((o, ov, nv) -> updateMontant());
    }

    private List<String> motifLibelles() {
        List<String> list = new ArrayList<>();
        for (MotifReservation m : MotifReservation.values()) list.add(m.getLibelle());
        return list;
    }

    /** Appele par LocauxPsychiatrieController juste apres le chargement du FXML. */
    public void setLocal(local_psychiatrie local) {
        this.local = local;
        this.hourlyRate = LocauxPsychiatrieController.hourlyRate(local.getType_local());
        populateLocalSummary();
        updateMontant();
    }

    private void populateLocalSummary() {
        if (local == null) return;

        localNomLabel.setText(local.getNom_local() != null ? local.getNom_local() : "Local");
        String adresse = (local.getAdresse_local() != null ? local.getAdresse_local() : "Adresse non renseignee")
                + (local.getVille_local() != null ? ", " + local.getVille_local() : "");
        localAdresseLabel.setText(adresse);
        localCapaciteLabel.setText(local.getCapacite_local() + " places");
        localTelLabel.setText(String.valueOf(local.getTelephone_local()));
        localDescriptionLabel.setText(local.getDescription_local() != null && !local.getDescription_local().isBlank()
                ? local.getDescription_local() : "Aucune description disponible.");
        localTypeBadge.setText(TypeL.fromString(local.getType_local()).getLibelle());
        localTarifLabel.setText(String.format("%.0f DT / heure", hourlyRate));

        double w = 360, h = 190;
        Image img = tryLoadImage(local.getImage_url(), w, h);
        if (img != null) {
            localImageView.setFitWidth(w);
            localImageView.setFitHeight(h);
            localImageView.setPreserveRatio(false);
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            localImageView.setClip(clip);
            localImageView.setImage(img);
            localImageView.setVisible(true);
        }
    }

    private Image tryLoadImage(String path, double w, double h) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Image img = new Image(path, w, h, false, true, true);
                return img.isError() ? null : img;
            }
            File f = new File(path);
            if (!f.exists()) f = new File("src/main/resources/" + path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), w, h, false, true);
                return img.isError() ? null : img;
            }
            var is = getClass().getResourceAsStream("/" + path);
            if (is != null) {
                Image img = new Image(is, w, h, false, true);
                return img.isError() ? null : img;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private void updateMontant() {
        Double heures = dureeEnHeures();
        if (heures == null || heures <= 0) {
            montantLabel.setText("0 DT");
            dureeLabel.setText("0 h");
            return;
        }
        double montant = heures * hourlyRate;
        montantLabel.setText(String.format("%.0f DT", montant));
        dureeLabel.setText(formatDuree(heures));
    }

    private String formatDuree(double heures) {
        int h = (int) heures;
        int m = (int) Math.round((heures - h) * 60);
        if (m == 0) return h + " h";
        return h + " h " + m;
    }

    private Double dureeEnHeures() {
        String debut = heureDebutCombo.getValue();
        String fin = heureFinCombo.getValue();
        if (debut == null || fin == null) return null;
        LocalTime t1 = LocalTime.parse(debut);
        LocalTime t2 = LocalTime.parse(fin);
        double heures = java.time.Duration.between(t1, t2).toMinutes() / 60.0;
        return heures > 0 ? heures : null;
    }

    // VALIDATION + ENREGISTREMENT
    @FXML
    private void confirmerReservation() {
        hideError();

        if (local == null) {
            showError("Aucun local selectionne.");
            return;
        }
        LocalDate date = datePicker.getValue();
        String motifLib = motifCombo.getValue();
        String debut = heureDebutCombo.getValue();
        String fin = heureFinCombo.getValue();
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String prenom = prenomField.getText() == null ? "" : prenomField.getText().trim();

        if (date == null) { showError("Veuillez choisir une date de reservation."); return; }
        if (date.isBefore(LocalDate.now())) { showError("La date ne peut pas etre dans le passe."); return; }
        if (motifLib == null) { showError("Veuillez choisir un motif de reservation."); return; }
        if (debut == null || fin == null) { showError("Veuillez choisir l'heure de debut et de fin."); return; }
        if (nom.isEmpty() || prenom.isEmpty()) { showError("Veuillez renseigner votre nom et prenom."); return; }

        LocalTime t1 = LocalTime.parse(debut);
        LocalTime t2 = LocalTime.parse(fin);
        if (!t2.isAfter(t1)) { showError("L'heure de fin doit etre apres l'heure de debut."); return; }

        Date heureDebutDate = toDate(date, t1);
        Date heureFinDate = toDate(date, t2);

        try {
            if (hasConflict(local.getId_local(), date, t1, t2)) {
                showError("Ce local est deja reserve sur ce creneau. Merci de choisir un autre horaire.");
                return;
            }

            MotifReservation motif = motifFromLibelle(motifLib);
            double heures = java.time.Duration.between(t1, t2).toMinutes() / 60.0;
            int prix = (int) Math.round(heures * hourlyRate);

            reservation_local reservation = new reservation_local(
                    0,
                    local.getId_local(),
                    java.sql.Date.valueOf(date),
                    heureDebutDate,
                    heureFinDate,
                    StatusReservation.EN_ATTENTE,
                    motif,
                    prix,
                    nom,
                    prenom
            );

            confirmButton.setDisable(true);
            reservationService.add(reservation);

            // La reservation est enregistree : tout ce qui suit (email) ne doit
            // plus jamais faire echouer ce flux ni afficher d'erreur a l'utilisateur.
            envoyerEmailConfirmation(date, t1, t2, motifLib, nom, prenom, prix);

            showSuccessAndGoBack(prix);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[Reservation] Erreur lors de l'enregistrement : "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
            confirmButton.setDisable(false);
            showError("Une erreur est survenue lors de l'enregistrement. Veuillez reessayer.");
        }
    }

    /**
     * Envoie un email de confirmation au patient connecte.
     * Totalement isole du flux de reservation : on attrape Throwable (et pas
     * seulement Exception) pour garantir qu'aucun probleme lie a l'email
     * (dependance manquante, config SMTP absente, thread, etc.) ne puisse
     * remonter et perturber la sauvegarde de la reservation, deja effectuee
     * a ce stade.
     */
    private void envoyerEmailConfirmation(LocalDate date, LocalTime t1, LocalTime t2,
                                          String motifLib, String nom, String prenom, int prix) {
        try {
            String emailPatient = SessionManager.getEmail();
            if (emailPatient == null || emailPatient.isBlank()) {
                System.err.println("[Email] Aucune adresse email trouvee pour le patient connecte.");
                return;
            }

            String adresse = (local.getAdresse_local() != null ? local.getAdresse_local() : "Adresse non renseignee")
                    + (local.getVille_local() != null ? ", " + local.getVille_local() : "");

            String contenuHtml = EmailService.buildConfirmationReservationHtml(
                    prenom, nom, local.getNom_local(), adresse, date, t1, t2, motifLib, prix
            );

            EmailService.envoyerEmailAsync(
                    emailPatient,
                    "Confirmation de votre reservation - " + local.getNom_local(),
                    contenuHtml
            );
        } catch (Throwable t) {
            // On logge sans jamais propager : l'utilisateur ne doit pas etre
            // impacte par un souci d'envoi d'email.
            System.err.println("[Email] Impossible de preparer/envoyer l'email de confirmation : "
                    + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    private boolean hasConflict(int localId, LocalDate date, LocalTime t1, LocalTime t2) throws Exception {
        List<reservation_local> existantes = reservationService.afficherList();
        for (reservation_local r : existantes) {
            if (r.getId_local() != localId) continue;
            if (r.getStatus_reservation() == StatusReservation.ANNULEE) continue;
            if (r.getDate_reservation() == null) continue;

            LocalDate dExistante = toLocalDate(r.getDate_reservation());
            if (!dExistante.equals(date)) continue;

            LocalTime existDebut = toLocalTime(r.getHeure_debut_reservation());
            LocalTime existFin = toLocalTime(r.getHeure_fin_reservation());

            boolean overlap = t1.isBefore(existFin) && existDebut.isBefore(t2);
            if (overlap) return true;
        }
        return false;
    }

    private LocalDate toLocalDate(Date d) {
        // java.sql.Date ne supporte pas toInstant() (UnsupportedOperationException),
        // car il ne possede pas de composante horaire. On le detecte et on le
        // convertit directement, sinon on retombe sur la conversion via Instant
        // pour un java.util.Date "classique".
        if (d instanceof java.sql.Date) {
            return ((java.sql.Date) d).toLocalDate();
        }
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime toLocalTime(Date d) {
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime();
    }

    private MotifReservation motifFromLibelle(String libelle) {
        for (MotifReservation m : MotifReservation.values()) {
            if (m.getLibelle().equalsIgnoreCase(libelle)) return m;
        }
        return MotifReservation.CONSULTATION_INDIVIDUELLE;
    }

    private Date toDate(LocalDate date, LocalTime time) {
        LocalDateTime dt = LocalDateTime.of(date, time);
        Calendar cal = Calendar.getInstance();
        cal.set(dt.getYear(), dt.getMonthValue() - 1, dt.getDayOfMonth(), dt.getHour(), dt.getMinute(), 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showSuccessAndGoBack(int prix) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MindFullness");
        alert.setHeaderText("Reservation envoyee avec succes !");
        alert.setContentText("Votre demande de reservation pour \"" + local.getNom_local()
                + "\" a bien ete enregistree pour un montant estime de " + prix
                + " DT. Elle est en attente de confirmation par notre equipe.");
        alert.showAndWait();
        goBackToLocaux();
    }

    //  NAVIGATION
    @FXML
    private void goBackToLocaux() {
        navigateTo("LocauxPsychiatrie.fxml");
    }

    private void navigateTo(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlName));
            Parent view = loader.load();
            Stage stage = (Stage) navbar.getScene().getWindow();
            Parent currentRoot = stage.getScene().getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentRoot);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                stage.getScene().setRoot(view);
                view.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}