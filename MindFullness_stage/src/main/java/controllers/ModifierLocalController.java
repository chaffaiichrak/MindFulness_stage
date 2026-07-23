package controllers;

import entities.local_psychiatrie;
import enums.TypeL;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import services.local_psychiatrie_SERVICE;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class ModifierLocalController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label     headerNomLabel;

    @FXML private StackPane photoPreviewPane;
    @FXML private StackPane photoPlaceholder;
    @FXML private Label     photoPlaceholderIcon;
    @FXML private ImageView photoImageView;
    @FXML private Label     photoNameLabel;
    @FXML private Button    btnRemovePhoto;

    @FXML private TextField fieldNom;
    @FXML private ComboBox<String> fieldType;
    @FXML private TextField fieldAdresse;
    @FXML private TextField fieldVille;
    @FXML private TextField fieldCapacite;
    @FXML private TextField fieldTelephone;
    @FXML private ComboBox<String> fieldDisponibilite;
    @FXML private TextArea  fieldDescription;

    @FXML private Label errNom, errType, errAdresse, errVille, errCapacite, errTelephone, errDisponibilite, errGlobal;

    private DashboardController dashboardController;
    private final local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();

    private local_psychiatrie localToEdit;
    private File selectedPhotoFile;
    private boolean photoRemoved = false;

    private static final String PHOTOS_DIR = "src/main/resources/locaux/";
    private static final String PHOTOS_RELATIVE = "locaux/";

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        java.util.List<String> types = new java.util.ArrayList<>();
        for (TypeL t : TypeL.values()) types.add(t.getLibelle());
        fieldType.setItems(FXCollections.observableArrayList(types));
        fieldDisponibilite.setItems(FXCollections.observableArrayList("Disponible", "Indisponible"));
    }

    /** Appele juste apres le chargement du FXML par ListeLocauxController. */
    public void setLocalToEdit(local_psychiatrie local) {
        this.localToEdit = local;
        populateFields();
    }

    private void populateFields() {
        if (localToEdit == null) return;

        headerNomLabel.setText(localToEdit.getNom_local());
        fieldNom.setText(localToEdit.getNom_local());
        fieldAdresse.setText(localToEdit.getAdresse_local());
        fieldVille.setText(localToEdit.getVille_local());
        fieldCapacite.setText(String.valueOf(localToEdit.getCapacite_local()));
        fieldTelephone.setText(String.valueOf(localToEdit.getTelephone_local()));
        fieldDescription.setText(localToEdit.getDescription_local());

        if (localToEdit.getType_local() != null) {
            fieldType.getSelectionModel().select(TypeL.fromString(localToEdit.getType_local()).getLibelle());
        }

        String dispo = localToEdit.getDisponibilite_local();
        boolean ok = dispo != null && dispo.toLowerCase().contains("dispon") && !dispo.toLowerCase().contains("indispon");
        fieldDisponibilite.getSelectionModel().select(ok ? "Disponible" : "Indisponible");

        String imagePath = localToEdit.getImage_url();
        if (imagePath != null && !imagePath.isBlank()) {
            photoNameLabel.setText("Photo actuelle : " + imagePath);
            Image img = tryLoadImage(imagePath);
            if (img != null) {
                photoImageView.setImage(img);
                Rectangle clip = new Rectangle(90, 90);
                clip.setArcWidth(16); clip.setArcHeight(16);
                photoImageView.setClip(clip);
                photoImageView.setVisible(true);
                photoPlaceholder.setVisible(false);
                btnRemovePhoto.setVisible(true);
                btnRemovePhoto.setManaged(true);
            }
        } else {
            photoNameLabel.setText("Aucune photo actuelle");
        }
    }

    private Image tryLoadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Image img = new Image(path, 90, 90, false, true, true);
                return img.isError() ? null : img;
            }
            File f = new File(path);
            if (!f.exists()) f = new File("src/main/resources/" + path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 90, 90, false, true);
                return img.isError() ? null : img;
            }
            var is = getClass().getResourceAsStream("/" + path);
            if (is != null) {
                Image img = new Image(is, 90, 90, false, true);
                return img.isError() ? null : img;
            }
        } catch (Exception ignored) { }
        return null;
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(fieldNom.getScene().getWindow());
        if (file != null) {
            selectedPhotoFile = file;
            photoRemoved = false;
            photoNameLabel.setText(file.getName());
            try {
                Image img = new Image(file.toURI().toString(), 90, 90, false, true);
                if (!img.isError()) {
                    photoImageView.setImage(img);
                    Rectangle clip = new Rectangle(90, 90);
                    clip.setArcWidth(16); clip.setArcHeight(16);
                    photoImageView.setClip(clip);
                    photoImageView.setVisible(true);
                    photoPlaceholder.setVisible(false);
                    btnRemovePhoto.setVisible(true);
                    btnRemovePhoto.setManaged(true);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleRemovePhoto() {
        selectedPhotoFile = null;
        photoRemoved = true;
        photoImageView.setVisible(false);
        photoPlaceholder.setVisible(true);
        photoNameLabel.setText("Aucune photo");
        btnRemovePhoto.setVisible(false);
        btnRemovePhoto.setManaged(false);
    }

    @FXML
    private void handleAnnuler() {
        if (dashboardController != null) {
            dashboardController.navigateTo("ListeLocaux.fxml", "Gestion des Locaux Psychiatriques",
                    "Liste et gestion des etablissements");
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean valid = true;

        if (localToEdit == null) { errGlobal.setText("Aucun local a modifier."); return; }

        String nom = fieldNom.getText() == null ? "" : fieldNom.getText().trim();
        String adresse = fieldAdresse.getText() == null ? "" : fieldAdresse.getText().trim();
        String ville = fieldVille.getText() == null ? "" : fieldVille.getText().trim();
        String capaciteStr = fieldCapacite.getText() == null ? "" : fieldCapacite.getText().trim();
        String telStr = fieldTelephone.getText() == null ? "" : fieldTelephone.getText().trim();
        String typeSel = fieldType.getValue();
        String dispoSel = fieldDisponibilite.getValue();

        if (nom.isEmpty()) { errNom.setText("Le nom est obligatoire."); valid = false; }
        if (typeSel == null) { errType.setText("Veuillez choisir un type."); valid = false; }
        if (adresse.isEmpty()) { errAdresse.setText("L'adresse est obligatoire."); valid = false; }
        if (ville.isEmpty()) { errVille.setText("La ville est obligatoire."); valid = false; }

        int capacite = 0;
        if (capaciteStr.isEmpty()) { errCapacite.setText("La capacite est obligatoire."); valid = false; }
        else {
            try {
                capacite = Integer.parseInt(capaciteStr);
                if (capacite <= 0) { errCapacite.setText("La capacite doit etre positive."); valid = false; }
            } catch (NumberFormatException e) { errCapacite.setText("Nombre invalide."); valid = false; }
        }

        int telephone = 0;
        if (telStr.isEmpty()) { errTelephone.setText("Le telephone est obligatoire."); valid = false; }
        else {
            try {
                telephone = Integer.parseInt(telStr);
            } catch (NumberFormatException e) { errTelephone.setText("Numero invalide."); valid = false; }
        }

        if (dispoSel == null) { errDisponibilite.setText("Veuillez choisir une disponibilite."); valid = false; }

        if (!valid) return;

        try {
            String imageUrl = localToEdit.getImage_url();
            if (photoRemoved) imageUrl = null;
            else if (selectedPhotoFile != null) imageUrl = savePhoto();

            localToEdit.setNom_local(nom);
            localToEdit.setAdresse_local(adresse);
            localToEdit.setVille_local(ville);
            localToEdit.setCapacite_local(capacite);
            localToEdit.setTelephone_local(telephone);
            localToEdit.setDisponibilite_local(dispoSel);
            localToEdit.setImage_url(imageUrl);
            localToEdit.setDiscription_local(fieldDescription.getText());
            localToEdit.setType_local(typeSel);

            service.modifier(localToEdit);
            handleAnnuler();

        } catch (Exception e) {
            e.printStackTrace();
            errGlobal.setText("Une erreur est survenue lors de l'enregistrement.");
        }
    }

    private String savePhoto() {
        try {
            Path dir = Paths.get(PHOTOS_DIR);
            Files.createDirectories(dir);
            String ext = selectedPhotoFile.getName().substring(selectedPhotoFile.getName().lastIndexOf('.'));
            String fileName = "local_" + System.currentTimeMillis() + ext;
            Path target = dir.resolve(fileName);
            Files.copy(selectedPhotoFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return PHOTOS_RELATIVE + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return localToEdit.getImage_url();
        }
    }

    private void clearErrors() {
        errNom.setText(""); errType.setText(""); errAdresse.setText(""); errVille.setText("");
        errCapacite.setText(""); errTelephone.setText(""); errDisponibilite.setText(""); errGlobal.setText("");
    }
}
