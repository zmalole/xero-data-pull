package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;

@Data
@AllArgsConstructor
public class JournalLine implements Comparable<JournalLine> {
    private int tenantId;
    private Date txDate;
    private int txNum;
    private int lineNum;
    private int amount;
    private int account;
    private String description;

    public JournalLine(int tenantId, Date txDate, int txNum, int lineNum, int amount, int account) {
        this.tenantId = tenantId;
        this.txDate = txDate;
        this.txNum = txNum;
        this.lineNum = lineNum;
        this.amount = amount;
        this.account = account;
    }

    @Override
    public int compareTo(JournalLine o) {
        int c;
        c = Integer.compare(o.tenantId, tenantId);
        if (c == 0)
            c = Integer.compare(o.lineNum, lineNum);
        if (c == 0)
            c = Integer.compare(o.amount, amount);
        return c;
    }
}
