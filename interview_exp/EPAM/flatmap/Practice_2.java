package interview_exp.EPAM;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Practice_2 {
    
    class Order {
        List<Item> items;
    }

    class Item {
        String category;
        int price;

        public Item(String category, int price) {
            this.category = category;
            this.price = price;
        }

        public String getCategory() {
            return category;
        }

        public int getPrice() {
            return price;
        }
    }

    public static void main(String[] args) {
        //given list of orders, and each order has list of items, 
        //give categorywise total prices.
        List<Order> orders = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        items.add(new Practice_2().new Item("Electronics", 100));
        items.add(new Practice_2().new Item("Clothing", 200));
        Order order = new Practice_2().new Order();
        order.items = items;
        orders.add(order);

        var ans = orders.stream()
                    .flatMap(o -> o.items.stream())
                    .collect(Collectors.groupingBy(Item::getCategory, Collectors.summingInt(Item::getPrice)));
    
        System.out.println(ans);    
    }
}
