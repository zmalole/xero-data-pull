package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;

@Data
@AllArgsConstructor
public class JournalEntry implements Comparable<JournalEntry> {
    private int tenantId;
    private Date txDate;
    private int txNum;
    private String currency;
    private String documentNumber;
    private String privateNote;

    public JournalEntry(int tenantId, Date txDate, int txNum, String currency, String documentNumber) {
        this.tenantId = tenantId;
        this.txDate = txDate;
        this.txNum = txNum;
        this.currency = currency;
        this.documentNumber = documentNumber;
    }

    @Override
    public int compareTo(JournalEntry o) {
        return Integer.compare(o.tenantId, tenantId);
    }
}
