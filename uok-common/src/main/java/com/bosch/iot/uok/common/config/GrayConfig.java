package com.bosch.iot.uok.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gray release configuration.
 * Controls gradual rollout of observability instrumentation.
 */
public class GrayConfig {

    private List<String> serviceList = new ArrayList<>();
    private double instanceRatio = 1.0;
    private List<String> instanceIps = new ArrayList<>();
    private List<String> instanceTags = new ArrayList<>();
    private double trafficRatio = 1.0;

    public GrayConfig() {
    }

    /**
     * Check if a specific service is in the gray release list.
     * If the list is empty, all services are considered enabled.
     *
     * @param serviceName the service name to check
     * @return true if the service should be instrumented
     */
    public boolean isServiceEnabled(String serviceName) {
        if (serviceList == null || serviceList.isEmpty()) {
            return true;
        }
        return serviceList.contains(serviceName);
    }

    /**
     * Check if a specific instance IP is in the gray release list.
     * If the list is empty, all instances are considered enabled.
     *
     * @param ip the instance IP to check
     * @return true if the instance should be instrumented
     */
    public boolean isInstanceIpEnabled(String ip) {
        if (instanceIps == null || instanceIps.isEmpty()) {
            return true;
        }
        return instanceIps.contains(ip);
    }

    /**
     * Check if a specific instance tag is in the gray release list.
     * If the list is empty, all tagged instances are considered enabled.
     *
     * @param tag the instance tag to check
     * @return true if the tag should be instrumented
     */
    public boolean isInstanceTagEnabled(String tag) {
        if (instanceTags == null || instanceTags.isEmpty()) {
            return true;
        }
        return instanceTags.contains(tag);
    }

    /**
     * Check if the current instance should be enabled based on ratio.
     *
     * @param instanceHash hash value of the instance identifier
     * @return true if the instance should be instrumented
     */
    public boolean isInstanceRatioEnabled(int instanceHash) {
        if (instanceRatio >= 1.0) {
            return true;
        }
        if (instanceRatio <= 0.0) {
            return false;
        }
        return Math.abs(instanceHash % 100) < instanceRatio * 100;
    }

    /**
     * Check if the current request should be sampled based on traffic ratio.
     *
     * @param requestHash hash value of the request identifier
     * @return true if the request should be traced
     */
    public boolean isTrafficRatioEnabled(int requestHash) {
        if (trafficRatio >= 1.0) {
            return true;
        }
        if (trafficRatio <= 0.0) {
            return false;
        }
        return Math.abs(requestHash % 100) < trafficRatio * 100;
    }

    // --- Getters and Setters ---

    public List<String> getServiceList() {
        return Collections.unmodifiableList(serviceList);
    }

    public void setServiceList(List<String> serviceList) {
        this.serviceList = serviceList != null ? new ArrayList<>(serviceList) : new ArrayList<>();
    }

    public void addService(String serviceName) {
        if (this.serviceList == null) {
            this.serviceList = new ArrayList<>();
        }
        this.serviceList.add(serviceName);
    }

    public double getInstanceRatio() {
        return instanceRatio;
    }

    public void setInstanceRatio(double instanceRatio) {
        this.instanceRatio = Math.max(0.0, Math.min(1.0, instanceRatio));
    }

    public List<String> getInstanceIps() {
        return Collections.unmodifiableList(instanceIps);
    }

    public void setInstanceIps(List<String> instanceIps) {
        this.instanceIps = instanceIps != null ? new ArrayList<>(instanceIps) : new ArrayList<>();
    }

    public void addInstanceIp(String ip) {
        if (this.instanceIps == null) {
            this.instanceIps = new ArrayList<>();
        }
        this.instanceIps.add(ip);
    }

    public List<String> getInstanceTags() {
        return Collections.unmodifiableList(instanceTags);
    }

    public void setInstanceTags(List<String> instanceTags) {
        this.instanceTags = instanceTags != null ? new ArrayList<>(instanceTags) : new ArrayList<>();
    }

    public void addInstanceTag(String tag) {
        if (this.instanceTags == null) {
            this.instanceTags = new ArrayList<>();
        }
        this.instanceTags.add(tag);
    }

    public double getTrafficRatio() {
        return trafficRatio;
    }

    public void setTrafficRatio(double trafficRatio) {
        this.trafficRatio = Math.max(0.0, Math.min(1.0, trafficRatio));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GrayConfig that = (GrayConfig) o;
        return Double.compare(that.instanceRatio, instanceRatio) == 0
                && Double.compare(that.trafficRatio, trafficRatio) == 0
                && Objects.equals(serviceList, that.serviceList)
                && Objects.equals(instanceIps, that.instanceIps)
                && Objects.equals(instanceTags, that.instanceTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceList, instanceRatio, instanceIps, instanceTags, trafficRatio);
    }

    @Override
    public String toString() {
        return "GrayConfig{"
                + "serviceList=" + serviceList
                + ", instanceRatio=" + instanceRatio
                + ", instanceIps=" + instanceIps
                + ", instanceTags=" + instanceTags
                + ", trafficRatio=" + trafficRatio
                + '}';
    }
}
