package client.roadsafe.org.roadsafe.client;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import client.roadsafe.org.roadsafe.models.Incident;
import client.roadsafe.org.roadsafe.models.RiskFactor;
import okhttp3.OkHttpClient;

/**
 * Created by jedld on 3/17/17.
 */

public class RoadSafeServiceClient {
    private static final String API_URL = "https://localhost:3000";
    private static final String TAG = RoadSafeServiceClient.class.getName();

    private final OkHttpClient client;

    public RoadSafeServiceClient()
    {
       this.client = new OkHttpClient();
    }


    public ArrayList<Incident> getIncidentReports(double lng, double lat, float radius, int timeOfDay) {
        ArrayList<Incident> responseList = new ArrayList<Incident>();
        String url;
        if (timeOfDay < 0) {
            url = API_URL + "/query?lng="+lng+"&lat=" + lat +"&radius=" + radius;
        } else {
            url = API_URL + "/query?lng=" + lng + "&lat=" + lat + "&radius=" + radius + "&hour=" + timeOfDay;
        }
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        okhttp3.Response response = null;
        String responseBody = null;
        try {
            response = client.newCall(request).execute();
            responseBody = response.body().string();
            Log.d(TAG, "response " + responseBody);
            Log.e(TAG, "response code " + response.code());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response!=null && response.code() == 200) {
            try {
                JSONObject jsonResult =  new JSONObject(responseBody);
                JSONArray incidentResults = jsonResult.getJSONArray("result");
                for (int i = 0; i < incidentResults.length(); i++) {
                    JSONObject incidentJsonObj =  incidentResults.getJSONObject(i);
                    Incident incident = new Incident();
                    incident.setLongitude(incidentJsonObj.getDouble("lng"));
                    incident.setLatitude(incidentJsonObj.getDouble("lat"));
                    incident.setSeverity(incidentJsonObj.getString("severity"));
                    incident.setExternelUUID(incidentJsonObj.getString("external_uuid"));
                    responseList.add(incident);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return responseList;
    }

    public RiskFactor getRiskFactor(double lng, double lat, float radius, int timeOfDay) {
        Log.d(TAG, "start call getRiskFactor");
        String url;
        if (timeOfDay < 0) {
           url = API_URL + "/stats?lng="+lng+"&lat=" + lat +"&radius=" + radius;
        } else {
           url = API_URL + "/stats?lng=" + lng + "&lat=" + lat + "&radius=" + radius + "&hour=" + timeOfDay;
        }
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        okhttp3.Response response = null;
        String responseBody = null;
        try {
            response = client.newCall(request).execute();
            responseBody = response.body().string();
            Log.d(TAG, "response " + responseBody);
            Log.e(TAG, "response code " + response.code());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response!=null && response.code() == 200) {
            JSONObject jsonResult = null;
            try {
                jsonResult = new JSONObject(responseBody);

                RiskFactor riskFactor = new RiskFactor();
                riskFactor.setFatalRiskFactor(jsonResult.getDouble("fatal_risk_factor"));
                riskFactor.setPropertyRiskFactor(jsonResult.getDouble("property_risk_factor"));
                riskFactor.setInjuryRiskFactor(jsonResult.getDouble("injury_risk_factor"));
                riskFactor.setTotalAccidents(jsonResult.getInt("total"));
                riskFactor.setFatalCount(jsonResult.getInt("fatal"));
                riskFactor.setInjuryCount(jsonResult.getInt("injury"));
                riskFactor.setPropertyCount(jsonResult.getInt("property"));

                return riskFactor;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
