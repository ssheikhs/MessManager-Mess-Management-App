package com.example.messmanagement;

public class User {

    private String userId;
    private String fullName;
    private String username;
    private String password;
    private String contact;
    private String address;
    private String parentContact;
    private boolean admin;
    private String status;

    public User(String userId,
                String fullName,
                String username,
                String password,
                String contact,
                String address,
                String parentContact,
                boolean admin,
                String status) {

        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.contact = contact;
        this.address = address;
        this.parentContact = parentContact;
        this.admin = admin;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getContact() {
        return contact;
    }

    public String getAddress() {
        return address;
    }

    public String getParentContact() {
        return parentContact;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getStatus() {
        return status;
    }
}
