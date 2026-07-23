package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.NavigationManager;
import utils.SessionManager;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    //Sidebar / Topbar
    @FXML private Button    btnDashboard;
    @FXML private Button    btnUtilisateurs;
    @FXML private Button    btnLocauxPsychiatrie;
    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImageView;
    @FXML private Label     avatarLabel;
    @FXML private Label     userNameLabel;
    @FXML private Label     userRoleLabel;
    @FXML private Label     topbarTitle;
    @FXML private Label     topbarSubtitle;
    @FXML private StackPane contentArea;

    // Widget météo
    @FXML private HBox  weatherWidget;
    @FXML private Label weatherIcon;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherCityLabel;
    @FXML private Label topbarDate;

    //  Constantes météo
    private static final String OWM_API_KEY = "252790d466d5c3f3057431236c802988";
    private static final String OWM_CITY    = "Tunis";
    private static final String OWM_COUNTRY = "TN";
    private static final String OWM_LANG    = "fr";
    private static final String OWM_UNITS   = "metric";   // Celsius

    private static final int WEATHER_REFRESH_MINUTES = 1;

    private Button activeBtn;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUserInfo();
        setupDateClock();
        setupWeather();
        loadView("DashboardHome.fxml", "Tableau de Bord", "Vue d'ensemble de Mindfullness");
        setActive(btnDashboard);
    }


    private void setupWeather() {
        // Premier appel immédiat en arrière-plan
        fetchWeatherAsync();

        // Rafraîchissement périodique
        Timeline refreshTimer = new Timeline(
                new KeyFrame(
                        Duration.minutes(WEATHER_REFRESH_MINUTES),
                        e -> fetchWeatherAsync()
                )
        );
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void fetchWeatherAsync() {
        Thread t = new Thread(() -> {
            try {
                String url = String.format(
                        "https://api.openweathermap.org/data/2.5/weather" +
                                "?q=%s,%s&appid=%s&units=%s&lang=%s",
                        OWM_CITY, OWM_COUNTRY, OWM_API_KEY, OWM_UNITS, OWM_LANG
                );

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(8))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    WeatherData data = parseWeatherJson(response.body());
                    Platform.runLater(() -> updateWeatherUI(data));
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> setWeatherError("Clé API invalide"));
                } else {
                    Platform.runLater(() -> setWeatherError("Erreur " + response.statusCode()));
                }

            } catch (java.net.UnknownHostException e) {
                // Pas de connexion internet
                Platform.runLater(() -> setWeatherError("Hors ligne"));
            } catch (Exception e) {
                Platform.runLater(() -> setWeatherError("Météo indisponible"));
                System.err.println("[Météo] " + e.getMessage());
            }
        }, "weather-fetch-thread");
        t.setDaemon(true);
        t.start();
    }

    private WeatherData parseWeatherJson(String json) {
        WeatherData d = new WeatherData();

        d.temp        = extractDouble(json, "\"temp\"");
        d.humidity    = (int) extractDouble(json, "\"humidity\"");
        d.windSpeed   = extractDouble(json, "\"speed\"");
        d.description = extractString(json, "\"description\"");
        d.iconCode    = extractString(json, "\"icon\"");
        d.cityName    = extractString(json, "\"name\"");

        return d;
    }

    private void updateWeatherUI(WeatherData d) {
        if (weatherTempLabel == null) return;

        weatherTempLabel.setText(String.format("%.0f°C", d.temp));
        weatherDescLabel.setText(capitalize(d.description));
        weatherCityLabel.setText(d.cityName.isEmpty() ? OWM_CITY : d.cityName);
        weatherIcon.setText(owmIconToEmoji(d.iconCode));

        // Tooltip avec humidité + vent
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                String.format("Humidité : %d%%\nVent : %.1f km/h", d.humidity, d.windSpeed * 3.6)
        );
        javafx.scene.control.Tooltip.install(weatherWidget, tip);

        // Animation fade-in subtile à chaque rafraîchissement
        FadeTransition fade = new FadeTransition(Duration.millis(500), weatherWidget);
        fade.setFromValue(0.6);
        fade.setToValue(1.0);
        fade.play();
    }

    private void setWeatherError(String msg) {
        if (weatherTempLabel == null) return;
        weatherIcon.setText("🌐");
        weatherTempLabel.setText("--°C");
        weatherDescLabel.setText(msg);
    }

    private void setupDateClock() {
        // Affiche uniquement l'heure courante (HH:mm) dans la topbar
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm", new Locale("fr", "FR"));
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(30), e ->
                updateClock(timeFmt)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        updateClock(timeFmt);
    }

    private void updateClock(DateTimeFormatter fmt) {
        if (topbarDate != null)
            topbarDate.setText(LocalDateTime.now().format(fmt));
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        String rest = json.substring(idx + key.length()).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        StringBuilder sb = new StringBuilder();
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return sb.length() > 0 ? Double.parseDouble(sb.toString()) : 0.0; }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        String rest = json.substring(idx + key.length()).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        if (!rest.startsWith("\"")) return "";
        rest = rest.substring(1);
        int end = rest.indexOf("\"");
        return end >= 0 ? rest.substring(0, end) : "";
    }

    private String owmIconToEmoji(String code) {
        if (code == null || code.isEmpty()) return "🌤";
        return switch (code.substring(0, 2)) {
            case "01" -> code.endsWith("d") ? "☀️"  : "🌙";   // ciel clair
            case "02" -> "⛅";                                    // peu nuageux
            case "03" -> "🌥";                                    // nuageux
            case "04" -> "☁️";                                   // très nuageux
            case "09" -> "🌧";                                    // averses
            case "10" -> "🌦";                                    // pluie
            case "11" -> "⛈";                                    // orage
            case "13" -> "❄️";                                   // neige
            case "50" -> "🌫";                                    // brume / brouillard
            default   -> "🌤";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    //  Modèle de données météo
    private static class WeatherData {
        double temp;
        int    humidity;
        double windSpeed;
        String description = "";
        String iconCode    = "";
        String cityName    = "";
    }


    public StackPane getContentArea() { return contentArea; }

    public void updateTopbar(String title, String subtitle) {
        topbarTitle.setText(title);
        topbarSubtitle.setText(subtitle);
    }

    public void maintainActiveButton(String section) {
        switch (section.toLowerCase()) {
            case "dashboard"          -> setActive(btnDashboard);
            case "utilisateurs"       -> setActive(btnUtilisateurs);
            case "locauxpsychiatrie" -> setActive(btnLocauxPsychiatrie);
        }
    }

    @FXML private void showDashboard()    { loadView("DashboardHome.fxml",    "Tableau de Bord",           "Vue d'ensemble de MindFullness");   setActive(btnDashboard);     }
    @FXML private void showUtilisateurs() { loadView("ListeUtilisateurs.fxml","Gestion des Utilisateurs",  "Liste et gestion des comptes"); setActive(btnUtilisateurs); }
    @FXML private void showLocauxPsychiatrie() { loadView("ListeLocaux.fxml", "Gestion des Locaux Psychiatriques", "Liste et gestion des etablissements"); setActive(btnLocauxPsychiatrie); }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Stage stage = (Stage) contentArea.getScene().getWindow();
        NavigationManager.navigateTo("Login.fxml", "MindFullness – Connexion", stage, false);
    }

    public void loadView(String fxmlFile, String title, String subtitle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/" + fxmlFile));
            Node view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardAware da) da.setDashboardController(this);

            view.setOpacity(0);
            contentArea.getChildren().setAll(view);
            if (view instanceof Region r) {
                r.prefWidthProperty().bind(contentArea.widthProperty());
                r.prefHeightProperty().bind(contentArea.heightProperty());
            }
            FadeTransition ft = new FadeTransition(Duration.millis(280), view);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            topbarTitle.setText(title);
            topbarSubtitle.setText(subtitle);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void navigateTo(String fxmlFile, String title, String subtitle) {
        loadView(fxmlFile, title, subtitle);
        switch (fxmlFile) {
            case "DashboardHome.fxml"     -> setActive(btnDashboard);
            case "ListeUtilisateurs.fxml" -> setActive(btnUtilisateurs);
            case "AjouterUtilisateur.fxml", "ModifierUtilisateur.fxml" -> setActive(btnUtilisateurs);
            case "ListeLocaux.fxml", "AjouterLocal.fxml", "ModifierLocal.fxml" -> setActive(btnLocauxPsychiatrie);
            default -> {}
        }
    }

    private void setActive(Button btn) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-btn-active");
            if (!activeBtn.getStyleClass().contains("sidebar-btn"))
                activeBtn.getStyleClass().add("sidebar-btn");
        }
        btn.getStyleClass().add("sidebar-btn-active");
        activeBtn = btn;
    }

    private void setupUserInfo() {
        avatarLabel.setText(SessionManager.getInitiales());
        userNameLabel.setText(SessionManager.getNomComplet());
        userRoleLabel.setText(SessionManager.getRoleLibelle());
        loadUserPhoto();
    }

    private void loadUserPhoto() {
        String photoPath = SessionManager.getPhotoPath();
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File photoFile = Paths.get("src/main/resources/" + photoPath).toFile();
                if (photoFile.exists()) {
                    Image img = new Image(photoFile.toURI().toString(), 36, 36, false, true);
                    avatarImageView.setImage(img);
                    avatarImageView.setVisible(true);
                    avatarLabel.setVisible(false);
                    return;
                }
            } catch (Exception e) {
                System.out.println("Photo de profil introuvable : " + e.getMessage());
            }
        }
        avatarImageView.setVisible(false);
        avatarLabel.setVisible(true);
    }

    /** Interface pour les contrôleurs enfants */
    public interface DashboardAware {
        void setDashboardController(DashboardController dc);
    }
}