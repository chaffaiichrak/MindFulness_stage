package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.Duration;
import entities.utilisateurs;
import enums.Role;
import services.utilisateurs_service;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;


public class LoginController implements Initializable {


    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private TextField     loginPasswordVisible;
    @FXML private Button        btnToggleLoginPwd;
    @FXML private Label         loginErrorLabel;
    @FXML private Button        loginBtn;


    @FXML private StackPane checkboxPane;
    @FXML private Label     checkmark;
    private boolean souvenirActif = false;

    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userNodeForPackage(LoginController.class);
    private static final String PREF_EMAIL    = "remember_email";
    private static final String PREF_PASSWORD = "remember_password";
    private static final String PREF_ENABLED  = "remember_enabled";

    //  Inscription
    @FXML private TextField     regPrenom;
    @FXML private TextField     regNom;
    @FXML private TextField     regEmail;
    @FXML private TextField     regTelephone;
    @FXML private DatePicker    regDateNaissance;
    @FXML private TextArea      regBio;
    @FXML private PasswordField regPassword;
    @FXML private TextField     regPasswordVisible;
    @FXML private Button        btnToggleRegPwd;
    @FXML private PasswordField regPasswordConfirm;
    @FXML private TextField     regPasswordConfirmVisible;
    @FXML private Button        btnToggleRegPwdConfirm;
    @FXML private Label         regErrorLabel;

    //  Photo (inscription)
    @FXML private StackPane  photoPreviewPane;
    @FXML private StackPane  photoPlaceholder;
    @FXML private ImageView  photoImageView;
    @FXML private Label      photoNameLabel;
    @FXML private Button     btnRemovePhoto;

    // Navigation / layout
    @FXML private TabPane tabPane;

    @FXML private VBox rightPanel;

    // États toggle
    private boolean loginPwdVisible      = false;
    private boolean regPwdVisible        = false;
    private boolean regPwdConfirmVisible = false;

    private File   selectedPhotoFile = null;
    private String savedPhotoPath    = null;

    private final utilisateurs_service userService = new utilisateurs_service();

    private static final Pattern EMAIL_PATTERN    = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN    = Pattern.compile("^\\d{8}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{6,}$");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loginEmail.textProperty().addListener((o, ov, nv) -> loginErrorLabel.setText(""));
        loginPassword.textProperty().addListener((o, ov, nv) -> loginErrorLabel.setText(""));

        bindPasswordToggle(loginPassword,        loginPasswordVisible);
        bindPasswordToggle(regPassword,          regPasswordVisible);
        bindPasswordToggle(regPasswordConfirm,   regPasswordConfirmVisible);

        if (regDateNaissance != null) {
            regDateNaissance.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isAfter(LocalDate.now()));
                }
            });
        }

        if (regTelephone != null) {
            regTelephone.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && nv.length() > 8) regTelephone.setText(nv.substring(0, 8));
                if (nv != null && !nv.matches("\\d*"))
                    regTelephone.setText(nv.replaceAll("[^\\d]", ""));
            });
        }

        chargerCredentialsSauvegardes();
    }

    private void chargerCredentialsSauvegardes() {
        boolean enabled = PREFS.getBoolean(PREF_ENABLED, false);
        if (!enabled) return;
        String savedEmail = PREFS.get(PREF_EMAIL, "");
        String savedPwd   = PREFS.get(PREF_PASSWORD, "");
        if (!savedEmail.isEmpty()) {
            loginEmail.setText(savedEmail);
            loginPassword.setText(savedPwd);
            souvenirActif = true;
            activerCheckbox(true);
        }
    }

    @FXML
    private void handleToggleSouvenir() {
        souvenirActif = !souvenirActif;
        activerCheckbox(souvenirActif);
        if (!souvenirActif) {
            PREFS.remove(PREF_EMAIL);
            PREFS.remove(PREF_PASSWORD);
            PREFS.putBoolean(PREF_ENABLED, false);
        }
    }

    private void activerCheckbox(boolean actif) {
        if (checkboxPane == null || checkmark == null) return;
        if (actif) {
            checkboxPane.setStyle(
                    "-fx-min-width:18; -fx-min-height:18; -fx-pref-width:18; -fx-pref-height:18;" +
                            "-fx-background-color:#1B4332; -fx-border-color:#1B4332; -fx-border-width:1.5;" +
                            "-fx-background-radius:5; -fx-border-radius:5; -fx-cursor:hand;"
            );
            checkmark.setVisible(true);
        } else {
            checkboxPane.setStyle(
                    "-fx-min-width:18; -fx-min-height:18; -fx-pref-width:18; -fx-pref-height:18;" +
                            "-fx-background-color:#FFFFFF; -fx-border-color:#C5CDD8; -fx-border-width:1.5;" +
                            "-fx-background-radius:5; -fx-border-radius:5; -fx-cursor:hand;"
            );
            checkmark.setVisible(false);
        }
    }

    private void sauvegarderOuEffacerCredentials(String email, String password) {
        if (souvenirActif) {
            PREFS.put(PREF_EMAIL, email);
            PREFS.put(PREF_PASSWORD, password);
            PREFS.putBoolean(PREF_ENABLED, true);
        } else {
            PREFS.remove(PREF_EMAIL);
            PREFS.remove(PREF_PASSWORD);
            PREFS.putBoolean(PREF_ENABLED, false);
        }
    }

    private void bindPasswordToggle(PasswordField pf, TextField tf) {
        if (pf == null || tf == null) return;
        pf.textProperty().addListener((o, ov, nv) -> { if (!tf.getText().equals(nv)) tf.setText(nv); });
        tf.textProperty().addListener((o, ov, nv) -> { if (!pf.getText().equals(nv)) pf.setText(nv); });
    }


    @FXML
    private void handleLogin() {
        String email = loginEmail.getText().trim();
        String mdp   = loginPassword.getText().trim();

        if (email.isEmpty() || mdp.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs."); return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showLoginError("Adresse email invalide (ex: user@example.com).");
            shakeField(loginEmail); return;
        }

        try {
            List<utilisateurs> users = userService.afficherList();
            utilisateurs found = null;
            for (utilisateurs u : users) {
                if (!u.getEmail_utilisateur().equalsIgnoreCase(email)) continue;
                String hashBdd = u.getMdp_utilisateur();
                if (PasswordUtils.isHashed(hashBdd)) {
                    if (PasswordUtils.verify(mdp, hashBdd)) { found = u; break; }
                } else {
                    if (mdp.equals(hashBdd)) {
                        migratePasswordToHash(u, mdp);
                        found = u; break;
                    }
                }
            }

            if (found == null) {
                showLoginError("Email ou mot de passe incorrect.");
                shakeField(loginPassword); return;
            }
            if (!found.isEst_actif_utilisateur()) {
                showLoginError("Votre compte est désactivé. Contactez l'administrateur."); return;
            }

            sauvegarderOuEffacerCredentials(email, mdp);

            SessionManager.setCurrentUser(found);
            ConnexionHistorique.enregistrer(found);
            redirectToHome(found);

        } catch (SQLException e) {
            showLoginError("Erreur de connexion à la base de données.");
            e.printStackTrace();
        }
    }

    private void migratePasswordToHash(utilisateurs user, String plainPassword) {
        try {
            user.setMdp_utilisateur(PasswordUtils.hash(plainPassword));
            userService.modifier(user);
        } catch (Exception e) {
            System.err.println("[BCrypt] Échec migration : " + e.getMessage());
        }
    }


    @FXML
    private void handleMotDePasseOublie(MouseEvent e) {
        showMotDePasseOublie();
    }

    public void showMotDePasseOublie() {
        loadInRightPanel("/MotDePasseOublie.fxml", ctrl ->
                ((MotDePasseOublieController) ctrl).setLoginController(this));
    }

    public void showVerificationCode(String email, String code, utilisateurs user) {
        loadInRightPanel("/VerificationCode.fxml", ctrl -> {
            VerificationCodeController vcc = (VerificationCodeController) ctrl;
            vcc.setLoginController(this);
            vcc.setData(email, code, user);
        });
    }

    public void showNouveauMotDePasse(utilisateurs user) {
        loadInRightPanel("/NouveauMotDePasse.fxml", ctrl -> {
            NouveauMotDePasseController nmdp = (NouveauMotDePasseController) ctrl;
            nmdp.setLoginController(this);
            nmdp.setUser(user);
        });
    }

    /** Retour au panneau de connexion (onglet Connexion) */
    public void showLoginPanel() {
        if (rightPanel != null && tabPane != null) {
            rightPanel.getChildren().setAll(buildBrandHeader(), tabPane);
            tabPane.getSelectionModel().selectFirst();
            applyFade(rightPanel);
        }
    }

    /** Retour à la connexion + message de succès */
    public void showLoginPanelAvecSucces(String successMsg) {
        showLoginPanel();
        javafx.application.Platform.runLater(() -> {
            if (loginErrorLabel != null) {
                loginErrorLabel.setStyle("-fx-text-fill:#1B4332; -fx-font-size:11px; -fx-font-weight:bold;");
                loginErrorLabel.setText(successMsg);
            }
        });
    }

    private void loadInRightPanel(String fxmlPath, java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            if (setup != null) setup.accept(loader.getController());

            if (rightPanel != null) {
                rightPanel.setAlignment(javafx.geometry.Pos.CENTER);
                rightPanel.getChildren().setAll(view);
                if (view instanceof Region r) r.setMaxWidth(500);
                applyFade(rightPanel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur de navigation : " + e.getMessage());
        }
    }

    /** Reconstruit le header "MindAura" du panneau droit */
    private Node buildBrandHeader() {
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(6);
        hbox.setAlignment(javafx.geometry.Pos.CENTER);
        hbox.setStyle("-fx-padding: 0 0 20 0;");
        Label mind = new Label("Mind"); mind.getStyleClass().add("login-brand");
        Label aura = new Label("Aura"); aura.getStyleClass().addAll("login-brand", "login-brand-accent");
        hbox.getChildren().addAll(mind, aura);
        return hbox;
    }

    private void applyFade(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void redirectToHome(utilisateurs user) {
        Stage stage = (Stage) rightPanel.getScene().getWindow();
        if (user.getRole_utilisateur() == Role.ROLE_ADMIN) {
            NavigationManager.navigateTo("Dashboard.fxml", "MindFullness - Tableau de Bord", stage, true);
        } else {
            NavigationManager.navigateTo("UserHome.fxml", "MindFullness - Accueil", stage, true);
        }
    }


    @FXML
    private void handleToggleLoginPassword() {
        loginPwdVisible = !loginPwdVisible;
        togglePasswordVisibility(loginPassword, loginPasswordVisible, btnToggleLoginPwd, loginPwdVisible);
    }

    @FXML
    private void handleToggleRegPassword() {
        regPwdVisible = !regPwdVisible;
        togglePasswordVisibility(regPassword, regPasswordVisible, btnToggleRegPwd, regPwdVisible);
    }

    @FXML
    private void handleToggleRegPasswordConfirm() {
        regPwdConfirmVisible = !regPwdConfirmVisible;
        togglePasswordVisibility(regPasswordConfirm, regPasswordConfirmVisible, btnToggleRegPwdConfirm, regPwdConfirmVisible);
    }

    private void togglePasswordVisibility(PasswordField pf, TextField tf, Button btn, boolean show) {
        if (show) {
            tf.setText(pf.getText()); tf.setVisible(true); tf.setManaged(true);
            pf.setVisible(false); pf.setManaged(false);
            tf.requestFocus(); tf.positionCaret(tf.getText().length());
            if (btn != null) btn.setText("🙈");
        } else {
            pf.setText(tf.getText()); pf.setVisible(true); pf.setManaged(true);
            tf.setVisible(false); tf.setManaged(false);
            pf.requestFocus(); pf.positionCaret(pf.getText().length());
            if (btn != null) btn.setText("👁");
        }
    }


    @FXML
    private void handleChoosePhoto() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.gif","*.bmp"));
        Stage stage = (Stage) rightPanel.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) { showRegError("La photo ne doit pas dépasser 5 MB."); return; }
        selectedPhotoFile = file;
        photoNameLabel.setText(file.getName());
        try {
            Image img = new Image(file.toURI().toString(), 80, 80, false, true);
            photoImageView.setImage(img); photoImageView.setVisible(true); photoPlaceholder.setVisible(false);
        } catch (Exception e) { photoImageView.setVisible(false); photoPlaceholder.setVisible(true); }
        btnRemovePhoto.setVisible(true); btnRemovePhoto.setManaged(true);
    }

    @FXML
    private void handleRemovePhoto() {
        selectedPhotoFile = null; savedPhotoPath = null;
        photoNameLabel.setText("Aucune photo sélectionnée");
        photoImageView.setImage(null); photoImageView.setVisible(false);
        photoPlaceholder.setVisible(true);
        btnRemovePhoto.setVisible(false); btnRemovePhoto.setManaged(false);
    }

    private String copyPhotoToResources(File photoFile, String email) {
        try {
            Path destPath = Paths.get("src/main/resources/images_users/");
            if (!Files.exists(destPath)) Files.createDirectories(destPath);
            String ext = getExtension(photoFile.getName());
            String newName = email.replaceAll("[^a-zA-Z0-9]","_") + "_" + System.currentTimeMillis() + "." + ext;
            Files.copy(photoFile.toPath(), destPath.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
            return "images_users/" + newName;
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot+1).toLowerCase() : "jpg";
    }


    private boolean validateRegistration() {
        String prenom = regPrenom.getText().trim(), nom = regNom.getText().trim(),
                email  = regEmail.getText().trim(),  tel = regTelephone.getText().trim(),
                bio    = regBio.getText().trim(),     mdp = regPassword.getText(),
                mdpConf = regPasswordConfirm.getText();

        if (prenom.isEmpty() || prenom.length()<2) { showRegError("Prénom : minimum 2 caractères."); shakeField(regPrenom); return false; }
        if (nom.isEmpty() || nom.length()<3)        { showRegError("Nom : minimum 3 caractères."); shakeField(regNom); return false; }
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) { showRegError("Email invalide."); shakeField(regEmail); return false; }
        if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches())    { showRegError("Téléphone : 8 chiffres."); shakeField(regTelephone); return false; }
        if (regDateNaissance.getValue() == null)    { showRegError("Date de naissance obligatoire."); return false; }
        if (regDateNaissance.getValue().isAfter(LocalDate.now())) { showRegError("Date dans le futur."); return false; }
        if (bio.isEmpty() || bio.length()<10)       { showRegError("Bio : minimum 10 caractères."); shakeField(regBio); return false; }
        if (!PASSWORD_PATTERN.matcher(mdp).matches()) { showRegError("Mot de passe : min 6 car., 1 maj., 1 chiffre."); shakeField(regPassword); return false; }
        if (!mdp.equals(mdpConf))                   { showRegError("Mots de passe différents."); shakeField(regPasswordConfirm); return false; }
        return true;
    }

    @FXML
    private void handleRegister() {
        if (!validateRegistration()) return;

        String prenom = regPrenom.getText().trim(), nom = regNom.getText().trim(),
                email  = regEmail.getText().trim(),  tel = regTelephone.getText().trim(),
                bio    = regBio.getText().trim(),     mdp = regPassword.getText();
        try {
            for (utilisateurs u : userService.afficherList()) {
                if (u.getEmail_utilisateur().equalsIgnoreCase(email)) {
                    showRegError("Email déjà utilisé."); shakeField(regEmail); return;
                }
            }

            String photoPath = null;
            if (selectedPhotoFile != null) photoPath = copyPhotoToResources(selectedPhotoFile, email);

            utilisateurs newUser = new utilisateurs();
            newUser.setNom_utilisateur(nom); newUser.setPrenom_utilisateur(prenom);
            newUser.setEmail_utilisateur(email); newUser.setMdp_utilisateur(mdp);
            newUser.setTelephone_utilisateur(tel.isEmpty() ? null : tel);
            newUser.setRole_utilisateur(Role.ROLE_USER);
            newUser.setDate_naissance_utilisateur(Date.from(regDateNaissance.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            newUser.setDate_inscription_utilisateur(new Date());
            newUser.setEst_actif_utilisateur(true);
            newUser.setBio_utilisateur(bio);
            newUser.setPhoto_profil_utilisateur(photoPath);

            userService.add(newUser);


            showRegSuccess("✓ Compte créé ! Email de confirmation envoyé.");

            clearRegForm();
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> tabPane.getSelectionModel().selectFirst());
            }).start();

        } catch (SQLException e) {
            showRegError("Erreur : " + e.getMessage()); e.printStackTrace();
        }
    }


    @FXML private void switchToRegister(MouseEvent e) { tabPane.getSelectionModel().selectLast();  regErrorLabel.setText(""); }
    @FXML private void switchToLogin(MouseEvent e)    { tabPane.getSelectionModel().selectFirst(); loginErrorLabel.setText(""); }

    private void showLoginError(String msg) {
        if (loginErrorLabel == null) return;
        loginErrorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        loginErrorLabel.setText(msg);
        if (loginBtn != null && loginBtn.isDisable()) { loginBtn.setText("Se connecter"); loginBtn.setDisable(false); }
    }

    private void showLoginInfo(String msg) {
        if (loginErrorLabel == null) return;
        loginErrorLabel.setStyle("-fx-text-fill:#2980B9; -fx-font-size:11px; -fx-font-weight:bold;");
        loginErrorLabel.setText(msg);
    }

    private void showRegError(String msg) {
        regErrorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        regErrorLabel.setText(msg);
    }

    private void showRegSuccess(String msg) {
        regErrorLabel.setStyle("-fx-text-fill:#2E8B57; -fx-font-size:12px; -fx-font-weight:bold;");
        regErrorLabel.setText(msg);
    }

    private void clearRegForm() {
        regPrenom.clear(); regNom.clear(); regEmail.clear(); regTelephone.clear();
        regBio.clear(); regPassword.clear(); regPasswordConfirm.clear();
        regDateNaissance.setValue(null); handleRemovePhoto();
    }

    private void shakeField(javafx.scene.Node field) {
        TranslateTransition t = new TranslateTransition(Duration.millis(60), field);
        t.setCycleCount(6); t.setAutoReverse(true); t.setByX(8); t.play();
    }
}