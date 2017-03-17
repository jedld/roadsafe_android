package client.roadsafe.org.roadsafe.models;

/**
 * Created by jedld on 3/17/17.
 */

public class RiskFactor {
    private double fatalRiskFactor;
    private double propertyRiskFactor;
    private double injuryRiskFactor;
    private int totalAccidents;
    private int fatalCount;
    private int injuryCount;
    private int propertyCount;

    public void setFatalRiskFactor(double fatalRiskFactor) {
        this.fatalRiskFactor = fatalRiskFactor;
    }

    public double getFatalRiskFactor() {
        return fatalRiskFactor;
    }

    public void setPropertyRiskFactor(double propertyRiskFactor) {
        this.propertyRiskFactor = propertyRiskFactor;
    }

    public double getPropertyRiskFactor() {
        return propertyRiskFactor;
    }

    public void setInjuryRiskFactor(double injuryRiskFactor) {
        this.injuryRiskFactor = injuryRiskFactor;
    }

    public double getInjuryRiskFactor() {
        return injuryRiskFactor;
    }

    public void setTotalAccidents(int totalAccidents) {
        this.totalAccidents = totalAccidents;
    }

    public int getTotalAccidents() {
        return totalAccidents;
    }

    public void setFatalCount(int fatalCount) {
        this.fatalCount = fatalCount;
    }

    public int getFatalCount() {
        return fatalCount;
    }

    public void setInjuryCount(int injuryCount) {
        this.injuryCount = injuryCount;
    }

    public int getInjuryCount() {
        return injuryCount;
    }

    public void setPropertyCount(int propertyCount) {
        this.propertyCount = propertyCount;
    }

    public int getPropertyCount() {
        return propertyCount;
    }
}
