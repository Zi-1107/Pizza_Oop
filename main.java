import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    static final String MENU_FILE   = "pizzas.txt";
    static final String ORDERS_FILE = "orders.txt";
    static final int    MAX_ORDERS  = 100;                    // keep last 100 orders
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static final Menu    menu = new Menu(MENU_FILE);
    static final Scanner sc   = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            if (!new File(MENU_FILE).exists()) {
                menu.seedDefaults();
                menu.save();
                System.out.println("Created default menu: " + MENU_FILE);
            }
            menu.load();
        } catch (IOException e) {
            System.out.println("Could not initialise menu: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println();
            System.out.println("===== PIZZA ORDERING SYSTEM =====");
            System.out.println("1. Order Pizza");
            System.out.println("2. View Order");
            System.out.println("3. Pizza Maintenance");
            System.out.println("4. Exit");

            int choice = readInt("Select option: ");
            switch (choice) {
                case 1: orderPizza();   break;
                case 2: viewOrder();    break;
                case 3: maintenance();  break;
                case 4: System.out.println("Thank you. Goodbye!"); return;
                default: System.out.println("Invalid option. Please choose 1-4.");
            }
        }
    }

    // 1. Order Pizza

    static void orderPizza() {
        if (menu.getPizzas().isEmpty()) {
            System.out.println("No pizzas available. Use Pizza Maintenance first.");
            return;
        }
        printMenu();

        Order order = new Order(nextOrderId(), LocalDate.now().format(DATE_FMT));

        while (true) {
            int idx = readInt("Enter pizza number (0 to finish): ");
            if (idx == 0) break;
            if (idx < 1 || idx > menu.getPizzas().size()) {
                System.out.println("Invalid pizza number.");
                continue;
            }
            Pizza p = menu.getPizzas().get(idx - 1);

            int qty = readInt("Quantity for " + p.getDisplayName() + ": ");
            if (qty <= 0) {
                System.out.println("Quantity must be a positive whole number.");
                continue;
            }
            order.addPizza(p, qty);
            System.out.println("Added " + qty + " x " + p.getDisplayName());
        }

        if (order.getLines().isEmpty()) {
            System.out.println("No pizzas ordered. Order cancelled.");
            return;
        }

        order.printSummary();
        try {
            appendOrder(order);
            System.out.println("Order " + order.getId() + " saved to " + ORDERS_FILE);
        } catch (IOException e) {
            System.out.println("Could not save order: " + e.getMessage());
        }
    }

    // 2. View Order 

    static void viewOrder() {
        String id = readLine("Enter Order ID (e.g. P1000): ").toUpperCase();
        if (id.isEmpty()) { System.out.println("Order ID cannot be empty."); return; }

        for (Order o : loadOrders()) {
            if (o.getId().equalsIgnoreCase(id)) {
                o.printSummary();
                return;
            }
        }
        System.out.println("Order " + id + " not found.");
    }

    // 3. Pizza Maintenance

    static void maintenance() {
        while (true) {
            System.out.println();
            System.out.println("--- Pizza Maintenance ---");
            System.out.println("1. View pizzas");
            System.out.println("2. Add pizza");
            System.out.println("3. Edit pizza");
            System.out.println("4. Delete pizza");
            System.out.println("0. Back");

            int c = readInt("Select: ");
            switch (c) {
                case 0: return;
                case 1: printMenu();    break;
                case 2: addPizza();     break;
                case 3: editPizza();    break;
                case 4: deletePizza();  break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    static void addPizza() {
        String name = readLine("Pizza name: ");
        if (name.isEmpty() || badChars(name)) {
            System.out.println("Name cannot be empty and cannot contain , | ;");
            return;
        }
        String size = readLine("Size (e.g. S/M/L/R): ").toUpperCase();
        if (size.isEmpty() || badChars(size)) {
            System.out.println("Size cannot be empty and cannot contain , | ;");
            return;
        }
        double price = readDouble("Price (RM): ");
        if (price <= 0) { System.out.println("Price must be positive."); return; }

        if (menu.find(name, size) != null) {
            System.out.println("A pizza with that name and size already exists.");
            return;
        }
        menu.add(new Pizza(name, size, price));
        saveMenu();
        System.out.println("Added " + name + "-" + size);
    }

    static void editPizza() {
        if (menu.getPizzas().isEmpty()) { System.out.println("No pizzas to edit."); return; }
        printMenu();

        int idx = readInt("Pizza number to edit (0 = cancel): ");
        if (idx == 0) return;
        if (idx < 1 || idx > menu.getPizzas().size()) { System.out.println("Invalid number."); return; }

        Pizza old = menu.getPizzas().get(idx - 1);

        String name = readLine("New name [" + old.getName() + "]: ");
        if (name.isEmpty()) name = old.getName();
        if (badChars(name)) { System.out.println("Name cannot contain , | ;"); return; }

        String size = readLine("New size [" + old.getSize() + "]: ").toUpperCase();
        if (size.isEmpty()) size = old.getSize();
        if (badChars(size)) { System.out.println("Size cannot contain , | ;"); return; }

        double price = old.getPrice();
        String priceStr = readLine("New price [" + old.getPrice() + "]: ");
        if (!priceStr.isEmpty()) {
            try { price = Double.parseDouble(priceStr); }
            catch (NumberFormatException e) { System.out.println("Invalid price."); return; }
            if (price <= 0) { System.out.println("Price must be positive."); return; }
        }

        boolean same = name.equalsIgnoreCase(old.getName()) && size.equalsIgnoreCase(old.getSize());
        if (!same && menu.find(name, size) != null) {
            System.out.println("Another pizza with that name and size already exists.");
            return;
        }
        menu.set(idx - 1, new Pizza(name, size, price));
        saveMenu();
        System.out.println("Updated pizza #" + idx);
    }

    static void deletePizza() {
        if (menu.getPizzas().isEmpty()) { System.out.println("No pizzas to delete."); return; }
        printMenu();

        int idx = readInt("Pizza number to delete (0 = cancel): ");
        if (idx == 0) return;
        if (idx < 1 || idx > menu.getPizzas().size()) { System.out.println("Invalid number."); return; }

        Pizza removed = menu.remove(idx - 1);
        saveMenu();
        System.out.println("Deleted " + removed.getDisplayName());
    }

    // order file handling 

    static List<Order> loadOrders() {
        List<Order> list = new ArrayList<>();
        File f = new File(ORDERS_FILE);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    list.add(Order.fromRecord(line, menu));
                } catch (RuntimeException e) {
                    // skip malformed line
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + ORDERS_FILE + ": " + e.getMessage());
        }
        return list;
    }
    
    // last 100 orders
    static void appendOrder(Order order) throws IOException {
        List<Order> all = loadOrders();
        all.add(order);
        while (all.size() > MAX_ORDERS) all.remove(0);

        try (PrintWriter pw = new PrintWriter(new FileWriter(ORDERS_FILE))) {
            for (Order o : all) pw.println(o.toRecord());
        }
    }

    static String nextOrderId() {
        int max = 999;
        for (Order o : loadOrders()) {
            String digits = o.getId().replaceAll("\\D", "");
            if (!digits.isEmpty()) max = Math.max(max, Integer.parseInt(digits));
        }
        return "P" + (max + 1);
    }

    // extra

    static void printMenu() {
        System.out.println();
        System.out.println("--- Available Pizzas ---");
        List<Pizza> list = menu.getPizzas();
        if (list.isEmpty()) { System.out.println("(none)"); return; }
        for (int i = 0; i < list.size(); i++)
            System.out.printf("%2d. %s%n", i + 1, list.get(i));
    }

    static void saveMenu() {
        try {
            menu.save();
            System.out.println("Menu saved to " + MENU_FILE);
        } catch (IOException e) {
            System.out.println("Could not save menu: " + e.getMessage());
        }
    }

    static boolean badChars(String s) {
        return s.contains(",") || s.contains("|") || s.contains(";");
    }

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Please enter a whole number."); }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { System.out.println("Please enter a number."); }
        }
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}