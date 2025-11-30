package com.checkmates.model;

public class Borrower {
    private String cardId;
    private String ssn;
    private String name;
    private String address;
    private String phone;

    public Borrower(String ssn, String name, String address, String phone) {
        this.ssn = ssn;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getSsn() {
        return ssn;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
