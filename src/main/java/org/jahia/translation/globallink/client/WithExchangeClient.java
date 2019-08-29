package org.jahia.translation.globallink.client;

import com.globallink.api.GLExchange;
import com.globallink.api.config.ProjectDirectorConfig;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global Link client with callback
 * ensure that the class loader is the right one.
 */
public class WithExchangeClient {

    private static final Logger log = LoggerFactory.getLogger(WithExchangeClient.class);

    public static boolean execute(GlobalLinkConfigurationDTO config, WithGLExchangeClientCallBack callback) {

        // store current class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // due to JDK11, jaws libs are include in the bundle
        // We need to change the class loader to locate classes correctly
        Thread.currentThread().setContextClassLoader(WithExchangeClient.class.getClassLoader());
        ProjectDirectorConfig gblConfig = new ProjectDirectorConfig();
        gblConfig.setUrl(config.getUrl());
        gblConfig.setUsername(config.getUsername());
        gblConfig.setPassword(config.getPassword());
        gblConfig.setUserAgent(config.getUserAgent());
        try {
            GLExchange exchangeClient = new GLExchange(gblConfig);
            if (exchangeClient == null) {
                return false;
            }
            callback.execute(exchangeClient);
        } catch (Exception ex) {
            log.error("Error while generating GLExchange client: ", ex);
            return false;
        }
        Thread.currentThread().setContextClassLoader(cl);
        return true;
    }
}
