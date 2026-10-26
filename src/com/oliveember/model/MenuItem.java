package com.oliveember.model;

/**
 * Immutable catalog descriptor for a food or beverage item on the fixed menu.
 * Prices are in whole rupees (INR).
 */
public class MenuItem {

    private final int id;
    private final String name;
    private final String category;
    private final String description;
    private final int price;
    private final String imageUrl;

    public MenuItem(int id, String name, String category, String description, int price) {
        this(id, name, category, description, price, defaultImageFor(id));
    }

    public MenuItem(int id, String name, String category, String description, int price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.imageUrl = (imageUrl != null && !imageUrl.isEmpty()) ? imageUrl : defaultImageFor(id);
    }

    private static String defaultImageFor(int id) {
        return switch (id) {
            case 1 -> "/images/margherita.jpg";
            case 2 -> "/images/flatbread.jpg";
            case 3 -> "/images/risotto.jpg";
            case 4 -> "/images/ravioli.jpg";
            case 5 -> "/images/focaccia.jpg";
            case 6 -> "/images/burrata.jpg";
            case 7 -> "/images/tiramisu.jpg";
            case 8 -> "/images/cake.jpg";
            case 9 -> "/images/hibiscus.jpg";
            case 10 -> "/images/coldbrew.jpg";
            default -> "/images/margherita.jpg";
        };
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return name + " (" + category + ", ₹" + price + ")";
    }
}
