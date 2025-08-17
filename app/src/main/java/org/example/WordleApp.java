package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class WordleApp extends Application {
    private static final int WORD_LENGTH = 5;
    private static final int MAX_GUESSES = 6;

    private final List<String> WORDS = WordRepository.words();
    private final Set<String> WORD_SET = new HashSet<>(WORDS);
    private String targetWord;

    private final Cell[][] board = new Cell[MAX_GUESSES][WORD_LENGTH];
    private int currentRow = 0;

    private Label statusLabel;
    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage stage) {
        targetWord = pickHourlyWord();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setPadding(new Insets(24));

        Label title = new Label("WORDLE");
        title.getStyleClass().add("title");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = buildBoard();

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER);
        TextField input = new TextField();
        input.setPromptText("Type a 5-letter word and press Enter");
        input.getStyleClass().add("input");
        input.setPrefColumnCount(12);
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                submitGuess(input.getText());
                input.clear();
            }
        });
        inputRow.getChildren().add(input);

        statusLabel = new Label("New word every hour (UTC). Good luck!");
        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        VBox bottom = new VBox(8, inputRow, statusLabel);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));
        root.setCenter(grid);
        BorderPane.setMargin(grid, new Insets(0, 0, 10, 0));
        root.setBottom(bottom);

        Scene scene = new Scene(root, 520, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/ibm.css")).toExternalForm());
        stage.setTitle("Wordle — IBM Retro");
        stage.setScene(scene);
        stage.show();

        // schedule hourly reset
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wordle-hourly-reset");
            t.setDaemon(true);
            return t;
        });
        scheduleNextReset();
    }

    private GridPane buildBoard() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < MAX_GUESSES; r++) {
            for (int c = 0; c < WORD_LENGTH; c++) {
                Cell cell = new Cell();
                board[r][c] = cell;
                grid.add(cell, c, r);
            }
        }
        return grid;
    }

    private void submitGuess(String raw) {
        if (currentRow >= MAX_GUESSES) return;
        if (raw == null) return;
        String guess = raw.trim().toLowerCase(Locale.ROOT);
        if (guess.length() != WORD_LENGTH || !guess.chars().allMatch(Character::isLetter)) {
            showInfo("Please enter a valid 5-letter word.");
            return;
        }
    if (!WORD_SET.contains(guess)) {
            showInfo("Word not in list.");
            return;
        }

        for (int i = 0; i < WORD_LENGTH; i++) {
            board[currentRow][i].setChar(guess.charAt(i));
        }

        paintRow(currentRow, guess, targetWord);

        if (guess.equals(targetWord)) {
            statusLabel.setText("You got it! The word was '" + targetWord.toUpperCase(Locale.ROOT) + "'.");
            currentRow = MAX_GUESSES; // lock
            return;
        }

        currentRow++;
        if (currentRow == MAX_GUESSES) {
            statusLabel.setText("Out of tries. The word was '" + targetWord.toUpperCase(Locale.ROOT) + "'.");
        }
    }

    private void paintRow(int row, String guess, String answer) {
        int[] counts = new int[26];
        for (char ch : answer.toCharArray()) counts[ch - 'a']++;

        ColorState[] states = new ColorState[WORD_LENGTH];
        for (int i = 0; i < WORD_LENGTH; i++) {
            char g = guess.charAt(i);
            if (g == answer.charAt(i)) {
                states[i] = ColorState.GREEN;
                counts[g - 'a']--;
            }
        }
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (states[i] != null) continue;
            char g = guess.charAt(i);
            if (counts[g - 'a'] > 0) {
                states[i] = ColorState.YELLOW;
                counts[g - 'a']--;
            } else {
                states[i] = ColorState.GRAY;
            }
        }
        for (int i = 0; i < WORD_LENGTH; i++) {
            board[row][i].setState(states[i]);
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.setTitle("Info");
        alert.showAndWait();
    }

    private String pickHourlyWord() {
        ZonedDateTime nowUtc = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
        ZonedDateTime hourBucket = nowUtc.truncatedTo(ChronoUnit.HOURS);
        long seed = hourBucket.toEpochSecond();
        Random r = new Random(seed);
        return WORDS.get(r.nextInt(WORDS.size()));
    }

    private void scheduleNextReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        long delayMillis = ChronoUnit.MILLIS.between(now, nextHour);
        scheduler.schedule(() -> {
            javafx.application.Platform.runLater(() -> {
                targetWord = pickHourlyWord();
                resetBoard();
                statusLabel.setText("New hour, new word. Good luck!");
            });
            scheduleNextReset();
        }, Math.max(1, delayMillis), TimeUnit.MILLISECONDS);
    }

    private void resetBoard() {
        for (int r = 0; r < MAX_GUESSES; r++) {
            for (int c = 0; c < WORD_LENGTH; c++) {
                board[r][c].reset();
            }
        }
        currentRow = 0;
    }

    @Override
    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    enum ColorState { GREEN, YELLOW, GRAY }

    static class Cell extends StackPane {
        private final Rectangle box = new Rectangle(60, 60);
        private final Label label = new Label("");

        Cell() {
            setAlignment(Pos.CENTER);
            getStyleClass().add("tile-container");
            box.getStyleClass().add("tile");
            label.getStyleClass().add("tile-text");
            label.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
            getChildren().addAll(box, label);
        }

        void setChar(char c) { label.setText(String.valueOf(Character.toUpperCase(c))); }
        void setState(ColorState state) {
            getStyleClass().removeAll("green", "yellow", "gray");
            switch (state) {
                case GREEN -> getStyleClass().add("green");
                case YELLOW -> getStyleClass().add("yellow");
                case GRAY -> getStyleClass().add("gray");
            }
        }
        void reset() {
            label.setText("");
            getStyleClass().removeAll("green", "yellow", "gray");
        }
    }
}
