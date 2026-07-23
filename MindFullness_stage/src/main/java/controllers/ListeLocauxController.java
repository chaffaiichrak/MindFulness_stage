package controllers;

import entities.local_psychiatrie;
import enums.TypeL;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.local_psychiatrie_SERVICE;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public class ListeLocauxController implements Initializable, DashboardController.DashboardAware {

    @FXML private TextField            searchField;
    @FXML private ComboBox<String>      filterType;
    @FXML private ComboBox<String>      filterVille;
    @FXML private ComboBox<String>      filterDispo;
    @FXML private ComboBox<String>      sortCombo;

    @FXML private TableView<local_psychiatrie>            locauxTable;
    @FXML private TableColumn<local_psychiatrie, Void>     colPhoto;
    @FXML private TableColumn<local_psychiatrie, String>   colNom;
    @FXML private TableColumn<local_psychiatrie, String>   colType;
    @FXML private TableColumn<local_psychiatrie, String>   colVille;
    @FXML private TableColumn<local_psychiatrie, Number>   colCapacite;
    @FXML private TableColumn<local_psychiatrie, Number>   colTelephone;
    @FXML private TableColumn<local_psychiatrie, String>   colDispo;
    @FXML private TableColumn<local_psychiatrie, Void>     colActions;

    @FXML private Label countLabel;
    @FXML private Label pageInfoLabel;
    @FXML private HBox  paginationBox;

    private DashboardController dashboardController;
    private final local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();

    private List<local_psychiatrie> allLocaux = new ArrayList<>();
    private List<local_psychiatrie> filteredLocaux = new ArrayList<>();

    private static final int PAGE_SIZE = 8;
    private int currentPage = 0;

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
        loadData();
    }

    //  CHARGEMENT
    private void loadData() {
        try {
            allLocaux = service.afficherList();
        } catch (SQLException e) {
            e.printStackTrace();
            allLocaux = new ArrayList<>();
        }
        populateFilterValues();
        applyFilters();
    }

    private void populateFilterValues() {
        TreeSet<String> types = new TreeSet<>();
        TreeSet<String> villes = new TreeSet<>();
        for (local_psychiatrie l : allLocaux) {
            if (l.getType_local() != null) types.add(TypeL.fromString(l.getType_local()).getLibelle());
            if (l.getVille_local() != null && !l.getVille_local().isBlank()) villes.add(l.getVille_local());
        }
        filterType.setItems(FXCollections.observableArrayList(types));
        filterVille.setItems(FXCollections.observableArrayList(villes));
    }

    private void setupFilters() {
        filterDispo.setItems(FXCollections.observableArrayList("Disponible", "Indisponible"));
        sortCombo.setItems(FXCollections.observableArrayList(
                "Nom (A-Z)", "Nom (Z-A)", "Capacite (croissant)", "Capacite (decroissant)"));

        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        filterType.valueProperty().addListener((o, ov, nv) -> applyFilters());
        filterVille.valueProperty().addListener((o, ov, nv) -> applyFilters());
        filterDispo.valueProperty().addListener((o, ov, nv) -> applyFilters());
        sortCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
    }

    @FXML
    private void handleSearch() { applyFilters(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterType.getSelectionModel().clearSelection();
        filterVille.getSelectionModel().clearSelection();
        filterDispo.getSelectionModel().clearSelection();
        sortCombo.getSelectionModel().clearSelection();
        applyFilters();
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String typeSel = filterType.getValue();
        String villeSel = filterVille.getValue();
        String dispoSel = filterDispo.getValue();

        filteredLocaux = new ArrayList<>();
        for (local_psychiatrie l : allLocaux) {
            boolean matchQ = q.isEmpty()
                    || (l.getNom_local() != null && l.getNom_local().toLowerCase().contains(q))
                    || (l.getVille_local() != null && l.getVille_local().toLowerCase().contains(q))
                    || (l.getAdresse_local() != null && l.getAdresse_local().toLowerCase().contains(q));

            boolean matchType = typeSel == null
                    || (l.getType_local() != null && TypeL.fromString(l.getType_local()).getLibelle().equals(typeSel));

            boolean matchVille = villeSel == null || villeSel.equals(l.getVille_local());

            boolean matchDispo = dispoSel == null || dispoMatches(l.getDisponibilite_local(), dispoSel);

            if (matchQ && matchType && matchVille && matchDispo) filteredLocaux.add(l);
        }

        applySort();
        currentPage = 0;
        renderPage();
    }

    private boolean dispoMatches(String value, String filter) {
        boolean isDispo = value != null && value.toLowerCase().contains("dispon")
                && !value.toLowerCase().contains("indispon") && !value.toLowerCase().contains("non");
        return filter.equals("Disponible") == isDispo;
    }

    private void applySort() {
        String sort = sortCombo.getValue();
        if (sort == null) return;
        switch (sort) {
            case "Nom (A-Z)" -> filteredLocaux.sort(Comparator.comparing(
                    l -> l.getNom_local() == null ? "" : l.getNom_local(), String.CASE_INSENSITIVE_ORDER));
            case "Nom (Z-A)" -> filteredLocaux.sort(Comparator.comparing(
                    (local_psychiatrie l) -> l.getNom_local() == null ? "" : l.getNom_local(),
                    String.CASE_INSENSITIVE_ORDER).reversed());
            case "Capacite (croissant)" -> filteredLocaux.sort(Comparator.comparingInt(local_psychiatrie::getCapacite_local));
            case "Capacite (decroissant)" -> filteredLocaux.sort(Comparator.comparingInt(local_psychiatrie::getCapacite_local).reversed());
            default -> {}
        }
    }

    // TABLE
    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom_local"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville_local"));
        colCapacite.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getCapacite_local()));
        colTelephone.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getTelephone_local()));

        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getType_local() != null
                        ? TypeL.fromString(d.getValue().getType_local()).getLibelle() : ""));

        colDispo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colDispo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                local_psychiatrie l = getTableRow().getItem();
                String val = l.getDisponibilite_local() != null ? l.getDisponibilite_local() : "Disponible";
                boolean ok = val.toLowerCase().contains("dispon") && !val.toLowerCase().contains("indispon")
                        && !val.toLowerCase().contains("non");
                Label b = new Label(ok ? "Disponible" : "Indisponible");
                b.getStyleClass().addAll("badge", ok ? "badge-actif" : "badge-inactif");
                setGraphic(b);
            }
        });

        colPhoto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                local_psychiatrie l = getTableRow().getItem();
                StackPane pane = new StackPane();
                pane.setStyle("-fx-min-width:38; -fx-min-height:38; -fx-pref-width:38; -fx-pref-height:38;");
                Image img = tryLoadImage(l.getImage_url());
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(38); iv.setFitHeight(38); iv.setPreserveRatio(false);
                    Circle clip = new Circle(19, 19, 19);
                    iv.setClip(clip);
                    pane.getChildren().add(iv);
                } else {
                    StackPane circle = new StackPane();
                    circle.setStyle("-fx-background-color:linear-gradient(to bottom right,#2D6A4F,#1B4332);"
                            + "-fx-background-radius:50; -fx-min-width:38; -fx-min-height:38;"
                            + "-fx-pref-width:38; -fx-pref-height:38;");
                    Label initial = new Label(l.getNom_local() != null && !l.getNom_local().isBlank()
                            ? String.valueOf(l.getNom_local().charAt(0)).toUpperCase() : "L");
                    initial.setStyle("-fx-text-fill:#FFFFFF; -fx-font-weight:bold; -fx-font-size:13px;");
                    circle.getChildren().add(initial);
                    pane.getChildren().add(circle);
                }
                setGraphic(pane);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button delBtn  = new Button("🗑");
            {
                editBtn.getStyleClass().addAll("btn-icon", "btn-icon-edit");
                delBtn.getStyleClass().addAll("btn-icon", "btn-icon-delete");
                editBtn.setOnAction(e -> handleModifier(getTableRow().getItem()));
                delBtn.setOnAction(e -> handleSupprimer(getTableRow().getItem()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                HBox box = new HBox(8, editBtn, delBtn);
                setGraphic(box);
            }
        });
    }

    private Image tryLoadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Image img = new Image(path, 38, 38, false, true, true);
                return img.isError() ? null : img;
            }
            File f = new File(path);
            if (!f.exists()) f = new File("src/main/resources/" + path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 38, 38, false, true);
                return img.isError() ? null : img;
            }
            var is = getClass().getResourceAsStream("/" + path);
            if (is != null) {
                Image img = new Image(is, 38, 38, false, true);
                return img.isError() ? null : img;
            }
        } catch (Exception ignored) { }
        return null;
    }

    // PAGINATION
    private void renderPage() {
        int total = filteredLocaux.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<local_psychiatrie> pageItems = from < to ? filteredLocaux.subList(from, to) : new ArrayList<>();

        locauxTable.setItems(FXCollections.observableArrayList(pageItems));
        countLabel.setText("(" + total + (total > 1 ? " locaux)" : " local)"));
        pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        buildPaginationButtons(totalPages);
    }

    private void buildPaginationButtons(int totalPages) {
        paginationBox.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add("page-btn");
            if (i == currentPage) btn.getStyleClass().add("page-btn-active");
            btn.setOnAction(e -> { currentPage = pageIndex; renderPage(); });
            paginationBox.getChildren().add(btn);
        }
    }

    // ACTIONS CRUD
    @FXML
    private void handleAjouter() {
        openForm("AjouterLocal.fxml", "Nouveau Local", "Ajouter un etablissement", ctrl -> {});
    }

    private void handleModifier(local_psychiatrie local) {
        if (local == null) return;
        openForm("ModifierLocal.fxml", "Modifier le Local", "Modifier les informations de l'etablissement", ctrl -> {
            if (ctrl instanceof ModifierLocalController mlc) mlc.setLocalToEdit(local);
        });
    }

    private void handleSupprimer(local_psychiatrie local) {
        if (local == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer \"" + local.getNom_local() + "\" ?");
        confirm.setContentText("Cette action est irreversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.delete(local);
                loadData();
            } catch (SQLException e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Erreur");
                err.setHeaderText("Suppression impossible");
                err.setContentText("Une erreur est survenue lors de la suppression.");
                err.showAndWait();
            }
        }
    }

    private void openForm(String fxmlName, String title, String subtitle, java.util.function.Consumer<Object> onLoaded) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlName));
            Parent view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController.DashboardAware da) da.setDashboardController(dashboardController);
            onLoaded.accept(ctrl);

            if (dashboardController != null) {
                dashboardController.getContentArea().getChildren().setAll(view);
                dashboardController.updateTopbar(title, subtitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  EXPORTS
    @FXML
    private void handleExportCSV() {
        try {
            File file = new File("export_locaux_" + System.currentTimeMillis() + ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                pw.println("Nom;Type;Ville;Adresse;Capacite;Telephone;Disponibilite;Description");
                for (local_psychiatrie l : filteredLocaux) {
                    pw.println(csv(l.getNom_local()) + ";" + csv(TypeL.fromString(l.getType_local()).getLibelle())
                            + ";" + csv(l.getVille_local()) + ";" + csv(l.getAdresse_local()) + ";"
                            + l.getCapacite_local() + ";" + l.getTelephone_local() + ";"
                            + csv(l.getDisponibilite_local()) + ";" + csv(l.getDescription_local()));
                }
            }
            infoAlert("Export CSV reussi", "Fichier genere : " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            infoAlert("Erreur d'export", "Impossible de generer le fichier CSV.");
        }
    }

    @FXML
    private void handleExportPDF() {
        infoAlert("Export PDF", "Utilisez l'impression du navigateur ou un export CSV pour le moment.");
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void infoAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MindFullness");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
