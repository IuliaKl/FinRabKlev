package ipap.finrabklev;

import javafx.beans.property.SimpleIntegerProperty;

import java.util.Objects;
// модель
public class Point {

    private SimpleIntegerProperty x;
    private SimpleIntegerProperty y;
    private SimpleIntegerProperty step;
    private SimpleIntegerProperty commandIndex;
    private Command command;

    public Point(int x, int y, int step, int commandIndex, Command command ) {
        this.x = new SimpleIntegerProperty(x);
        this.y = new SimpleIntegerProperty(y);
        this.step =new SimpleIntegerProperty(step);
        this.commandIndex =new SimpleIntegerProperty(commandIndex);
        this.command =command;
    }
// старый кнструктор для проверки
    public Point(int x, int y, int step) {
        this(x, y, step, -1, null);

    }

    public int getX() {return x.get();}
    public SimpleIntegerProperty xProperty() {return x;}


    public int getY() {return y.get();}
    public SimpleIntegerProperty yProperty() {return y;}


    public int getStep() {return step.get();}
    public SimpleIntegerProperty stepProperty() {return step;}

    public int getCommandIndex() { return commandIndex.get();}
    public SimpleIntegerProperty commandIndexProperty() {return commandIndex;}

    public Command getCommand() {return command;}

    @Override
    public int hashCode() { return Objects.hash(x.get(), y.get());}

        @Override
    public boolean equals(Object o) {
        if (this == o)return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return this.x.get() == point.x.get() && this.y.get() == point.y.get();
    }

    @Override
    public String toString() { // ????
        return String.format("Point{x=%d, y=%d, шаг=%d}", x.get(), y.get(), step.get(), command);
    }
}


