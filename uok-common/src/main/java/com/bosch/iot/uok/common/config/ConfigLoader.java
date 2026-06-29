package com.bosch.iot.uok.common.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader for UOK.
 * Loads configuration from system properties, environment variables, and properties files.
 * Priority: System properties > Environment variables > Properties file > Defaults
 */
public class ConfigLoader {

    private static final String CONFIG_FILE_PROPERTY = "uok.config.file";
    private static final String CONFIG_FILE_ENV = "UOK_CONFIG_FILE";
    private static final String DEFAULT_CONFIG_FILE = "uok.properties";

    private ConfigLoader() {
        // Prevent instantiation
    }

    /**
     * Load UOK configuration from all available sources.
     *
     * @return loaded UokConfig instance
     */
    public static UokConfig load() {
        UokConfig config = new UokConfig();
        Properties props = loadProperties();

        config.setServiceName(getString(props, "uok.service.name", config.getServiceName()));
        config.setBizDomain(getString(props, "uok.biz.domain", config.getBizDomain()));
        config.setTeamName(getString(props, "uok.team.name", config.getTeamName()));
        config.setEnv(getString(props, "uok.env", config.getEnv()));
        config.setAgentEnabled(getBoolean(props, "uok.agent.enabled", config.isAgentEnabled()));
        config.setLogEnabled(getBoolean(props, "uok.log.enable", config.isLogEnabled()));
        config.setTraceEnabled(getBoolean(props, "uok.trace.enable", config.isTraceEnabled()));
        config.setMetricsEnabled(getBoolean(props, "uok.metrics.enable", config.isMetricsEnabled()));
        config.setDegradeCpuThreshold(getInt(props, "uok.degrade.cpu-threshold", config.getDegradeCpuThreshold()));
        config.setDegradeLatencyIncrease(getInt(props, "uok.degrade.latency-increase", config.getDegradeLatencyIncrease()));

        // Load sampler config
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setHeadRate(getDouble(props, "uok.sampler.head-rate", samplerConfig.getHeadRate()));
        samplerConfig.setErrorAlways(getBoolean(props, "uok.sampler.error-always", samplerConfig.isErrorAlways()));
        samplerConfig.setDeviceRatio(getDouble(props, "uok.sampler.device-ratio", samplerConfig.getDeviceRatio()));
        config.setSamplerConfig(samplerConfig);

        // Load gray config
        GrayConfig grayConfig = new GrayConfig();
        String serviceListStr = getString(props, "uok.gray.service-list", "");
        if (!serviceListStr.isEmpty()) {
            for (String svc : serviceListStr.split(",")) {
                String trimmed = svc.trim();
                if (!trimmed.isEmpty()) {
                    grayConfig.addService(trimmed);
                }
            }
        }
        grayConfig.setInstanceRatio(getDouble(props, "uok.gray.instance-ratio", grayConfig.getInstanceRatio()));
        config.setGrayConfig(grayConfig);

        return config;
    }

    /**
     * Load configuration from a specific properties file.
     *
     * @param filePath path to the properties file
     * @return loaded UokConfig instance
     */
    public static UokConfig loadFromFile(String filePath) {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(filePath)) {
            props.load(is);
        } catch (IOException e) {
            // Fall back to default loading
            return load();
        }

        UokConfig config = new UokConfig();
        config.setServiceName(props.getProperty("uok.service.name", config.getServiceName()));
        config.setBizDomain(props.getProperty("uok.biz.domain", config.getBizDomain()));
        config.setTeamName(props.getProperty("uok.team.name", config.getTeamName()));
        config.setEnv(props.getProperty("uok.env", config.getEnv()));
        config.setAgentEnabled(Boolean.parseBoolean(props.getProperty("uok.agent.enabled", String.valueOf(config.isAgentEnabled()))));
        config.setLogEnabled(Boolean.parseBoolean(props.getProperty("uok.log.enable", String.valueOf(config.isLogEnabled()))));
        config.setTraceEnabled(Boolean.parseBoolean(props.getProperty("uok.trace.enable", String.valueOf(config.isTraceEnabled()))));
        config.setMetricsEnabled(Boolean.parseBoolean(props.getProperty("uok.metrics.enable", String.valueOf(config.isMetricsEnabled()))));
        config.setDegradeCpuThreshold(Integer.parseInt(props.getProperty("uok.degrade.cpu-threshold", String.valueOf(config.getDegradeCpuThreshold()))));
        config.setDegradeLatencyIncrease(Integer.parseInt(props.getProperty("uok.degrade.latency-increase", String.valueOf(config.getDegradeLatencyIncrease()))));

        return config;
    }

    /**
     * Load properties from all available sources.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();

        // Load from classpath default config file
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
            // Ignore, use defaults
        }

        // Load from external config file if specified
        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configFilePath == null || configFilePath.isEmpty()) {
            configFilePath = System.getenv(CONFIG_FILE_ENV);
        }
        if (configFilePath != null && !configFilePath.isEmpty()) {
            try (InputStream is = new FileInputStream(configFilePath)) {
                Properties externalProps = new Properties();
                externalProps.load(is);
                props.putAll(externalProps);
            } catch (IOException ignored) {
                // Ignore, use existing props
            }
        }

        return props;
    }

    /**
     * Get string property from system properties, environment variables, or properties file.
     */
    private static String getString(Properties props, String key, String defaultValue) {
        // System property takes highest priority
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Environment variable (convert dots to underscores, uppercase)
        String envKey = key.replace('.', '_').replace('-', '_').toUpperCase();
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Properties file
        value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    private static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = getString(props, key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static int getInt(Properties props, String key, int defaultValue) {
        String value = getString(props, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double getDouble(Properties props, String key, double defaultValue) {
        String value = getString(props, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
