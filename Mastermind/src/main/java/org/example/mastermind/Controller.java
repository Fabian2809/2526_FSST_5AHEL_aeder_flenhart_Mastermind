package org.example.mastermind;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Set;

public class Controller {

    // ─────────────────────────────────────────────────────
    //  FXML-Felder
    // ─────────────────────────────────────────────────────
    @FXML private Label  attemptsLabel;
    @FXML private VBox   boardBox;
    @FXML private HBox   slotsBox;
    @FXML private HBox   colorPalette;
    @FXML private Label  errorLabel;
    @FXML private Button submitButton;
    @FXML private Button hintButton;
    @FXML private Button clearButton;
    @FXML private VBox   resultBox;
    @FXML private Label  resultLabel;
    @FXML private Button restartButton;

    // ─────────────────────────────────────────────────────
    //  Farb-Definitionen
    // ─────────────────────────────────────────────────────
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
        "ROT (R)","GRÜN (G)","BLAU (B)","GELB (Y)","ORANGE (O)","PINK (P)"
    };

    // ─────────────────────────────────────────────────────
    //  Spielzustand
    // ─────────────────────────────────────────────────────
    private Model model;
    private char[]  currentGuess = new char[4];
    private int     filledSlots  = 0;
    private Label[] slotLabels   = new Label[4];

    /** Referenzen auf Palette-Labels, damit wir sie ausgrauen können */
    private Label[] paletteLabels = new Label[6];

    /** Vorschau-Zeilen im Board (10 Stück, vorbefüllt) */
    private HBox[] boardRows = new HBox[10];

    // ─────────────────────────────────────────────────────
    //  Initialisierung
    // ─────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        model = new Model();
        buildBoard();
        buildSlots();
        buildColorPalette();
        updateAttemptsLabel();
        setupKeyboardInput();
    }

    /**
     * Baut das feste 10-Zeilen-Board auf (alle Zeilen leer, wachsen mit Fenster).
     * VBox.vgrow=ALWAYS damit sie den Center-Bereich füllen.
     */
    private void buildBoard() {
        boardBox.getChildren().clear();
        boardBox.setFillWidth(true);
        VBox.setVgrow(boardBox, Priority.ALWAYS);

        for (int row = 0; row < 10; row++) {
            HBox rowBox = buildEmptyRow(row);
            boardRows[row] = rowBox;
            VBox.setVgrow(rowBox, Priority.ALWAYS);
            boardBox.getChildren().add(rowBox);
        }
    }

    /** Erstellt eine leere Board-Zeile mit Zeilennummer, 4 leeren Chips und leerem Hint-Grid. */
    private HBox buildEmptyRow(int rowIndex) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);

        // Zeilennummer (1 = unterste, sichtbar wenn noch offen)
        Label num = new Label(String.valueOf(10 - rowIndex));
        num.setPrefWidth(20);
        num.setAlignment(Pos.CENTER_RIGHT);
        num.setStyle("-fx-text-fill: #252535; -fx-font-size: 11px; -fx-font-family: 'Courier New';");

        // 4 leere Chips
        HBox chips = new HBox(7);
        chips.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            Label chip = new Label();
            chip.setPrefSize(30, 30);
            chip.setMinSize(30, 30);
            chip.setStyle(emptyChipStyle());
            chips.getChildren().add(chip);
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hint-Dots (2×2)
        GridPane hints = buildEmptyHintGrid();

        row.getChildren().addAll(num, chips, spacer, hints);
        return row;
    }

    /** Leeres 2×2 Hint-Grid mit dim Punkten. */
    private GridPane buildEmptyHintGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);
        grid.setPrefWidth(38);
        for (int i = 0; i < 4; i++) {
            Label dot = new Label("·");
            dot.setPrefSize(14, 14);
            dot.setAlignment(Pos.CENTER);
            dot.setStyle("-fx-text-fill: #1e1e2e; -fx-font-size: 12px;");
            grid.add(dot, i % 2, i / 2);
        }
        return grid;
    }

    /** Befüllt Zeile rowIndex mit einem gespielten Versuch. */
    private void fillBoardRow(int rowIndex, char[] guess, int exact, int color) {
        HBox row = boardRows[rowIndex];

        // Chips befüllen (Index 1 = HBox mit Chips)
        HBox chips = (HBox) row.getChildren().get(1);
        for (int i = 0; i < 4; i++) {
            Label chip = (Label) chips.getChildren().get(i);
            chip.setStyle(
                "-fx-background-color: " + hexForChar(guess[i]) + ";" +
                "-fx-background-radius: 15;"
            );
        }

        // Hint-Grid ersetzen (Index 3)
        GridPane hints = new GridPane();
        hints.setHgap(4);
        hints.setVgap(4);
        hints.setAlignment(Pos.CENTER);
        hints.setPrefWidth(38);

        int shown = 0;
        for (int i = 0; i < exact; i++) { addDot(hints, "●", "#e8e8f0", shown++); }
        for (int i = 0; i < color; i++) { addDot(hints, "○", "#555566", shown++); }
        while (shown < 4)               { addDot(hints, "·", "#1e1e2e", shown++); }

        row.getChildren().set(3, hints);

        // Zeilennummer heller machen
        Label num = (Label) row.getChildren().get(0);
        num.setStyle("-fx-text-fill: #35354a; -fx-font-size: 11px; -fx-font-family: 'Courier New';");

        // Aktive Zeile hervorheben
        row.setStyle(
            "-fx-background-color: #14141e;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 4 8 4 8;"
        );
    }

    private void addDot(GridPane grid, String sym, String col, int idx) {
        Label d = new Label(sym);
        d.setPrefSize(14, 14);
        d.setAlignment(Pos.CENTER);
        d.setStyle("-fx-text-fill: " + col + "; -fx-font-size: 12px;");
        grid.add(d, idx % 2, idx / 2);
    }

    // ─────────────────────────────────────────────────────
    //  Slots (aktueller Versuch)
    // ─────────────────────────────────────────────────────

    private void buildSlots() {
        slotsBox.getChildren().clear();
        filledSlots  = 0;
        currentGuess = new char[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Label slot = new Label();
            slot.setPrefSize(46, 46);
            slot.setMinSize(46, 46);
            slot.setAlignment(Pos.CENTER);
            slot.setStyle(emptySlotStyle());
            slot.setOnMouseClicked(e -> removeFromSlot(idx));
            slotLabels[i] = slot;
            slotsBox.getChildren().add(slot);
        }
    }

    private void addColorByChar(char c) {
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (COLOR_CHARS[i] == c) {
                addColor(c, COLOR_HEX[i]);
                return;
            }
        }
    }

    private void addColor(char c, String hex) {
        if (filledSlots >= 4) return;
        errorLabel.setText("");
        currentGuess[filledSlots] = c;
        Label slot = slotLabels[filledSlots];
        slot.setStyle(filledSlotStyle(hex));
        filledSlots++;
    }

    private void removeFromSlot(int index) {
        if (index >= filledSlots) return;
        for (int i = index; i < filledSlots - 1; i++) {
            currentGuess[i] = currentGuess[i + 1];
            slotLabels[i].setStyle(slotLabels[i + 1].getStyle());
        }
        filledSlots--;
        currentGuess[filledSlots] = 0;
        slotLabels[filledSlots].setStyle(emptySlotStyle());
    }

    private void removeLastSlot() {
        if (filledSlots == 0) return;
        filledSlots--;
        currentGuess[filledSlots] = 0;
        slotLabels[filledSlots].setStyle(emptySlotStyle());
    }

    // ─────────────────────────────────────────────────────
    //  Farb-Palette
    // ─────────────────────────────────────────────────────

    private void buildColorPalette() {
        colorPalette.getChildren().clear();
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            final char   c    = COLOR_CHARS[i];
            final String hex  = COLOR_HEX[i];
            final String name = COLOR_NAMES[i];

            Label btn = new Label(String.valueOf(c));
            btn.setPrefSize(42, 42);
            btn.setMinSize(42, 42);
            btn.setAlignment(Pos.CENTER);
            btn.setStyle(colorBtnStyle(hex));
            btn.setTooltip(new Tooltip(name));

            DropShadow glow = new DropShadow(12, Color.web(hex + "99"));
            btn.setOnMouseEntered(e -> { if (btn.getOpacity() > 0.4) btn.setEffect(glow); });
            btn.setOnMouseExited(e  -> btn.setEffect(null));
            btn.setOnMouseClicked(e -> { if (btn.getOpacity() > 0.4) addColor(c, hex); });

            paletteLabels[i] = btn;
            colorPalette.getChildren().add(btn);
        }
    }

    // ─────────────────────────────────────────────────────
    //  Tastatur-Eingabe
    // ─────────────────────────────────────────────────────

    /**
     * Setzt einen KeyEvent-Filter auf die gesamte Szene.
     * Wird aufgerufen sobald die Scene gesetzt ist (über sceneProperty-Listener).
     */
    private void setupKeyboardInput() {
        // boardBox ist immer in der Szene – wir hören auf die Scene, sobald sie verfügbar ist
        boardBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
            }
        });
    }

    private void handleKey(KeyEvent e) {
        if (model.isOver()) return;

        KeyCode code = e.getCode();

        if (code == KeyCode.ENTER) {
            onGuessSubmit();
        } else if (code == KeyCode.BACK_SPACE) {
            removeLastSlot();
            errorLabel.setText("");
        } else {
            String text = e.getText().toUpperCase();
            if (text.length() == 1) {
                char c = text.charAt(0);
                for (char valid : COLOR_CHARS) {
                    if (c == valid) {
                        addColorByChar(c);
                        break;
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────
    //  Event-Handler (FXML)
    // ─────────────────────────────────────────────────────

    @FXML
    private void onGuessSubmit() {
        if (filledSlots < 4) {
            errorLabel.setText("⚠  Alle 4 Felder füllen!");
            return;
        }
        errorLabel.setText("");

        // Welche Board-Zeile ist die nächste? (von unten: 9,8,7,...)
        int usedBefore = Model.MAX_ATTEMPTS - model.getRemainingAttempts();
        int rowIndex   = usedBefore; // Zeile von oben (0=oberste)

        int[] result = model.checkGuess(currentGuess.clone());
        fillBoardRow(rowIndex, currentGuess.clone(), result[0], result[1]);

        updateAttemptsLabel();
        buildSlots();
        resetHint();

        if (model.isWon()) {
            showResult("🎉  Gewonnen! Code geknackt!", true);
        } else if (model.isOver()) {
            String secret = new String(model.getSecretCode());
            showResult("😞  Verloren! Code war: " + secret, false);
        }
    }

    @FXML
    private void onClearSlots() {
        buildSlots();
        errorLabel.setText("");
    }

    /** Graut alle Farben aus, die NICHT im geheimen Code enthalten sind. */
    @FXML
    private void onHint() {
        Set<Character> inCode = model.getColorsInCode();
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (!inCode.contains(COLOR_CHARS[i])) {
                paletteLabels[i].setOpacity(0.18);
                paletteLabels[i].setStyle(
                    colorBtnStyle(COLOR_HEX[i]) +
                    "-fx-background-color: #1e1e2a; -fx-text-fill: #333340;"
                );
            }
        }
        hintButton.setDisable(true);
        hintButton.setStyle(
            "-fx-background-color: #1a1a2a; -fx-text-fill: #333340;" +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
            "-fx-background-radius: 6;"
        );
    }

    @FXML
    private void onRestart() {
        model = new Model();
        buildBoard();
        buildSlots();
        resetHint();
        errorLabel.setText("");
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        submitButton.setDisable(false);
        clearButton.setDisable(false);
        hintButton.setDisable(false);
        hintButton.setStyle(
            "-fx-background-color: #2a2a3a; -fx-text-fill: #f9e2af;" +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
            "-fx-background-radius: 6; -fx-cursor: hand;" +
            "-fx-border-color: #3a3a4a; -fx-border-radius: 6;"
        );
        updateAttemptsLabel();
    }

    // ─────────────────────────────────────────────────────
    //  Hilfsmethoden
    // ─────────────────────────────────────────────────────

    private void updateAttemptsLabel() {
        int rem = model.getRemainingAttempts();
        attemptsLabel.setText(rem + " Versuch" + (rem == 1 ? "" : "e") + " verbleibend");
    }

    private void showResult(String message, boolean won) {
        resultLabel.setText(message);
        resultLabel.setStyle(
            "-fx-text-fill: " + (won ? "#a6e3a1" : "#f38ba8") + ";" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-font-family: 'Courier New'; -fx-text-alignment: center;"
        );
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        submitButton.setDisable(true);
        clearButton.setDisable(true);
        hintButton.setDisable(true);
    }

    /** Setzt die Palette nach onRestart zurück. */
    private void resetHint() {
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            paletteLabels[i].setOpacity(1.0);
            paletteLabels[i].setStyle(colorBtnStyle(COLOR_HEX[i]));
        }
    }

    private String hexForChar(char c) {
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (COLOR_CHARS[i] == c) return COLOR_HEX[i];
        }
        return "#ffffff";
    }

    // ─────────────────────────────────────────────────────
    //  Style-Strings
    // ─────────────────────────────────────────────────────

    private String emptyChipStyle() {
        return "-fx-background-color: #18181f;" +
               "-fx-background-radius: 15;" +
               "-fx-border-color: #1e1e2a;" +
               "-fx-border-width: 1;" +
               "-fx-border-radius: 15;";
    }

    private String emptySlotStyle() {
        return "-fx-background-color: #18181f;" +
               "-fx-background-radius: 23;" +
               "-fx-border-color: #2e2e42;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 23;" +
               "-fx-cursor: default;";
    }

    private String filledSlotStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 23;" +
               "-fx-border-color: transparent;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 23;" +
               "-fx-cursor: hand;";
    }

    private String colorBtnStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 21;" +
               "-fx-cursor: hand;" +
               "-fx-font-weight: bold;" +
               "-fx-font-family: 'Courier New';" +
               "-fx-font-size: 12px;" +
               "-fx-text-fill: #0d0d12;";
    }
}
