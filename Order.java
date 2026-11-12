import java.util.*;

/**
 * One customer order. Items are stored as (reference to Pizza object, quantity).
 * No Map is used — a List of OrderLine is used instead.
 */
public class Order {
    public static final double TAX_RATE = 0.05;

    private final String id;      // e.g. P1000
    private final String date;    // dd/MM/yyyy
    private final List<OrderLine> lines = new ArrayList<>();

    /** One ordered item: pointer to a Pizza object + quantity. */
    public static class OrderLine {
        private final Pizza pizza;
        private int quantity;

        public OrderLine(Pizza pizza, int quantity) {
            this.pizza    = pizza;
            this.quantity = quantity;
        }
        public Pizza getPizza()   { return pizza; }
        public int   getQuantity(){ return quantity; }
        public void  addQuantity(int q) { quantity += q; }
        public double getLineTotal()    { return pizza.getPrice() * quantity; }
    }

    public Order(String id, String date) {
        this.id   = id;
        this.date = date;
    }

    public String getId()            { return id; }
    public String getDate()          { return date; }
    public List<OrderLine> getLines(){ return lines; }

    /**
     * Adds a pizza by reference. If the exact same Pizza object is already
     * in this order, only the quantity is increased.
     */
    public void addPizza(Pizza pizza, int quantity) {
        if (pizza == null || quantity <= 0) return;
        for (OrderLine line : lines) {
            if (line.getPizza() == pizza) {   // identity: same object
                line.addQuantity(quantity);
                return;
            }
        }
        lines.add(new OrderLine(pizza, quantity));
    }

    public double getSubtotal() {
        double s = 0;
        for (OrderLine l : lines) s += l.getLineTotal();
        return round2(s);
    }
    public double getTax()   { return round2(getSubtotal() * TAX_RATE); }
    public double getTotal() { return round2(getSubtotal() + getTax()); }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Prints the receipt in the required Figure-1 layout. */
    public void printSummary() {
        System.out.println();
        System.out.println("Order ID: " + id + "\t\tDate: " + date);
        System.out.printf("%-4s %-24s %-10s %10s%n", "No", "Pizza", "Quantity", "Total");
        System.out.println("------------------------------------------------------------");
        int no = 1;
        for (OrderLine l : lines) {
            System.out.printf("%-4d %-24s %-10d %10.2f%n",
                    no++, l.getPizza().getDisplayName(), l.getQuantity(), l.getLineTotal());
        }
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-34s %10s %10.2f%n", "", "Tax(5%)", getTax());
        System.out.printf("%-34s %10s %10.2f%n", "", "TOTAL (RM)", getTotal());
        System.out.println();
    }

    /** One-line record saved to orders.txt:
     *  id;date;name|size|price|qty;name|size|price|qty;...          */
    public String toRecord() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(';').append(date);
        for (OrderLine l : lines) {
            Pizza p = l.getPizza();
            sb.append(';')
              .append(p.getName()).append('|')
              .append(p.getSize()).append('|')
              .append(String.format("%.2f", p.getPrice())).append('|')
              .append(l.getQuantity());
        }
        return sb.toString();
    }

    /**
     * Rebuilds an Order from a saved line. Each item is re-linked to the
     * matching Pizza object in the current menu (or a new Pizza if the menu
     * no longer contains it).
     */
    public static Order fromRecord(String record, Menu menu) {
        String[] parts = record.split(";");
        Order order = new Order(parts[0].trim(), parts[1].trim());
        for (int i = 2; i < parts.length; i++) {
            String[] f    = parts[i].split("\\|");
            String name   = f[0].trim();
            String size   = f[1].trim();
            double price  = Double.parseDouble(f[2].trim());
            int qty       = Integer.parseInt(f[3].trim());

            Pizza p = menu.find(name, size);
            if (p == null) p = new Pizza(name, size, price);
            order.addPizza(p, qty);
        }
        return order;
    }
}