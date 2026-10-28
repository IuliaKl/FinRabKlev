package ipap.finrabklev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;



public class TrajectoryService {

    private String lastMessage ="";

    public String getLastMessage() {
        return lastMessage;
    }


    public List<Command> loadCommandsFromResourse(String resourcePath) throws IOException{
        List<String>lines = readLines(resourcePath);
        return parseCommands(lines);
    }


    private List<String> readLines(String resourcePath) throws IOException{
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream==null) {
            throw new IOException("Список команд не найден: " + resourcePath);
        }
        List<String>lines = new ArrayList<>();
        try
                (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
            while((line =reader.readLine())!=null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {lines.add(trimmed);}
                }
            }
            return lines;
        }


    public List<Point> calculateFromResource(String resourcePath) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        // List<String> rawLines = Files.readAllLines(Path.of("commands.txt"), StandardCharsets.UTF_8);
        // List<Command> cmds = parseCommands(rawLines);
        if (stream == null) {
            throw new IOException("Ресурс не найден: " + resourcePath + ". Проверь, лежит ли файл в src/main/resources");
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }

        List<Command> cmds = parseCommands(lines);
        return calculateTrajectory(cmds, 50);

    }


    private List<Command> parseCommands(List<String>lines) {
        List<Command> result = new ArrayList<>();

        for (int i=0; i<lines.size();i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) continue;

            try {
                result.add(Command.valueOf(trimmed));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверная команда: '" + trimmed + "'", e);
            }

        }return result;
    }

    //final int MAX_STEP = 50;

    public List<Point> calculateTrajectory(List<Command> cmds, int MAX_STEP) {
       lastMessage = ""; // сбрасывается перед новым расчетом

        int x = 0, y = 0;
        int dx = 1, dy = 0;
        int step = 0;

        Set<Point> visited = new LinkedHashSet<>();
        List<Point> trajectory = new ArrayList<>();

        Point start = new Point(x, y, step,-1, null);// стартовая точка (шаг 0)
        visited.add(start);
        trajectory.add(start);// в таблицу

        if (cmds == null || cmds.isEmpty()) {
            lastMessage = "Список команд пуст - двигаться некуда.";
            return trajectory;
        }

//        boolean hasStep = cmds.stream().anyMatch(c -> c == Command.S);
//        if (!hasStep) {
//            return trajectory;// двигаться некуда
//        }

        int i = 0;
        int processed = 0;// защита от бесконечного цикла
        int safetyLimit = MAX_STEP * cmds.size() + cmds.size();
        while (step < MAX_STEP && processed < safetyLimit) {

            int cmdIndex = i % cmds.size();//счётчик повторяется после достижения последнего элемента.
            Command cmd = cmds.get(cmdIndex);

            if (cmd == Command.R) { //поворот направо: (dx, dy)->(dy,-dx)
                int temp = dx;
                dx = dy;
                dy = -temp;
            } else if (cmd == Command.L) { // попорот налево: (dx, dy) ->(-dy, dx)
                int temp = dx;
                dx = -dy;
                dy = temp;
            } else if (cmd == Command.S) {
                int nextX = x + dx;
                int nextY = y + dy;
                // Проверяем, есть ли уже такая точка в траектории.
                //Point nextPoint = new Point(nextX, nextY, 0);
                if (visited.contains(new Point(nextX, nextY,0))) {
                    lastMessage = ("Точка (" + nextX + ", " + nextY + ") уже есть в траектории");
                    break;
                }
                // Выполняем шаг
                x = nextX;
                y = nextY;
                step++;

                Point p = new Point(x, y, step,cmdIndex, cmd);
                visited.add(p);
                trajectory.add(p);
                }
            processed++;
            i++;
        }
        if (lastMessage.isEmpty()) {
            lastMessage = "Достигнут лимит шагов: " + MAX_STEP;
        } else lastMessage = "Траектория построена, шагов: " + step;


    return trajectory;
}
}

