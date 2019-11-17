import com.xero.api.client.AccountingApi;
import com.xero.models.accounting.Invoice;
import db.mysql.DatabaseManager;
import external_connectors.XeroConnector;
import javafx.util.Pair;
import models.Account;
import models.JournalEntry;
import models.JournalLine;
import models.Tenant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class DatabaseManagerTest {
    private DatabaseManager databaseManager;

    @Before
    public void preparation() {
        XeroConnector xeroConnector = new XeroConnector();
        AccountingApi accountingApi = xeroConnector.getConnection();
        databaseManager = new DatabaseManager(accountingApi);
    }

    @Test
    public void pullTenants() {
        List<Tenant> expectedTenantList = databaseManager.pullTenants();
        List<Tenant> actualTenantList = databaseManager.getTenants();
        Arrays.asList(expectedTenantList, actualTenantList).forEach(Collections::sort);

        Assert.assertEquals("Database contains wrong tenant data",
                expectedTenantList, actualTenantList);
    }

    @Test
    public void pullInvoices() {
        databaseManager.pullTenants();
        List<Tenant> tenantList = databaseManager.getTenants();
        int randomIndex = new Random().nextInt(tenantList.size() - 2) + 1;
        Tenant targetTenant = tenantList.get(randomIndex);
        int randomTenantId = targetTenant.getId();
        List<JournalEntry> expectedJournalEntryList = databaseManager.pullInvoices(randomTenantId).getKey();
        List<JournalEntry> actualJournalEntryList = databaseManager.getJournalEntries(randomTenantId);
        Arrays.asList(expectedJournalEntryList, actualJournalEntryList).forEach(Collections::sort);

        System.out.println("Verify invoices of " + targetTenant + " tenant");
        Assert.assertEquals("Database contains wrong journal_entry data",
                expectedJournalEntryList, actualJournalEntryList);
    }

    @Test
    public void pullInvoicesDetails() {
        databaseManager.pullTenants();
        List<Tenant> tenantList = databaseManager.getTenants();
        int randomIndex = new Random().nextInt(tenantList.size() - 2) + 1;
        Tenant targetTenant = tenantList.get(randomIndex);
        int randomTenantId = targetTenant.getId();
        Pair<List<JournalEntry>, List<Invoice>> journalEntryAndInvoiceListsPair = databaseManager.pullInvoices(randomTenantId);
        List<JournalLine> expectedJournalLineList =
                databaseManager.pullInvoicesDetails(
                        journalEntryAndInvoiceListsPair.getValue(), journalEntryAndInvoiceListsPair.getKey());
        List<JournalLine> actualJournalLineList = databaseManager.getJournalLines(randomTenantId);
        Arrays.asList(expectedJournalLineList, actualJournalLineList).forEach(Collections::sort);

        System.out.println("Verify journal lines of " + targetTenant + " tenant");
        Assert.assertEquals("Database contains wrong journal_line data",
                expectedJournalLineList, actualJournalLineList);
    }

    @Test
    public void pullAccountDetails() {
        databaseManager.pullTenants();
        List<Tenant> tenantList = databaseManager.getTenants();
        int randomIndex = new Random().nextInt(tenantList.size() - 2) + 1;
        Tenant targetTenant = tenantList.get(randomIndex);
        int randomTenantId = targetTenant.getId();
        Pair<List<JournalEntry>, List<Invoice>> journalEntryAndInvoiceListsPair = databaseManager.pullInvoices(randomTenantId);
        List<JournalLine> journalLineList =
                databaseManager.pullInvoicesDetails(
                        journalEntryAndInvoiceListsPair.getValue(), journalEntryAndInvoiceListsPair.getKey());
        Set<Account> expectedAccountList = databaseManager.pullAccountDetails(journalLineList);
        Set<Account> actualAccountList = databaseManager.getAccounts(randomTenantId);

        System.out.println("Verify journal lines of " + targetTenant + " tenant");
        Assert.assertEquals("Database contains wrong account data",
                expectedAccountList, actualAccountList);
    }
}