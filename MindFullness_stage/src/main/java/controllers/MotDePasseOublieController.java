package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import entities.utilisateurs;
import services.utilisateurs_service;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;


public class MotDePasseOublieController {

    @FXML private TextField emailField;
    @FXML private Label     errorLabel;
    @FXML private Button    btnEnvoyer;

    private LoginController loginController;
    private final utilisateurs_service userService = new utilisateurs_service();

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Appelé par LoginController pour passer la référence */
    public void setLoginController(LoginController lc) {
        this.loginController = lc;
    }



    @FXML
    private void handleEnvoyer() {
        String email = emailField.getText().trim();

        // Validation format email
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showError("Veuillez saisir une adresse email valide.");
            return;
        }

        // Désactiver le bouton pour éviter les doubles clics
        btnEnvoyer.setDisable(true);
        btnEnvoyer.setText("Vérification...");
        errorLabel.setText("");

        // Vérification existence en base + envoi email en arrière-plan
        new Thread(() -> {
            try {
                List<utilisateurs> users = userService.afficherList();
                utilisateurs found = null;
                for (utilisateurs u : users) {
                    if (u.getEmail_utilisateur().equalsIgnoreCase(email)) {
                        found = u;
                        break;
                    }
                }

                final utilisateurs userFound = found;

                javafx.application.Platform.runLater(() -> {
                    if (userFound == null) {
                        showError("Aucun compte n'est associé à cette adresse email.");
                        btnEnvoyer.setDisable(false);
                        btnEnvoyer.setText("Envoyer le code de vérification");
                        return;
                    }

                    // Générer un code à 6 chiffres
                    String code = String.format("%06d", (int)(Math.random() * 900000) + 100000);


                });

            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur base de données : " + e.getMessage());
                    btnEnvoyer.setDisable(false);
                    btnEnvoyer.setText("Envoyer le code de vérification");
                });
            }
        }).start();
    }

    @FXML
    private void handleRetour(MouseEvent e) {
        if (loginController != null) {
            loginController.showLoginPanel();
        }
    }


    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText(msg);
    }
}
