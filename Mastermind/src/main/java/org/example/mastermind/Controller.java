package org.example.mastermind;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class Controller {

    // ──────────────────────────────────────────────
    //  FXML-Felder
    // ──────────────────────────────────────────────
    @FXML private Label      attemptsLabel;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox       boardBox;
    @FXML private HBox       slotsBox;
    @FXML private HBox       colorPalette;
    @FXML private Label      errorLabel;
    @FXML private Button     submitButton;
    @FXML private Button     clearButton;
    @FXML private VBox       resultBox;
    @FXML private Label      resultLabel;
    @FXML private Button     restartButton;

    // ──────────────────────────────────────────────
    //  Farb-Definitionen
    // ──────────────────────────────────────────────
    private static final char[]   COLOR_CHARS = {'R','G','B','Y','O','P'};
    private static final String[] COLOR_HEX   = {
        "#f38ba8",  // R – Rot
        "#a6e3a1",  // G – Grün
        "#89b4fa",  // B – Blau
        "#f9e2af",  // Y – Gelb
        "#fab387",  // O – Orange
        "#cba6f7"   // P – Pink
    };
    private static final String[] COLOR_NAMES = {
        "ROT","GRÜN","BLAU","GELB","ORANGE","PINK"
    };

    // ──────────────────────────────────────────────
    //  Interner Zustand
    // ──────────────────────────────────────────────
    private Model model;

    /** Aktuell gewählte Farben (null = leer) */
    private char[] currentGuess = new char[4];
    private int    filledSlots  = 0;

    /** Die 4 Slot-Labels für den aktuellen Versuch */
    private Label[] slotLabels = new Label[4];

    // ──────────────────────────────────────────────
    //  Initialisierung
    // ──────────────────────────────────────────────
    @FXML
    public void initialize() {
        model = new Model();
        buildSlots();
        buildColorPalette();
        updateAttemptsLabel();
    }

    /** Erstellt die 4 leeren Slots für den aktuellen Versuch. */
    private void buildSlots() {
        slotsBox.getChildren().clear();
        filledSlots = 0;
        currentGuess = new char[4];

        for (int i = 0; i < 4; i++) {
            final int index = i;
            Label slot = new Label();
            slot.setPrefSize(52, 52);
            slot.setMinSize(52, 52);
            slot.setAlignment(Pos.CENTER);
            slot.setStyle(emptySlotStyle());
            slot.setOnMouseClicked(e -> removeFromSlot(index));
            slotLabels[i] = slot;
            slotsBox.getChildren().add(slot);
        }
    }

    /** Erstellt die 6 klickbaren Farbkreise. */
    private void buildColorPalette() {
        colorPalette.getChildren().clear();
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            final char c     = COLOR_CHARS[i];
            final String hex = COLOR_HEX[i];
            final String name = COLOR_NAMES[i];

            Label btn = new Label(String.valueOf(c));
            btn.setPrefSize(48, 48);
            btn.setMinSize(48, 48);
            btn.setAlignment(Pos.CENTER);
            btn.setStyle(colorBtnStyle(hex));
            btn.setTooltip(new Tooltip(name));

            // Hover-Effekt
            DropShadow glow = new DropShadow(14, Color.web(hex + "aa"));
            btn.setOnMouseEntered(e -> btn.setEffect(glow));
            btn.setOnMouseExited(e  -> btn.setEffect(null));

            btn.setOnMouseClicked(e -> addColor(c, hex));
            colorPalette.getChildren().add(btn);
        }
    }

    // ──────────────────────────────────────────────
    //  Slot-Interaktion
    // ──────────────────────────────────────────────

    /** Fügt eine Farbe in den nächsten freien Slot ein. */
    private void addColor(char c, String hex) {
        if (filledSlots >= 4) return;
        errorLabel.setText("");

        currentGuess[filledSlots] = c;
        Label slot = slotLabels[filledSlots];
        slot.setText(String.valueOf(c));
        slot.setStyle(filledSlotStyle(hex));

        filledSlots++;
    }

    /** Entfernt die Farbe an Position index und schiebt nachfolgende nach links. */
    private void removeFromSlot(int index) {
        if (index >= filledSlots) return; // leerer Slot – nichts tun

        // Nachfolgende Slots nach links verschieben
        for (int i = index; i < filledSlots - 1; i++) {
            currentGuess[i] = currentGuess[i + 1];
            Label slot = slotLabels[i];
            Label next = slotLabels[i + 1];
            slot.setText(next.getText());
            slot.setStyle(next.getStyle());
        }

        // Letzten gefüllten Slot leeren
        filledSlots--;
        currentGuess[filledSlots] = 0;
        slotLabels[filledSlots].setText("");
        slotLabels[filledSlots].setStyle(emptySlotStyle());
    }

    // ──────────────────────────────────────────────
    //  Event-Handler
    // ──────────────────────────────────────────────

    @FXML
    private void onGuessSubmit() {
        if (filledSlots < 4) {
            errorLabel.setText("⚠  Bitte alle 4 Felder füllen!");
            return;
        }
        errorLabel.setText("");

        int[] result = model.checkGuess(currentGuess.clone());
        addBoardRow(currentGuess.clone(), result[0], result[1]);

        updateAttemptsLabel();
        buildSlots(); // Slots zurücksetzen

        if (model.isWon()) {
            showResult("🎉  Gewonnen!  Du hast den Code geknackt!", true);
        } else if (model.isOver()) {
            String secret = new String(model.getSecretCode());
            showResult("😞  Leider verloren!\nGeheimer Code war:  " + secret, false);
        }

        scrollPane.setVvalue(1.0);
    }

    @FXML
    private void onClearSlots() {
        buildSlots();
        errorLabel.setText("");
    }

    @FXML
    private void onRestart() {
        model = new Model();
        boardBox.getChildren().clear();
        errorLabel.setText("");
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        submitButton.setDisable(false);
        clearButton.setDisable(false);
        buildSlots();
        updateAttemptsLabel();
    }

    // ──────────────────────────────────────────────
    //  Spielbrett-Zeile
    // ──────────────────────────────────────────────

    private void addBoardRow(char[] guess, int exact, int color) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setStyle(
            "-fx-background-color: #16161f;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #1e1e2e;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        // Farbige Chips für den Versuch
        HBox guessBox = new HBox(8);
        guessBox.setAlignment(Pos.CENTER_LEFT);
        for (char c : guess) {
            Label chip = new Label();
            chip.setPrefSize(36, 36);
            chip.setMinSize(36, 36);
            chip.setAlignment(Pos.CENTER);
            chip.setStyle(
                "-fx-background-color: " + hexForChar(c) + ";" +
                "-fx-background-radius: 18;"
            );
            guessBox.getChildren().add(chip);
        }

        // Auswertungs-Grid (2×2)
        GridPane hints = new GridPane();
        hints.setHgap(4);
        hints.setVgap(4);
        hints.setAlignment(Pos.CENTER);

        int shown = 0;
        // ● zuerst, dann ○
        for (int i = 0; i < exact; i++) {
            addHintDot(hints, "●", "#ffffff", shown);
            shown++;
        }
        for (int i = 0; i < color; i++) {
            addHintDot(hints, "○", "#888899", shown);
            shown++;
        }
        // Leere Plätze auffüllen
        while (shown < 4) {
            addHintDot(hints, "·", "#2a2a3a", shown);
            shown++;
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(guessBox, spacer, hints);
        boardBox.getChildren().add(row);
    }

    private void addHintDot(GridPane grid, String symbol, String color, int index) {
        Label dot = new Label(symbol);
        dot.setPrefSize(16, 16);
        dot.setAlignment(Pos.CENTER);
        dot.setStyle(
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 13px;"
        );
        grid.add(dot, index % 2, index / 2);
    }

    // ──────────────────────────────────────────────
    //  Hilfs-Methoden
    // ──────────────────────────────────────────────

    private void updateAttemptsLabel() {
        int rem = model.getRemainingAttempts();
        attemptsLabel.setText(rem + " Versuch" + (rem == 1 ? "" : "e") + " verbleibend");
    }

    private void showResult(String message, boolean won) {
        resultLabel.setText(message);
        resultLabel.setStyle(
            "-fx-text-fill: " + (won ? "#a6e3a1" : "#f38ba8") + ";" +
            "-fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-font-family: 'Courier New'; -fx-text-alignment: center;"
        );
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        submitButton.setDisable(true);
        clearButton.setDisable(true);
    }

    private String hexForChar(char c) {
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (COLOR_CHARS[i] == c) return COLOR_HEX[i];
        }
        return "#ffffff";
    }

    // ──────────────────────────────────────────────
    //  Style-Strings
    // ──────────────────────────────────────────────

    private String emptySlotStyle() {
        return "-fx-background-color: #1a1a26;" +
               "-fx-background-radius: 26;" +
               "-fx-border-color: #2e2e42;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 26;" +
               "-fx-cursor: default;";
    }

    private String filledSlotStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 26;" +
               "-fx-border-color: transparent;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 26;" +
               "-fx-cursor: hand;";
    }

    private String colorBtnStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 24;" +
               "-fx-cursor: hand;" +
               "-fx-font-weight: bold;" +
               "-fx-font-family: 'Courier New';" +
               "-fx-font-size: 13px;" +
               "-fx-text-fill: #0f0f13;";
    }
}
