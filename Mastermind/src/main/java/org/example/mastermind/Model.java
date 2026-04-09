package org.example.mastermind;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Model {

    public static final char[] COLORS       = {'R', 'G', 'B', 'Y', 'O', 'P'};
    public static final int    CODE_LENGTH   = 4;
    public static final int    MAX_ATTEMPTS  = 10;

    private final char[] secretCode;
    private int     attemptsUsed;
    private boolean won;

    public Model() {
        secretCode   = new char[CODE_LENGTH];
        attemptsUsed = 0;
        won          = false;
        generateCode();
    }

    private void generateCode() {
        Random rnd = new Random();
        for (int i = 0; i < CODE_LENGTH; i++) {
            secretCode[i] = COLORS[rnd.nextInt(COLORS.length)];
            System.out.println(secretCode[i]);
        }
    }

    /**
     * Wertet einen Versuch aus.
     * @return int[]{exactHits (●), colorHits (○)}
     */
    public int[] checkGuess(char[] guess) {
        attemptsUsed++;

        boolean[] secretUsed = new boolean[CODE_LENGTH];
        boolean[] guessUsed  = new boolean[CODE_LENGTH];
        int exact = 0, color = 0;

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i] == secretCode[i]) {
                exact++;
                secretUsed[i] = true;
                guessUsed[i]  = true;
            }
        }
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guessUsed[i]) continue;
            for (int j = 0; j < CODE_LENGTH; j++) {
                if (!secretUsed[j] && guess[i] == secretCode[j]) {
                    color++;
                    secretUsed[j] = true;
                    break;
                }
            }
        }

        if (exact == CODE_LENGTH) won = true;
        return new int[]{exact, color};
    }

    /**
     * Gibt die Menge der Farben zurück, die im geheimen Code enthalten sind.
     * Wird für den Hint-Button verwendet.
     */
    public Set<Character> getColorsInCode() {
        Set<Character> set = new HashSet<>();
        for (char c : secretCode) set.add(c);
        return set;
    }

    public boolean isWon()                { return won; }
    public boolean isOver()               { return won || attemptsUsed >= MAX_ATTEMPTS; }
    public int     getRemainingAttempts() { return MAX_ATTEMPTS - attemptsUsed; }
    public char[]  getSecretCode()        { return secretCode; }
}
