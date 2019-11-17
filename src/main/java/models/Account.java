package models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Account implements Comparable<Account> {
    private int tenantId;
    private int id;
    private String name;
    private int type;

    public Account(int tenantId, String name, int type) {
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
    }

    @Override
    public int compareTo(Account o) {
        int c;
        c = Integer.compare(o.tenantId, tenantId);
        if (c == 0)
            c = Integer.compare(o.id, id);
        return c;
    }
}
