/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.checkmates.model;

/**
 *
 * @author nguyenp
 */

public class Librarian {
    private String libName;
    private String email;
    private String libID;
    
    public Librarian(String libName, String email, String libID) {
        this.libName = libName;
        this.email = email;
        this.libID = libID;
    }
    
    public String getLibName() {
        return libName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getLibID() {
        return libID;
    }
}



