package client.roadsafe.org.roadsafe.models;

/**
 * Created by jedld on 3/17/17.
 */

public class Incident {
    private double longitude;
    private double latitude;
    private String severity;
    private String externelUUID;

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setExternelUUID(String externelUUID) {
        this.externelUUID = externelUUID;
    }

    public String getExternelUUID() {
        return externelUUID;
    }
}
