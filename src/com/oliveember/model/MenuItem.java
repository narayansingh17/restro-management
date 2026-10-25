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

    public MenuItem(int id, String name, String category, String description, int price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
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

    @Override
    public String toString() {
        return name + " (" + category + ", ₹" + price + ")";
    }
}
