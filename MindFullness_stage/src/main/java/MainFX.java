
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import utils.MyDataBase;


public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialisation de la base de données
        MyDataBase.getInstance();

        // Chargement de la page Login
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/Login.fxml")
        );
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
            getClass().getResource("/styles.css").toExternalForm()
        );

        // Configuration du stage
        stage.setTitle("MindAura – Psychologie & Développement Personnel");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();

        // Icône de l'application (si disponible)
        try {
            stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/logo.png"))
            );
        } catch (Exception ignored) {}

        stage.show();
    }

    @Override
    public void stop() {
        // Fermeture propre de la connexion BDD
        try {
            if (MyDataBase.getInstance().getConx() != null
                    && !MyDataBase.getInstance().getConx().isClosed()) {
                MyDataBase.getInstance().getConx().close();
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
