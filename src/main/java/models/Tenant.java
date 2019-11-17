package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class Tenant implements Comparable<Tenant> {
    private int id;
    private String name;

    public Tenant(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Tenant o) {
        return o.getName().compareTo(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(name, tenant.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
