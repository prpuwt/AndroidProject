package com.example.mybill.model;

public class Bill {
    private long id;
    private String date;         // yyyy-MM-dd
    private String personName;  // 工人姓名
    private String location;     // 工作地点
    private String description;  // 事项说明
    private double amount;       // 金额
    private String createdAt;
    private int settled;

    public Bill() {}

    public Bill(String date, String personName, String location, String description, double amount) {
        this.date = date;
        this.personName = personName;
        this.location = location;
        this.description = description;
        this.amount = amount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getSettled() { return settled; }
    public void setSettled(int settled) { this.settled = settled; }
    public boolean isSettled() { return settled == 1; }
}
