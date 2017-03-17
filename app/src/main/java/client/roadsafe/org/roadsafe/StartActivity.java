package client.roadsafe.org.roadsafe;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import client.roadsafe.org.roadsafe.utils.IntentConstants;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    private View startModePedestrianBtn;
    private View startModeMotorist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        startModePedestrianBtn = findViewById(R.id.start_mode_pedestrian);
        startModeMotorist = findViewById(R.id.start_mode_motorist);

        startModePedestrianBtn.setOnClickListener(this);
        startModeMotorist.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == startModePedestrianBtn) {
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(IntentConstants.MODE, IntentConstants.MODE_PEDESTRIAN);
            startActivity(intent);
        } else if (view == startModeMotorist) {
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(IntentConstants.MODE, IntentConstants.MODE_MOTORIST);
            startActivity(intent);
        }
    }
}
