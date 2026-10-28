package ipap.finrabklev;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

public class TrajectoryController {

    @FXML
    private Button btnStart;

    @FXML
    private Canvas canvasField; // поле отражения движения точки

    @FXML
    private ListView<Command> listOfCommands; // перечень полученных комманд

    @FXML
    private TableView<Point> tablePoits; // таблица с  тремя колонками (координатами точек и номером шага)

    @FXML
    private TextArea logArea; // поле для вывода информации

    private final ObservableList<Command> commands=FXCollections.observableArrayList();

    private final List<Point> allPoints = new java.util.ArrayList<>();

    private final ObservableList<Point> displayedPoints = FXCollections.observableArrayList();
    private final ObservableList<Point> trajectory = FXCollections.observableArrayList();
    // сервис логикой
    private final TrajectoryService trajectoryService = new TrajectoryService();
    // анимация
    private int currentIndex = 0;
    private AnimationTimer animationTimer;
    private boolean isAnimating = false;

    @FXML
    public void initialize() {
        setupTable();
        setupCommandList();

        btnStart.setOnAction(e -> {
            if (isAnimating) return;
            {
                loadTrajectoryData();
            }if (!allPoints.isEmpty()){
                startSimulation();
            }
        }
        );

    }

    private void setupTable() {
        // создание таблицы
        // удаление старых столбцов
        tablePoits.getColumns().clear();
        // добавление новых столбцов
        TableColumn<Point, Integer> colStep = new TableColumn<>("шаг");
        colStep.setCellValueFactory(new PropertyValueFactory<>("step"));

        TableColumn<Point, Integer> colX = new TableColumn<>("x");
        colX.setCellValueFactory(new PropertyValueFactory<>("x"));

        TableColumn<Point, Integer> colY = new TableColumn<>("y");
        colY.setCellValueFactory(new PropertyValueFactory<>("y"));

        tablePoits.getColumns().addAll(colStep, colX, colY);

        // привязка списка к таблице
        tablePoits.setItems(displayedPoints);
    }

    private void setupCommandList() {
        listOfCommands.setItems(commands);

        listOfCommands.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Command cmd, boolean empty) {
                super.updateItem(cmd, empty);
                if (empty || cmd == null) {
                    setText(null);
                } else {
                    setText(switch (cmd) {
                        case S -> "S — шаг";
                        case R -> "R — поворот направо";
                        case L -> "L — поворот налево";
                    });
                }
            }
        });
    }

    // Добавление строк в лог
    private void appendLog(String message) {

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(()->appendLog(message));
            return;}
        if (logArea.getLength()>0) logArea.appendText("\n");
            logArea.appendText(message);
            logArea.setScrollTop(Double.MAX_VALUE);
            }


    private void loadTrajectoryData() {
        try {
            List<Command> cmds = trajectoryService.loadCommandsFromResourse("/ipap/finrabklev/commands.txt");
            commands.setAll(cmds);                       // ← список команд в ListView


            List<Point> points = trajectoryService.calculateTrajectory(cmds,50);
            allPoints.clear();
            allPoints.addAll(points);
            appendLog("Загружено команд: " + cmds.size());
           // appendLog("Траектория успешно рассчитана. Точек: " + points.size());
        String msg = trajectoryService.getLastMessage();
        if (msg!=null && !msg.isBlank()) appendLog(msg);

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Ошибка при загрузке траектории: " + e.getMessage();
            appendLog(errorMessage);
        }

    }

    // запуск симуляции
    private void startSimulation() {
        isAnimating = true;
        currentIndex = 0;
        displayedPoints.clear();
        listOfCommands.getSelectionModel().clearSelection();

        //tablePoits.getItems().clear(); // очищение таблицы перед стартом
        appendLog("Симуляция запущена...");

        animationTimer = new AnimationTimer() {
            private long lastFrameNanos = 0;
            private final long stepDelayNanos = 750_000_000L;

            @Override
            public void handle(long now) {
                if (currentIndex >= allPoints.size()) {
                    stopSimulation();
                    return;
                }
                if (now - lastFrameNanos < stepDelayNanos) {
                    return;
                }
                lastFrameNanos = now;
                applyStep(currentIndex);
                currentIndex++;
            }
        };
        animationTimer.start();
    }


    private void stopSimulation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        isAnimating = false;
        appendLog("Симуляция завершена.");
        appendLog (trajectoryService.getLastMessage());
        // Финальная отрисовка всей траектории
        drawTrajectoryStatic(trajectory);
    }

    private void drawFrame(int index) {
        if (canvasField == null || allPoints.isEmpty()) return;

        GraphicsContext gc = canvasField.getGraphicsContext2D();
        double w = canvasField.getWidth();
        double h = canvasField.getHeight();
        gc.clearRect(0, 0, w, h);

        // Сетка сначала — под траекторией
        double[] t = computeTransform(allPoints, w, h);
        drawGrid(gc, t[1], t[2], t[0], w, h);

        // Уже пройденная часть траектории
        drawFullTrajectoryLine(gc, w, h);

        // Текущая точка
        Point p = allPoints.get(index);
        drawPointAt(gc, p, w, h, Color.ORANGE, 8);

        gc.setFill(Color.BLACK);
        gc.fillText("Шаг: " + p.getStep() + "  команда: " + p.getCommand(), 10, 20);
    }



    // финальная отрисовка
    private void drawTrajectoryStatic(List<Point> points) {
        if (points.isEmpty() || canvasField == null) return;

        GraphicsContext gc = canvasField.getGraphicsContext2D();
        double w = canvasField.getWidth();
        double h = canvasField.getHeight();
        gc.clearRect(0, 0, w, h);

        double[] transform = computeTransform(points, w, h);
        double scale = transform[0];
        double offsetX = transform[1];
        double offsetY = transform[2];

            // Линия
        if (points.size() >= 2) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2);
            gc.beginPath();
            Point first = points.get(0);
            gc.moveTo(offsetX + first.getX() * scale, offsetY - first.getY() * scale);
            for (int i = 1; i < points.size(); i++) {
                Point p = points.get(i);
                gc.lineTo(offsetX + p.getX() * scale, offsetY - p.getY() * scale);
                }
                gc.stroke();
            }

            // Точки: Start/End/промежуточные
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                double px = offsetX + p.getX() * scale;
                double py = offsetY - p.getY() * scale;

                if (i == 0) {
                    gc.setFill(Color.GREEN);
                    gc.fillOval(px - 8, py - 8, 12, 12);

                } else if (i == points.size() - 1) {
                    gc.setFill(Color.RED);
                    gc.fillOval(px - 8, py - 8, 14, 14);

                } else {
                    gc.setFill(Color.PINK);
                    gc.fillOval(px - 6, py - 6, 4, 4);
                }
            }

            drawGrid(gc, offsetX, offsetY, scale, w, h);

    }

    private void drawFullTrajectoryLine(GraphicsContext gc, double w, double h) {
        List<Point> pts = allPoints;
        if (pts.size() < 2) return;

        double[] transform = computeTransform(pts, w, h);
        double scale = transform[0];
        double offsetX = transform[1];
        double offsetY = transform[2];

        gc.setStroke(Color.RED);
        gc.setLineWidth(1.5);
        gc.beginPath();

        Point first = pts.get(0);
        gc.moveTo(offsetX + first.getX() * scale, offsetY - first.getY() * scale);
        for (int i = 1; i < pts.size(); i++) {
            Point p = pts.get(i);
            gc.lineTo(offsetX + p.getX() * scale, offsetY - p.getY() * scale);
        }
        gc.stroke();
    }

    private void drawPointAt(GraphicsContext gc, Point p, double w, double h, Color color, double radius) {
        double[] transform = computeTransform(allPoints, w, h);
        double scale = transform[0];
        double offsetX = transform[1];
        double offsetY = transform[2];

        double px = offsetX + p.getX() * scale;
        double py = offsetY - p.getY() * scale;

        gc.setFill(color);
        gc.fillOval(px - radius, py - radius, radius * 2, radius * 2);
    }

    private double[] computeTransform(List<Point> points, double w, double h) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        int margin = 15;
        int dataWidth = maxX - minX + 2 * margin;
        int dataHeight = maxY - minY + 2 * margin;

        double scaleFit = Math.min(
                (w - 2 * margin) / Math.max(1, dataWidth),
                (h - 2 * margin) / Math.max(1, dataHeight)
        );
        double minComfortableScale = 40.0;
        double scale = Math.min(scaleFit, minComfortableScale);

        double centerX = w / 2;
        double centerY = h / 2;

        double dataCenterX = (minX + maxX) / 2.0;
        double dataCenterY = (minY + maxY) / 2.0;

        double targetX = (dataWidth < 20) ? 0 : dataCenterX;
        double targetY = (dataHeight < 20) ? 0 : dataCenterY;

        double offsetX = centerX - targetX * scale;
        double offsetY = centerY - targetY * scale;

        return new double[]{scale, offsetX, offsetY};
    }

    private void drawGrid(GraphicsContext gc, double offsetX, double offsetY, double scale, double w, double h) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        int step = 10;
        for (int y = -100; y <= 100; y += step) {
            double yPos = offsetY - y * scale;
            if (yPos >= 0 && yPos <= h) {
                gc.strokeLine(0, yPos, w, yPos);
            }
        }
        for (int x = -100; x <= 100; x += step) {
            double xPos = offsetX + x * scale;
            if (xPos >= 0 && xPos <= w) {
                gc.strokeLine(xPos, 0, xPos, h);
            }
        }
    }

    private void applyStep(int index){
        Point p = allPoints.get(index);

        displayedPoints.add(p); // таблица

        int cmdIdx = p.getCommandIndex(); // команда
        if (cmdIdx >= 0 && cmdIdx < commands.size()) {
            listOfCommands.getSelectionModel().select(cmdIdx);
            listOfCommands.scrollTo(cmdIdx);
        }
        drawFrame(index); // точка в Canvas

        appendLog("Шаг " + p.getStep() + ": (" + p.getX() + ", " + p.getY() + ") — команда " + p.getCommand());
    }

}
