package com.example.drools.model;

/**
 * Simple fact class used by the sample Drools rules.
 */
public class Person {

    private String name;
    private int age;
    private boolean adult;

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isAdult() {
        return adult;
    }

    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", adult=" + adult + "}";
    }
}
