package external_connectors;

import com.xero.api.ApiClient;
import com.xero.api.Config;
import com.xero.api.JsonConfig;
import com.xero.api.RsaSignerFactory;
import com.xero.api.client.AccountingApi;

import java.io.FileInputStream;

public class XeroConnector {
    public AccountingApi getConnection() {
        AccountingApi accountingApi = null;
        try {
            Config config = JsonConfig.getInstance();
            try (FileInputStream privateKeyStream = new FileInputStream(config.getPathToPrivateKey())) {
                RsaSignerFactory signerFactory = new RsaSignerFactory(privateKeyStream, config.getPrivateKeyPassword());
                ApiClient apiClientForAccounting = new ApiClient(config.getApiUrl(), null, null, null);
                accountingApi = new AccountingApi(config, signerFactory);
                accountingApi.setApiClient(apiClientForAccounting);
                accountingApi.setOAuthToken(config.getConsumerKey(), config.getConsumerSecret());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return accountingApi;
    }
}
