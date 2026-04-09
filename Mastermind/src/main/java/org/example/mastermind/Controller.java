package org.example.mastermind;
//package com.tutorialspoint;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URI;
import java.util.*;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;



class Helper extends TimerTask {
    public Label UhrzeitLabel;
    int i = 0;
    Timer timer;

    public Helper(Label Uhrzeitlabel, Timer timer) {
        this.UhrzeitLabel = Uhrzeitlabel;
        this.timer = timer;
    }

    public void run() {
        Platform.runLater(() -> {
            UhrzeitLabel.setText("Tick: " + i);
        });
        i++;
    }
}

public class Controller {

    public Label UhrzeitLabel;
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
    private Model   model;
    private char[]  currentGuess  = new char[4];
    private int     filledSlots   = 0;
    private Label[] slotLabels    = new Label[4];
    private Label[] paletteLabels = new Label[6];
    private HBox[]  boardRows     = new HBox[10];
    Timer timer;

    /**
     * Merkt sich welche Palette-Einträge durch den Hint deaktiviert wurden.
     * Wird beim Neustart zurückgesetzt, aber NICHT nach jedem Versuch.
     */
    private boolean[] hintDisabled = new boolean[6];
    private boolean   hintUsed     = false;

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
        timer = new Timer();
        TimerTask task = new Helper(UhrzeitLabel, timer);

        timer.schedule(task, 2000, 5000);
        /*
        TimerTask tasknew = new TimerSchedulePeriod();
        timer = new Timer();
        timer.schedule(tasknew,100, 100);

         */
    }
    /*
    public void run() {
        UhrzeitLabel.setText(timer.toString());
    }
*/
    // ─────────────────────────────────────────────────────
    //  Board aufbauen (10 feste Zeilen)
    // ─────────────────────────────────────────────────────

    private void buildBoard() {
        boardBox.getChildren().clear();
        for (int row = 0; row < 10; row++) {
            HBox rowBox = buildEmptyRow(row);
            boardRows[row] = rowBox;
            boardBox.getChildren().add(rowBox);
        }
    }

    private HBox buildEmptyRow(int rowIndex) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        row.setPrefHeight(34);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(3, 6, 3, 6));
        row.setStyle("-fx-background-color: transparent;");

        // Zeilennummer
        Label num = new Label(String.valueOf(10 - rowIndex));
        num.setPrefWidth(18);
        num.setAlignment(Pos.CENTER_RIGHT);
        num.setStyle("-fx-text-fill: #1e1e2a; -fx-font-size: 10px; -fx-font-family: 'Courier New';");

        // 4 leere Farbchips
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            Label chip = new Label();
            chip.setPrefSize(28, 28);
            chip.setMinSize(28, 28);
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

    private GridPane buildEmptyHintGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);
        grid.setPrefWidth(34);
        for (int i = 0; i < 4; i++) {
            Label dot = new Label("·");
            dot.setPrefSize(13, 13);
            dot.setAlignment(Pos.CENTER);
            dot.setStyle("-fx-text-fill: #1a1a26; -fx-font-size: 11px;");
            grid.add(dot, i % 2, i / 2);
        }
        return grid;
    }

    private void fillBoardRow(int rowIndex, char[] guess, int exact, int color) {
        HBox row   = boardRows[rowIndex];
        HBox chips = (HBox) row.getChildren().get(1);

        for (int i = 0; i < 4; i++) {
            Label chip = (Label) chips.getChildren().get(i);
            chip.setStyle(
                "-fx-background-color: " + hexForChar(guess[i]) + ";" +
                "-fx-background-radius: 14;"
            );
        }

        GridPane hints = new GridPane();
        hints.setHgap(3);
        hints.setVgap(3);
        hints.setAlignment(Pos.CENTER);
        hints.setPrefWidth(34);

        int shown = 0;
        for (int i = 0; i < exact; i++) addDot(hints, "●", "#dedef0", shown++);
        for (int i = 0; i < color; i++) addDot(hints, "○", "#444456", shown++);
        while (shown < 4)               addDot(hints, "·", "#1a1a26", shown++);

        row.getChildren().set(3, hints);

        // Hintergrund der gespielten Zeile leicht aufhellen
        row.setStyle(
            "-fx-background-color: #12121c;" +
            "-fx-background-radius: 7;" +
            "-fx-padding: 3 6 3 6;"
        );

        Label num = (Label) row.getChildren().get(0);
        num.setStyle("-fx-text-fill: #2e2e40; -fx-font-size: 10px; -fx-font-family: 'Courier New';");
    }

    private void addDot(GridPane grid, String sym, String col, int idx) {
        Label d = new Label(sym);
        d.setPrefSize(13, 13);
        d.setAlignment(Pos.CENTER);
        d.setStyle("-fx-text-fill: " + col + "; -fx-font-size: 11px;");
        grid.add(d, idx % 2, idx / 2);
    }

    // ─────────────────────────────────────────────────────
    //  Eingabe-Slots
    // ─────────────────────────────────────────────────────

    private void buildSlots() {
        slotsBox.getChildren().clear();
        filledSlots  = 0;
        currentGuess = new char[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Label slot = new Label();
            slot.setPrefSize(44, 44);
            slot.setMinSize(44, 44);
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
                // Tastatureingabe: ausgegaute Farben (Hint) trotzdem blockieren
                if (hintDisabled[i]) return;
                addColor(c, COLOR_HEX[i]);
                return;
            }
        }
    }

    private void addColor(char c, String hex) {
        if (filledSlots >= 4) return;
        errorLabel.setText("");
        currentGuess[filledSlots] = c;
        slotLabels[filledSlots].setStyle(filledSlotStyle(hex));
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
            final int    idx  = i;

            Label btn = new Label(String.valueOf(c));
            btn.setPrefSize(40, 40);
            btn.setMinSize(40, 40);
            btn.setAlignment(Pos.CENTER);
            btn.setStyle(colorBtnStyle(hex));
            btn.setTooltip(new Tooltip(name));

            DropShadow glow = new DropShadow(10, Color.web(hex + "88"));
            btn.setOnMouseEntered(e -> { if (!hintDisabled[idx]) btn.setEffect(glow); });
            btn.setOnMouseExited(e  -> btn.setEffect(null));
            btn.setOnMouseClicked(e -> { if (!hintDisabled[idx]) addColor(c, hex); });

            paletteLabels[i] = btn;
            colorPalette.getChildren().add(btn);
        }
    }

    // ─────────────────────────────────────────────────────
    //  Tastatur-Eingabe
    // ─────────────────────────────────────────────────────

    private void setupKeyboardInput() {
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
                    if (c == valid) { addColorByChar(c); break; }
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

        int usedBefore = Model.MAX_ATTEMPTS - model.getRemainingAttempts();
        int[] result   = model.checkGuess(currentGuess.clone());
        fillBoardRow(usedBefore, currentGuess.clone(), result[0], result[1]);

        updateAttemptsLabel();
        buildSlots();

        // Hint-Status BLEIBT nach dem Versuch erhalten – ausgegaute Farben
        // bleiben ausgegraut für den Rest des Spiels
        if (hintUsed) reapplyHint();

        if (model.isWon()) {
            showResult("🎉  Gewonnen! Code geknackt! ", true);
        } else if (model.isOver()) {
            String secret = new String(model.getSecretCode());
            showResult("😞  Verloren! Code war: " + secret, false);
        }
    }

    @FXML
    private void onClearSlots() {
        buildSlots();
        errorLabel.setText("");
        // Hint-Zustand nach Löschen der Slots wieder anwenden
        if (hintUsed) reapplyHint();
    }

    /**
     * Graut alle Farben dauerhaft aus, die NICHT im Code enthalten sind.
     * Bleibt für den Rest des Spiels aktiv.
     */
    @FXML
    private void onHint() {
        if (hintUsed) return;
        hintUsed = true;

        Set<Character> inCode = model.getColorsInCode();
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (!inCode.contains(COLOR_CHARS[i])) {
                hintDisabled[i] = true;
            }
        }

        applyHintVisuals();

        hintButton.setDisable(true);
        hintButton.setStyle(
            "-fx-background-color: #13131e; -fx-text-fill: #252535;" +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
            "-fx-background-radius: 6; -fx-border-color: #1a1a26;" +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        );
    }

    /** Wendet die Hint-Ausgrauung visuell an (beim ersten Aufruf und nach buildSlots). */
    private void applyHintVisuals() {
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            if (hintDisabled[i]) {
                paletteLabels[i].setOpacity(0.15);
                paletteLabels[i].setStyle(
                    "-fx-background-color: #18181f;" +
                    "-fx-background-radius: 20;" +
                    "-fx-text-fill: #252530;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-family: 'Courier New';" +
                    "-fx-font-size: 12px;"
                );
            }
        }
    }

    /**
     * Nach buildSlots() (neuer Versuch) muss der Hint-Zustand erneut
     * auf die neu erstellten Palette-Labels angewendet werden,
     * da buildColorPalette() NICHT nochmal aufgerufen wird.
     */
    private void reapplyHint() {
        applyHintVisuals();
    }

    @FXML
    private void onRestart() {
        model        = new Model();
        hintUsed     = false;
        hintDisabled = new boolean[6];

        buildBoard();
        buildSlots();
        buildColorPalette();   // Palette komplett neu aufbauen → alle Farben aktiv
        errorLabel.setText("");
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        submitButton.setDisable(false);
        clearButton.setDisable(false);
        hintButton.setDisable(false);
        hintButton.setStyle(
            "-fx-background-color: #1e1e2e; -fx-text-fill: #f9e2af;" +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
            "-fx-background-radius: 6; -fx-cursor: hand;" +
            "-fx-border-color: #2e2e40; -fx-border-radius: 6; -fx-border-width: 1;"
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
        int attemps = 10- model.getRemainingAttempts();
        if(won){

            resultLabel.setText(message+ attemps+" Versuch(e) gebraucht!");
        }else{
            resultLabel.setText(message);
        }
        resultLabel.setStyle(
            "-fx-text-fill: " + (won ? "#a6e3a1" : "#f38ba8") + ";" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-font-family: 'Courier New'; -fx-text-alignment: center;"
        );
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        submitButton.setDisable(true);
        clearButton.setDisable(true);
        hintButton.setDisable(true);
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
        return "-fx-background-color: #16161e;" +
               "-fx-background-radius: 14;" +
               "-fx-border-color: #1c1c28;" +
               "-fx-border-width: 1;" +
               "-fx-border-radius: 14;";
    }

    private String emptySlotStyle() {
        return "-fx-background-color: #16161e;" +
               "-fx-background-radius: 22;" +
               "-fx-border-color: #2a2a3c;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 22;" +
               "-fx-cursor: default;";
    }

    private String filledSlotStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 22;" +
               "-fx-border-color: transparent;" +
               "-fx-border-width: 2;" +
               "-fx-border-radius: 22;" +
               "-fx-cursor: hand;";
    }

    private String colorBtnStyle(String hex) {
        return "-fx-background-color: " + hex + ";" +
               "-fx-background-radius: 20;" +
               "-fx-cursor: hand;" +
               "-fx-font-weight: bold;" +
               "-fx-font-family: 'Courier New';" +
               "-fx-font-size: 12px;" +
               "-fx-text-fill: #0d0d12;";
    }
}
