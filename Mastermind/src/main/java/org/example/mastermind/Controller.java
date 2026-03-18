package org.example.mastermind;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class Controller {

    // ---------- FXML-Felder (werden von der View injiziert) ----------

    @FXML private Label      attemptsLabel;
    @FXML private ScrollPane scrollPane;
    @FXML private javafx.scene.layout.VBox boardBox;
    @FXML private TextField  guessField;
    @FXML private Button     submitButton;
    @FXML private Label      errorLabel;
    @FXML private Label      resultLabel;
    @FXML private Button     restartButton;

    // ---------- Model ----------
    private Model model;

    // ---------- Initialisierung ----------

    @FXML
    public void initialize() {
        model = new Model();
        updateAttemptsLabel();
    }

    // ---------- Event-Handler ----------

    /** Wird aufgerufen, wenn der Spieler auf "Raten" klickt. */
    @FXML
    private void onGuessSubmit() {
        String input = guessField.getText();

        // Eingabe validieren (Model übernimmt die Prüfung)
        if (!model.isValidGuess(input)) {
            errorLabel.setText("⚠ Ungültige Eingabe! Bitte 4 Farbbuchstaben eingeben, z. B.: R G B Y");
            return;
        }

        errorLabel.setText("");

        // Versuch auswerten
        char[] guess  = model.parseGuess(input);
        int[]  result = model.checkGuess(guess);   // [exactHits, colorHits]

        // Zeile im Spielbrett hinzufügen
        addBoardRow(guess, result[0], result[1]);

        // Anzeigen aktualisieren
        updateAttemptsLabel();
        guessField.clear();

        // Gewinn / Verlust prüfen
        if (model.isWon()) {
            resultLabel.setText("🎉 Gewonnen! Du hast den Code geknackt!");
            endGame();
        } else if (model.isOver()) {
            String secret = new String(model.getSecretCode());
            resultLabel.setText("😞 Leider verloren! Der geheime Code war: " + secret);
            endGame();
        }

        // ScrollPane ans Ende scrollen
        scrollPane.setVvalue(1.0);
    }

    /** Startet ein neues Spiel. */
    @FXML
    private void onRestart() {
        model = new Model();
        boardBox.getChildren().clear();
        errorLabel.setText("");
        resultLabel.setText("");
        updateAttemptsLabel();
        guessField.setDisable(false);
        submitButton.setDisable(false);
        restartButton.setVisible(false);
        guessField.requestFocus();
    }

    // ---------- Hilfsmethoden (View-Aufbau) ----------

    /**
     * Fügt dem Spielbrett eine neue Zeile hinzu:
     *   Versuchs-Farben  |  Auswertung (● ○)
     */
    private void addBoardRow(char[] guess, int exact, int color) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 6, 4, 6));
        row.setStyle("-fx-background-color: #313244; -fx-background-radius: 6;");

        // Farbkreise für den Versuch
        HBox guessBox = new HBox(8);
        guessBox.setAlignment(Pos.CENTER_LEFT);
        for (char c : guess) {
            Label chip = new Label(String.valueOf(c));
            chip.setStyle(
                "-fx-background-color: " + colorHex(c) + ";" +
                "-fx-text-fill: #1e1e2e; -fx-font-weight: bold;" +
                "-fx-font-family: 'Monospace'; -fx-font-size: 14px;" +
                "-fx-min-width: 30px; -fx-min-height: 30px;" +
                "-fx-background-radius: 15;" +
                "-fx-alignment: center;"
            );
            guessBox.getChildren().add(chip);
        }

        // Trennstrich
        Label sep = new Label("│");
        sep.setStyle("-fx-text-fill: #45475a; -fx-font-size: 18px;");

        // Auswertungs-Symbole
        HBox hintBox = new HBox(4);
        hintBox.setAlignment(Pos.CENTER_LEFT);

        StringBuilder hintText = new StringBuilder();
        for (int i = 0; i < exact; i++) hintText.append("● ");
        for (int i = 0; i < color; i++) hintText.append("○ ");
        if (exact == 0 && color == 0) hintText.append("—");

        Label hint = new Label(hintText.toString().trim());
        hint.setStyle(
            "-fx-text-fill: #cdd6f4; -fx-font-size: 16px; -fx-font-family: 'Monospace';"
        );
        hintBox.getChildren().add(hint);

        row.getChildren().addAll(guessBox, sep, hintBox);
        boardBox.getChildren().add(row);
    }

    /** Aktualisiert das Label mit den verbleibenden Versuchen. */
    private void updateAttemptsLabel() {
        attemptsLabel.setText("Versuche verbleibend: " + model.getRemainingAttempts());
    }

    /** Deaktiviert Eingabe und zeigt "Neues Spiel"-Button. */
    private void endGame() {
        guessField.setDisable(true);
        submitButton.setDisable(true);
        restartButton.setVisible(true);
    }

    /** Gibt die Hintergrundfarbe (CSS Hex) für einen Farbcode-Buchstaben zurück. */
    private String colorHex(char c) {
        return switch (c) {
            case 'R' -> "#f38ba8";   // Rot
            case 'G' -> "#a6e3a1";   // Grün
            case 'B' -> "#89b4fa";   // Blau
            case 'Y' -> "#f9e2af";   // Gelb
            case 'O' -> "#fab387";   // Orange
            case 'P' -> "#cba6f7";   // Pink/Lila
            default  -> "#ffffff";
        };
    }
}
