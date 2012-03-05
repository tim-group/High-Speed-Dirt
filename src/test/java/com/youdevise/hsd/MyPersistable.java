package com.youdevise.hsd;

import java.io.Serializable;

@javax.persistence.Entity
@javax.persistence.Table(name="test")
public class MyPersistable implements Serializable {
    private static final long serialVersionUID = -7033663517388449709L;
    
    @javax.persistence.Id
    @javax.persistence.Column(name="ID")
    private int id;
    @javax.persistence.Column(name="NAME")
    private String name;
    
    public int getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String newName) { name = newName; }
    
}