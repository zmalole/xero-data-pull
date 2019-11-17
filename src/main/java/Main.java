import db.mysql.DatabaseManager;
import external_connectors.XeroConnector;

public class Main {

    public static void main(String[] args) {
        XeroConnector xeroConnector = new XeroConnector();
        DatabaseManager databaseManager = new DatabaseManager(xeroConnector.getConnection());
        databaseManager.pullTenants();
        databaseManager.pullInvoices(38);
    }
}
