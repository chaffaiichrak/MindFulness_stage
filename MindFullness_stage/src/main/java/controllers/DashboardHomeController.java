package controllers;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import entities.utilisateurs;
import enums.Role;
import services.utilisateurs_service;
import utils.ConnexionHistorique;
import utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class DashboardHomeController implements Initializable, DashboardController.DashboardAware {

    // Labels bienvenue
    @FXML private Label welcomeLabel, dateLabel;

    // KPI utilisateurs
    @FXML private Label totalUsersLabel, actifsLabel, inactifsLabel, adminsLabel;

    // Conteneurs des charts
    @FXML private VBox rolesChartBox;   // PieChart : répartition par rôle

    // Tables récentes
    @FXML private TableView<utilisateurs>           recentUsersTable;
    @FXML private TableColumn<utilisateurs, String> colUserNom, colUserEmail, colUserRole, colUserStatut;

    // Historique connexion
    @FXML private TableView<Map<String, String>>           connexionTable;
    @FXML private TableColumn<Map<String, String>, String> colCxNom, colCxEmail, colCxRole, colCxDate, colCxStatut;
    @FXML private Label connexionCountLabel;

    private DashboardController dashboardController;
    private final utilisateurs_service userService  = new utilisateurs_service();


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Bonjour, " + SessionManager.getNomComplet() + " 👋");
        if (dateLabel != null)
            dateLabel.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", new Locale("fr", "FR"))));
        setupTables();
        loadData();
        setupConnexionTable();
        loadConnexionHistory();
    }

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }


    private void loadData() {
        try {
            List<utilisateurs> users  = userService.afficherList();

            // KPI utilisateurs
            animateCount(totalUsersLabel, users.size());
            long actifs   = users.stream().filter(utilisateurs::isEst_actif_utilisateur).count();
            long inactifs = users.size() - actifs;
            long admins   = users.stream().filter(u -> u.getRole_utilisateur() == Role.ROLE_ADMIN).count();
            animateCount(actifsLabel,   (int) actifs);
            animateCount(inactifsLabel, (int) inactifs);
            animateCount(adminsLabel,   (int) admins);

            //  Charts JavaFX natifs
            buildRolesPieChart(users);

            // Tables récentes (5 derniers)
            int uSz = users.size();
            ObservableList<utilisateurs> rec = FXCollections.observableArrayList(
                    users.subList(Math.max(0, uSz - 5), uSz));
            FXCollections.reverse(rec);
            recentUsersTable.setItems(rec);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadConnexionHistory() {
        List<Map<String, String>> hist = ConnexionHistorique.getHistorique(50);
        connexionTable.setItems(FXCollections.observableArrayList(hist));
        if (connexionCountLabel != null)
            connexionCountLabel.setText("(" + hist.size() + " enregistrements)");
    }



    private void buildRolesPieChart(List<utilisateurs> users) {
        if (rolesChartBox == null) return;
        rolesChartBox.getChildren().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Role r : Role.values()) {
            long count = users.stream().filter(u -> u.getRole_utilisateur() == r).count();
            if (count > 0)
                pieData.add(new PieChart.Data(r.getLibelle() + "  (" + count + ")", count));
        }

        PieChart chart = new PieChart(pieData);
        chart.setLegendSide(Side.RIGHT);
        chart.setLabelsVisible(true);
        chart.setStartAngle(90);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("dashboard-pie-chart");
        chart.setTitle(null);

        rolesChartBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }


    private void applyBarColors(XYChart.Series<String, Number> series, String[] colors) {
        javafx.application.Platform.runLater(() -> {
            int i = 0;
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: " + colors[i % colors.length] + ";");
                }
                i++;
            }
        });
    }



    private void setupTables() {
        colUserNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getPrenom_utilisateur() + " " + d.getValue().getNom_utilisateur()));
        colUserEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getEmail_utilisateur()));
        colUserRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colUserRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.getRole_utilisateur().getLibelle());
                String s = switch (u.getRole_utilisateur()) {
                    case ROLE_ADMIN       -> "badge-admin";
                    case ROLE_PSYCHOLOGUE -> "badge-psy";
                    case ROLE_COACH       -> "badge-coach";
                    default               -> "badge-patient";
                };
                b.getStyleClass().addAll("badge", s); setGraphic(b);
            }
        });
        colUserStatut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colUserStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.isEst_actif_utilisateur() ? "Actif" : "Inactif");
                b.getStyleClass().addAll("badge", u.isEst_actif_utilisateur() ? "badge-actif" : "badge-inactif");
                setGraphic(b);
            }
        });
    }

    private void setupConnexionTable() {
        colCxNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getOrDefault("nom_complet", "")));
        colCxEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getOrDefault("email", "")));
        colCxRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colCxRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                String role = getTableRow().getItem().getOrDefault("role", "");
                if (role.isBlank()) { setText("–"); setGraphic(null); return; }
                Label b = new Label(role.replace("ROLE_", "").replace("_", " "));
                b.getStyleClass().addAll("badge", "badge-patient");
                setGraphic(b); setText(null);
            }
        });
        colCxDate.setCellValueFactory(d -> {
            String raw = d.getValue().getOrDefault("date", "");
            try {
                LocalDateTime ldt = LocalDateTime.parse(raw,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                return new javafx.beans.property.SimpleStringProperty(
                        ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm", new Locale("fr", "FR"))));
            } catch (Exception ex) {
                return new javafx.beans.property.SimpleStringProperty(raw);
            }
        });
        colCxStatut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colCxStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Label b = new Label("Connecté");
                b.getStyleClass().addAll("badge", "badge-actif");
                setGraphic(b); setText(null);
            }
        });
    }



    private void animateCount(Label label, int target) {
        if (target == 0) { label.setText("0"); return; }
        int step = Math.max(1, target / 25);
        final int[] cur = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            cur[0] = Math.min(cur[0] + step, target);
            label.setText(String.valueOf(cur[0]));
        }));
        tl.setCycleCount((int) Math.ceil((double) target / step));
        tl.setOnFinished(e -> label.setText(String.valueOf(target)));
        tl.play();
    }


    @FXML private void goToUtilisateurs() {
        if (dashboardController != null)
            dashboardController.navigateTo("ListeUtilisateurs.fxml",
                    "Gestion des Utilisateurs", "Liste et gestion des comptes");
    }
}