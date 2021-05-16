package com.smd.leansnow;


import java.util.Objects;

public class SkiResort {
    String name;
    Status status;

    private SkiResort() {
    }

    public SkiResort(String name, String status) {
        this.name = name;
        this.status = Status.valueOf(status.toLowerCase());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SkiResort{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkiResort skiResort = (SkiResort) o;
        return name.equals(skiResort.name) &&
                status == skiResort.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, status);
    }

    enum Status {
        OPEN,
        CLOSED
        ;
    }
}
