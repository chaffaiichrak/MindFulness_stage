package controllers;

import entities.local_psychiatrie;
import enums.TypeL;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import services.local_psychiatrie_SERVICE;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class LocauxPsychiatrieController implements Initializable {

    @FXML private HBox navbar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> villeFilterCombo;
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyStateBox;
    @FXML private VBox loadingBox;
    @FXML private Label resultsCountLabel;

    private final local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();
    private List<local_psychiatrie> allLocaux = new ArrayList<>();

    private static final double CARD_W = 300;
    private static final double IMG_H  = 180;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showLoading(true);
        Platform.runLater(this::loadData);

        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        typeFilterCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
        villeFilterCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void loadData() {
        try {
            allLocaux = service.afficherList();
        } catch (Exception e) {
            e.printStackTrace();
            allLocaux = new ArrayList<>();
        }
        populateFilterOptions();
        showLoading(false);
        applyFilters();
    }

    private void populateFilterOptions() {
        TreeSet<String> types = new TreeSet<>();
        TreeSet<String> villes = new TreeSet<>();
        for (local_psychiatrie l : allLocaux) {
            if (l.getType_local() != null) types.add(TypeL.fromString(l.getType_local()).getLibelle());
            if (l.getVille_local() != null && !l.getVille_local().isBlank()) villes.add(l.getVille_local());
        }
        typeFilterCombo.setItems(FXCollections.observableArrayList(types));
        villeFilterCombo.setItems(FXCollections.observableArrayList(villes));
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        typeFilterCombo.getSelectionModel().clearSelection();
        villeFilterCombo.getSelectionModel().clearSelection();
        applyFilters();
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String typeSel = typeFilterCombo.getValue();
        String villeSel = villeFilterCombo.getValue();

        List<local_psychiatrie> filtered = new ArrayList<>();
        for (local_psychiatrie l : allLocaux) {
            boolean matchQ = q.isEmpty()
                    || (l.getNom_local() != null && l.getNom_local().toLowerCase().contains(q))
                    || (l.getVille_local() != null && l.getVille_local().toLowerCase().contains(q))
                    || (l.getAdresse_local() != null && l.getAdresse_local().toLowerCase().contains(q));

            boolean matchType = typeSel == null
                    || (l.getType_local() != null && TypeL.fromString(l.getType_local()).getLibelle().equals(typeSel));

            boolean matchVille = villeSel == null
                    || (l.getVille_local() != null && l.getVille_local().equals(villeSel));

            if (matchQ && matchType && matchVille) filtered.add(l);
        }

        renderCards(filtered);
        resultsCountLabel.setText(filtered.size() + (filtered.size() > 1 ? " locaux disponibles" : " local disponible"));
    }

    private void renderCards(List<local_psychiatrie> locaux) {
        cardsContainer.getChildren().clear();
        boolean empty = locaux.isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        cardsContainer.setVisible(!empty);
        cardsContainer.setManaged(!empty);

        for (local_psychiatrie l : locaux) {
            VBox card = buildCard(l);
            cardsContainer.getChildren().add(card);
        }
    }

    // CONSTRUCTION D'UNE CARTE
    private VBox buildCard(local_psychiatrie local) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:18;"
                + "-fx-border-color:#E2E8F0; -fx-border-radius:18; -fx-border-width:1.5;"
                + "-fx-pref-width:" + CARD_W + "; -fx-min-width:" + CARD_W + "; -fx-max-width:" + CARD_W + ";"
                + "-fx-effect:dropshadow(gaussian,rgba(27,67,50,0.09),16,0,0,4);");

        // ── Image / bandeau ──
        StackPane imgPane = new StackPane();
        imgPane.setStyle("-fx-background-color:linear-gradient(to bottom right,#2D6A4F,#1B4332);"
                + "-fx-background-radius:16 16 0 0;"
                + "-fx-min-width:" + CARD_W + "; -fx-min-height:" + IMG_H + ";"
                + "-fx-pref-width:" + CARD_W + "; -fx-pref-height:" + IMG_H + ";"
                + "-fx-max-width:" + CARD_W + "; -fx-max-height:" + IMG_H + ";");

        ImageView iv = new ImageView();
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(IMG_H);
        iv.setPreserveRatio(false);
        Rectangle clip = new Rectangle(CARD_W, IMG_H);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        iv.setClip(clip);

        Image img = tryLoadImage(local.getImage_url());
        if (img != null) {
            iv.setImage(img);
            imgPane.getChildren().add(iv);
        } else {
            FontIcon placeholder = new FontIcon("fas-clinic-medical");
            placeholder.setIconSize(46);
            placeholder.setIconColor(Color.web("rgba(255,255,255,0.55)"));
            imgPane.getChildren().add(placeholder);
        }

        // Badge type en haut de l'image
        String typeLibelle = local.getType_local() != null
                ? TypeL.fromString(local.getType_local()).getLibelle() : "Etablissement";
        Label typeBadge = new Label(typeLibelle);
        typeBadge.setStyle("-fx-background-color:rgba(255,255,255,0.92); -fx-text-fill:#1B4332;"
                + "-fx-font-size:10px; -fx-font-weight:800; -fx-background-radius:20; -fx-padding:5 12 5 12;");
        StackPane.setAlignment(typeBadge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(12, 0, 0, 12));
        imgPane.getChildren().add(typeBadge);

        // Badge disponibilite
        String dispo = local.getDisponibilite_local() != null ? local.getDisponibilite_local() : "Disponible";
        Label dispoBadge = new Label(dispo);
        boolean dispoOk = dispo.toLowerCase().contains("dispon") && !dispo.toLowerCase().contains("indispon")
                && !dispo.toLowerCase().contains("non");
        dispoBadge.setStyle("-fx-background-color:" + (dispoOk ? "#D1FAE5" : "#FEE2E2") + ";"
                + "-fx-text-fill:" + (dispoOk ? "#1B4332" : "#B91C1C") + ";"
                + "-fx-font-size:9.5px; -fx-font-weight:800; -fx-background-radius:20; -fx-padding:5 12 5 12;");
        StackPane.setAlignment(dispoBadge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(dispoBadge, new Insets(12, 12, 0, 0));
        imgPane.getChildren().add(dispoBadge);

        // ── Corps texte ──
        VBox body = new VBox(10);
        body.setStyle("-fx-padding:20 20 20 20;");

        Label nom = new Label(local.getNom_local() != null ? local.getNom_local() : "Local sans nom");
        nom.setWrapText(true);
        nom.setStyle("-fx-text-fill:#1A1A2E; -fx-font-size:16.5px; -fx-font-weight:900; -fx-font-family:'Georgia';");

        HBox adresseRow = infoRow("fas-map-marker-alt", "#7B5EA7",
                (local.getAdresse_local() != null ? local.getAdresse_local() : "Adresse non renseignee")
                        + (local.getVille_local() != null ? ", " + local.getVille_local() : ""));

        HBox infosRow = new HBox(14);
        infosRow.getChildren().add(miniInfo("fas-users", local.getCapacite_local() + " places"));
        infosRow.getChildren().add(miniInfo("fas-phone", String.valueOf(local.getTelephone_local())));

        Label description = new Label(local.getDescription_local() != null && !local.getDescription_local().isBlank()
                ? local.getDescription_local() : "Aucune description disponible pour cet etablissement.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill:#5A6475; -fx-font-size:11.5px; -fx-line-spacing:3; -fx-max-width:" + (CARD_W - 40) + ";");
        description.setMaxHeight(48);

        Region spring = new Region();
        VBox.setVgrow(spring, Priority.ALWAYS);

        Region divider = new Region();
        divider.setStyle("-fx-background-color:#E2E8F0; -fx-pref-height:1;");

        double rate = hourlyRate(local.getType_local());
        Label priceLabel = new Label(String.format("A partir de %.0f DT / heure", rate));
        priceLabel.setStyle("-fx-text-fill:#1B4332; -fx-font-size:12.5px; -fx-font-weight:800;");

        Button reserverBtn = new Button("Reserver");
        reserverBtn.setStyle("-fx-background-color:#1B4332; -fx-text-fill:#FFFFFF;"
                + "-fx-font-size:12.5px; -fx-font-weight:700; -fx-background-radius:12;"
                + "-fx-padding:9 0 9 0; -fx-cursor:hand; -fx-pref-width:9999; -fx-border-color:transparent;");
        reserverBtn.setDisable(!dispoOk);
        if (!dispoOk) {
            reserverBtn.setText("Indisponible");
            reserverBtn.setStyle(reserverBtn.getStyle().replace("#1B4332", "#CBD5E1"));
        }
        reserverBtn.setOnAction(e -> navigateToReservation(local));

        body.getChildren().addAll(nom, adresseRow, infosRow, description, spring, divider, priceLabel, reserverBtn);

        card.getChildren().addAll(imgPane, body);
        return card;
    }

    private HBox infoRow(String icon, String color, String text) {
        HBox row = new HBox(6);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(11);
        fi.setIconColor(Color.web(color));
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill:#5A6475; -fx-font-size:11.5px; -fx-font-weight:600; -fx-max-width:" + (CARD_W - 60) + ";");
        row.getChildren().addAll(fi, lbl);
        return row;
    }

    private HBox miniInfo(String icon, String text) {
        HBox row = new HBox(5);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(11);
        fi.setIconColor(Color.web("#1B4332"));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill:#1A1A2E; -fx-font-size:11px; -fx-font-weight:700;");
        row.getChildren().addAll(fi, lbl);
        return row;
    }

    /** Tarif horaire indicatif selon le type d'etablissement. */
    static double hourlyRate(String typeLocalRaw) {
        TypeL type = TypeL.fromString(typeLocalRaw);
        switch (type) {
            case CABINET_PRIVE: return 80;
            case CLINIQUE_PSYCHIATRIQUE: return 120;
            case CENTRE_DE_SANTE_MENTALE: return 100;
            case HOPITAL: return 150;
            case CENTRE_DE_BIEN_ETRE: return 90;
            case ESPACE_DE_THERAPIE: return 70;
            default: return 90;
        }
    }

    private Image tryLoadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Image img = new Image(path, CARD_W, IMG_H, false, true, true);
                return img.isError() ? null : img;
            }
            File f = new File(path);
            if (!f.exists()) f = new File("src/main/resources/" + path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), CARD_W, IMG_H, false, true);
                return img.isError() ? null : img;
            }
            var is = getClass().getResourceAsStream("/" + path);
            if (is != null) {
                Image img = new Image(is, CARD_W, IMG_H, false, true);
                return img.isError() ? null : img;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private void showLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
        cardsContainer.setVisible(!loading);
        cardsContainer.setManaged(!loading);
    }

    //  NAVIGATION
    @FXML
    private void goBack() {
        navigateTo("UserHome.fxml", null);
    }

    private void navigateToReservation(local_psychiatrie local) {
        navigateTo("ReservationLocal.fxml", loader -> {
            Object ctrl = loader.getController();
            if (ctrl instanceof ReservationLocalController rc) {
                rc.setLocal(local);
            }
        });
    }

    private void navigateTo(String fxmlName, java.util.function.Consumer<FXMLLoader> onLoaded) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlName));
            Parent view = loader.load();
            if (onLoaded != null) onLoaded.accept(loader);

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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("MindFullness");
            alert.setHeaderText("Erreur de navigation");
            alert.setContentText("Impossible d'ouvrir " + fxmlName + ".");
            alert.showAndWait();
        }
    }
}
