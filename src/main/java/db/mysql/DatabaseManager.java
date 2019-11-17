package db.mysql;

import com.xero.api.client.AccountingApi;
import com.xero.models.accounting.*;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import models.JournalEntry;
import models.JournalLine;
import models.Tenant;
import org.threeten.bp.DateTimeUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DatabaseManager {
    private final AccountingApi accountingApi;
    private Connection connect = null;
    private Statement statement = null;
    private ResultSet resultSet = null;

    private void connect() throws SQLException, IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/main/resources/db/credentials.properties"));
        connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/?rewriteBatchedStatements=true", properties);
        statement = connect.createStatement();
        databasePreparation(statement);
    }

    private void databasePreparation(Statement statement) throws SQLException {
        statement.execute("CREATE DATABASE IF NOT EXISTS tasly;");
        statement.execute("USE tasly;");
        statement.execute("CREATE TABLE IF NOT EXISTS tenant ( id INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY (id), name VARCHAR(255) NOT NULL, UNIQUE KEY (name) );");
        statement.execute("CREATE TABLE IF NOT EXISTS journal_entry ( tenant INTEGER NOT NULL, FOREIGN KEY (tenant) REFERENCES tenant (id), tx_date DATE NOT NULL, tx_num INTEGER NOT NULL, PRIMARY KEY (tenant, tx_date, tx_num), UNIQUE KEY (tenant, tx_num), currency CHAR(3) DEFAULT NULL, doc_number VARCHAR(32) DEFAULT NULL, private_note VARCHAR(4000) DEFAULT NULL );");
        statement.execute("CREATE TABLE IF NOT EXISTS `account` ( tenant INTEGER NOT NULL, id INTEGER NOT NULL, PRIMARY KEY (tenant, id), name VARCHAR(255) NOT NULL, UNIQUE KEY (tenant, name), `type` SMALLINT NOT NULL );");
        statement.execute("CREATE TABLE IF NOT EXISTS journal_line ( tenant INTEGER NOT NULL, tx_date DATE NOT NULL, tx_num INTEGER NOT NULL, line_num INTEGER NOT NULL, PRIMARY KEY (tenant, tx_date, tx_num, line_num), FOREIGN KEY (tenant, tx_date, tx_num) REFERENCES journal_entry(tenant, tx_date, tx_num) ON DELETE CASCADE ON UPDATE CASCADE, amount BIGINT NOT NULL, account INTEGER NOT NULL, description VARCHAR(4000) DEFAULT NULL );");
    }

    private void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connect != null) {
                connect.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Tenant> getTenants() {
        List<Tenant> tenantList = new ArrayList<>();
        try {
            connect();
            resultSet = statement.executeQuery("SELECT * FROM tenant");
            while (resultSet.next()) {
                tenantList.add(new Tenant(resultSet.getInt("id"), resultSet.getString("name")));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return tenantList;
    }

    public List<JournalEntry> getJournalEntries(int tenantId) {
        List<JournalEntry> journalEntryList = new ArrayList<>();
        try {
            connect();
            resultSet = statement.executeQuery("SELECT * FROM journal_entry WHERE tenant = " + tenantId);
            while (resultSet.next()) {
                journalEntryList.add(new JournalEntry(
                        resultSet.getInt("tenant"),
                        resultSet.getDate("tx_date"),
                        resultSet.getInt("tx_num"),
                        resultSet.getString("currency"),
                        resultSet.getString("doc_number"),
                        resultSet.getString("private_note")));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return journalEntryList;
    }

    public List<JournalLine> getJournalLines(int tenantId) {
        List<JournalLine> journalLineList = new ArrayList<>();
        try {
            connect();
            resultSet = statement.executeQuery("SELECT * FROM journal_line WHERE tenant = " + tenantId);
            while (resultSet.next()) {
                journalLineList.add(new JournalLine(
                        resultSet.getInt("tenant"),
                        resultSet.getDate("tx_date"),
                        resultSet.getInt("tx_num"),
                        resultSet.getInt("line_num"),
                        resultSet.getInt("amount"),
                        resultSet.getInt("account"),
                        resultSet.getString("description")));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return journalLineList;
    }

    public Set<models.Account> getAccounts(int tenantId) {
        Set<models.Account> accountSet = new HashSet<>();
        try {
            connect();
            resultSet = statement.executeQuery("SELECT * FROM account WHERE tenant = " + tenantId);
            while (resultSet.next()) {
                accountSet.add(new models.Account(
                        resultSet.getInt("tenant"),
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("type")));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return accountSet;
    }

    public List<Tenant> pullTenants() {
        List<Tenant> tenantList = new ArrayList<>();
        String query = "INSERT INTO tenant (name) SELECT '%s' WHERE NOT EXISTS(SELECT * from tenant where name='%s')";
        try {
            List<String> contactNameList = accountingApi.getContacts(null, null, null, null, null, null)
                    .getContacts().stream().map(Contact::getName).collect(Collectors.toList());
            connect();
            long start = System.currentTimeMillis();
            int i = 0;
            for (String name : contactNameList) {
                i++;
                statement.addBatch(String.format(query, name, name));
                if (i % 1000 == 0) statement.executeBatch();
                tenantList.add(new Tenant(name));
            }
            statement.executeBatch();
            System.out.println("<" + query + "> execution time taken = " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return tenantList;
    }

    public Pair<List<JournalEntry>, List<Invoice>> pullInvoices(Integer tenantId) {
        List<JournalEntry> journalEntryList = new ArrayList<>();
        List<Invoice> tenantInvoices = null;
        String query = "REPLACE INTO journal_entry " +
                "SET tenant = ?, tx_date = ?, tx_num = ?, currency = ?, doc_number = ?;";
        try {
            connect();
            resultSet = statement.executeQuery("SELECT name FROM tenant WHERE id = " + tenantId);
            while (resultSet.next()) {
                String tenantName = resultSet.getString(1);
                tenantInvoices = accountingApi.getInvoices(null, null, null, null, null, null, null, null, null, null, null)
                        .getInvoices().stream().filter(i ->
                                i.getType() == Invoice.TypeEnum.ACCREC && i.getContact().getName().equals(tenantName))
                        .collect(Collectors.toList());
                int i = 0;
                PreparedStatement ps = connect.prepareStatement(query);
                long start = System.currentTimeMillis();
                for (Invoice invoice : tenantInvoices) {
                    i++;
                    Date invoiceDate = DateTimeUtils.toSqlDate(invoice.getDate());
                    String currency = invoice.getCurrencyCode().getValue();
                    String invoiceNumber = invoice.getInvoiceNumber();
                    int txNum = invoice.getInvoiceID().hashCode();
                    ps.setInt(1, tenantId);
                    ps.setDate(2, invoiceDate);
                    ps.setInt(3, txNum);
                    ps.setString(4, currency);
                    ps.setString(5, invoiceNumber);
                    ps.addBatch();
                    if (i % 1000 == 0) ps.executeBatch();
                    journalEntryList.add(new JournalEntry(tenantId, invoiceDate, txNum, currency, invoiceNumber));
                }
                ps.executeBatch();
                System.out.println("<" + query + "> execution time taken = " + (System.currentTimeMillis() - start) + " ms");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        List<JournalLine> journalLineList = pullInvoicesDetails(tenantInvoices, journalEntryList);
        pullAccountDetails(journalLineList);
        return new Pair<>(journalEntryList, tenantInvoices);
    }

    public List<JournalLine> pullInvoicesDetails(List<Invoice> tenantInvoices, List<JournalEntry> journalEntryList) {
        List<JournalLine> journalLineList = new ArrayList<>();
        int accountConsultingServicesCode = -1;
        String query1 = "REPLACE INTO journal_line " +
                "SET tenant = ?, tx_date = ?, tx_num = ?, line_num = ?, amount = ?, account = ?, description = ?;";
        String query2 = "REPLACE INTO journal_line " +
                "SET tenant = ?, tx_date = ?, tx_num = ?, line_num = ?, amount = ?, account = ?;";
        try {
            connect();
            PreparedStatement ps1 = connect.prepareStatement(query1);
            PreparedStatement ps2 = connect.prepareStatement(query2);
            List<UUID> invoiceIdList = tenantInvoices.stream().map(Invoice::getInvoiceID).collect(Collectors.toList());
            long start = System.currentTimeMillis();
            int i = 0;
            for (UUID invoiceId : invoiceIdList) {
                for (Invoice invoice : accountingApi.getInvoice(invoiceId).getInvoices()) {
                    int lineNum = 0;
                    for (LineItem lineItem : invoice.getLineItems()) {
                        JournalEntry journalEntry = journalEntryList.get(i);
                        int tenantId = journalEntry.getTenantId();
                        Date invoiceDate = journalEntry.getTxDate();
                        int invoiceNumber = journalEntry.getTxNum();
                        int lineAmount = lineItem.getLineAmount().intValue();
                        int accountReceivableCode = Integer.parseInt(lineItem.getAccountCode());
                        String description = lineItem.getDescription();

                        lineNum++;
                        journalLineList.add(new JournalLine(tenantId, invoiceDate, invoiceNumber, lineNum,
                                lineAmount, accountReceivableCode, description));
                        ps1.setInt(1, tenantId);
                        ps1.setDate(2, invoiceDate);
                        ps1.setInt(3, invoiceNumber);
                        ps1.setInt(4, lineNum);
                        ps1.setInt(5, lineAmount);
                        ps1.setInt(6, accountReceivableCode);
                        ps1.setString(7, description);
                        ps1.addBatch();

                        lineNum++;
                        journalLineList.add(new JournalLine(tenantId, invoiceDate, invoiceNumber, lineNum,
                                -lineAmount, accountConsultingServicesCode));
                        ps2.setInt(1, tenantId);
                        ps2.setDate(2, invoiceDate);
                        ps2.setInt(3, invoiceNumber);
                        ps2.setInt(4, lineNum);
                        ps2.setInt(5, -lineAmount);
                        ps2.setInt(6, accountConsultingServicesCode);
                        ps2.addBatch();

                        if (i % 1000 == 0) {
                            ps1.executeBatch();
                            ps2.executeBatch();
                        }
                    }
                    i++;
                }
            }
            ps1.executeBatch();
            ps2.executeBatch();
            System.out.println("<" + query1 + "> and <" + query2 + "> execution time taken = " +
                    (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return journalLineList;
    }

    public Set<models.Account> pullAccountDetails(List<JournalLine> journalLineList) {
        Set<models.Account> accountModelSet = new HashSet<>();
        String query = "REPLACE INTO account SET tenant = ?, id = ?, name = ?, type = ?;";
        try {
            List<Account> accountList = accountingApi.getAccounts(null, null, null).getAccounts();
            Set<Integer> accountIdSet = journalLineList.stream().map(JournalLine::getAccount).collect(Collectors.toSet());
            List<Account> targetAccountList = accountList.stream()
                    .filter(a -> accountIdSet.contains(Integer.parseInt(a.getCode()))).collect(Collectors.toList());
            Account consultingServicesAccount = new Account();
            consultingServicesAccount.setName("Consulting Services");
            consultingServicesAccount.setType(AccountType.EXPENSE);

            Set<models.Account> uniqueAccountSet = new HashSet<>();
            for (JournalLine journalLine : journalLineList) {
                int tenantId = journalLine.getTenantId();
                int accountId = journalLine.getAccount();
                String accountName = targetAccountList.stream()
                        .filter(ta -> Integer.parseInt(ta.getCode()) == accountId)
                        .findAny().orElse(consultingServicesAccount).getName();
                uniqueAccountSet.add(new models.Account(tenantId, accountName, accountId));
            }

            connect();
            PreparedStatement ps = connect.prepareStatement(query);
            long start = System.currentTimeMillis();
            int i = 0;
            for (models.Account account : uniqueAccountSet) {
                i++;
                int tenantId = account.getTenantId();
                String accountName = account.getName();
                int accountType = account.getType();
                ps.setInt(1, tenantId);
                ps.setInt(2, i);
                ps.setString(3, accountName);
                ps.setInt(4, accountType);
                ps.addBatch();
                if (i % 1000 == 0) ps.executeBatch();
                accountModelSet.add(new models.Account(tenantId, i, accountName, accountType));
            }
            ps.executeBatch();
            System.out.println("<" + query + "> execution time taken = " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
        return accountModelSet;
    }
}