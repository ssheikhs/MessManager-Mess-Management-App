package com.example.messmanagement;

public class Member {
    public int id;
    public String name;
    public String role;
    public int totalMeals;
    public double totalPaid;

    // Extra profile fields (optional)
    public String contact;
    public String address;
    public String parentContact;

    // Full constructor with all fields
    public Member(int id,
                  String name,
                  String role,
                  int totalMeals,
                  double totalPaid,
                  String contact,
                  String address,
                  String parentContact) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.totalMeals = totalMeals;
        this.totalPaid = totalPaid;
        this.contact = contact;
        this.address = address;
        this.parentContact = parentContact;
    }

    // Existing constructor used in MessDBHelper (keep it!)
    public Member(int id, String name, String role, int totalMeals, double totalPaid) {
        this(id, name, role, totalMeals, totalPaid, null, null, null);
    }

    // Simple constructor used other places
    public Member(int id, String name, String role) {
        this(id, name, role, 0, 0.0, null, null, null);
    }
}
