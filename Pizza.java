import java.io.*;
import java.util.*;

abstract class MenuItem {
    protected String name;
    public String getName() { return name; }
    public abstract double getPrice();
}

/**
 * single pizza: name + size + price.
 * Each pizza orderis is an object, so Order has a list of object.
 */
public class Pizza extends MenuItem {
    private String size;
    private double price;

    public Pizza(String name, String size, double price) {
        this.name  = name;
        this.size  = size;
        this.price = price;
    }

    @Override public String getName()  { return name; }
    @Override public double getPrice() { return price; }

    public String getSize()        { return size; }
    public String getDisplayName() { return name + "-" + size; }

    public void setName(String name)   { this.name = name; }
    public void setSize(String size)   { this.size = size; }
    public void setPrice(double price) { this.price = price; }

    /** One CSV line for pizzas.txt: name,size,price */
    public String toLine() {
        return name + "," + size + "," + String.format("%.2f", price);
    }

    public static Pizza fromLine(String line) {
        String[] p = line.split(",");
        return new Pizza(p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()));
    }

    @Override
    public String toString() {
        return String.format("%-24s RM %7.2f", getDisplayName(), price);
    }
}

class Menu {
    private final String file;
    private final List<Pizza> pizzas = new ArrayList<>();

    public Menu(String file) { this.file = file; }

    public List<Pizza> getPizzas() { return pizzas; }

    public void load() throws IOException {
        pizzas.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                pizzas.add(Pizza.fromLine(line));
            }
        }
    }

    public void save() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Pizza p : pizzas) pw.println(p.toLine());
        }
    }

    public void add(Pizza p)              { pizzas.add(p); }
    public void set(int index, Pizza p)   { pizzas.set(index, p); }
    public Pizza remove(int index)        { return pizzas.remove(index); }

    public Pizza find(String name, String size) {
        for (Pizza p : pizzas)
            if (p.getName().equalsIgnoreCase(name) && p.getSize().equalsIgnoreCase(size))
                return p;
        return null;
    }

    /** Default menu used the first time the program runs. */
    public void seedDefaults() {
        String[][] data = {
            {"Margherita",       "9.50",  "13.50", "18.00"},
            {"Pepperoni",        "10.75", "15.50", "20.80"},
            {"BBQ Chicken",      "12.00", "17.25", "22.50"},
            {"Vegetarian",       "11.00", "15.75", "21.00"},
            {"Supreme",          "13.00", "18.50", "24.00"},
            {"Hawaiian",         "11.50", "16.00", "21.50"},
            {"Meat Lovers",      "13.50", "19.00", "24.50"},
            {"Four Cheese",      "12.00", "16.50", "22.00"},
            {"Buffalo Chicken",  "12.50", "17.75", "23.00"},
            {"Mushroom Truffle", "14.00", "19.50", "25.00"},
            {"Philly Steak",     "13.75", "19.25", "24.75"},
            {"Mediterranean",    "12.25", "17.00", "22.50"}
        };
        String[] sizes = {"S", "M", "L"};
        for (String[] row : data)
            for (int i = 0; i < sizes.length; i++)
                pizzas.add(new Pizza(row[0], sizes[i], Double.parseDouble(row[i + 1])));
    }
}