package com.newrelic.fit.empcon;

import com.newrelic.fit.config.Instance;
import com.newrelic.insights.publish.ClientConnectionConfiguration;
import com.newrelic.insights.publish.InsightsClient;
import com.newrelic.insights.publish.MultiThreadedInsightsClient;
import com.salesforce.emp.connector.TopicSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;


public class Main {

    private static String configDir = "./config";
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        if (System.getProperty("ConfigDir") != null) {
            configDir = System.getProperty("ConfigDir");
        }
        System.setProperty("logback.configurationFile", configDir + File.separator + "logback.xml");
    }

    public static void main(String[] args) {
        logger.info("Starting.. ");
        String configFile = configDir + File.separator + "empcon-config.yml";

        List<Instance> configuredInstances = ConfigParse.readConfigFileOrEnv(configFile);

        InsightsClient client = new MultiThreadedInsightsClient(Instance.getInsightsUrl(), Instance.getInsightsInsertKey());
        ClientConnectionConfiguration c = new ClientConnectionConfiguration();
        if (Instance.getProxyHost() != null) {
            c.setUseProxy(true);
            logger.info("Setting proxy_host to " + Instance.getProxyHost());
            c.setProxyHost(Instance.getProxyHost());
        }
        if (Instance.getProxyPort() != 0) {
            logger.info("Setting proxy_port to " + Instance.getProxyPort());
            c.setProxyPort(Instance.getProxyPort());
        }
        if (Instance.getProxyUser() != null) {
            logger.info("Setting proxy_user to " + Instance.getProxyUser());
            c.setProxyUsername(Instance.getProxyUser());
        }
        if (Instance.getProxyPassword() != null) {
            c.setProxyPassword(Instance.getProxyPassword());
        }
        client.init(c);


        configuredInstances.forEach((instance) -> {
            logger.info("Creating consumer: " + instance);
            MonitoringConsumer monitoringConsumer = new MonitoringConsumer();

            TopicSubscription subscription = monitoringConsumer.connect(instance, client);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    logger.info("unsubscribe consumer: " + instance.getUrl() + subscription);
                    subscription.cancel();
                }
            });

        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("stop InsightsClient...");
                client.destroyClient();
            }
        });
    }
}
