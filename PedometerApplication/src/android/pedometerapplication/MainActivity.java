package android.pedometerapplication;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Arrays;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	static float lowPassOut_Z;
	static float lowPassOut_Y;
	static float lowPassOut_X;
	static float magnitude;
	static double zMaxAccel = 0; // the highest acceleration in the z axis
	static double zMinAccel = 0; // the highest deceleration in the z axis
	static Button refresh; // reference to the refresh button
	static float averagedData; // average of range of data points
	static float previousAverage = 0.00f; // average data calculated previously
	static float[] oldData = { 0.00f, 0.00f, 0.00f };
	static int state = 0; // stores current state of step
	static int steps = 0; // records number of steps
	static TextView stepstv; //textview displaying number of steps
	static ArrayList<Float> dataPoints = new ArrayList<Float>(); // initializes
																	// arraylist
																	// for data
																	// points
	static TextView stateTV;

	static public float[] lowpass(float[] oldIn, float[] in) {

		float[] out = new float[in.length];
		final float alpha = 0.15f;

		for (int i = 0; i < oldIn.length; i++) {
			out[i] = in[i] + alpha * (oldIn[i] - in[i]);
		}

		//out[0] = 0;
		//out[1] = 0;
		lowPassOut_Z = out[2];
		lowPassOut_Y = out[1];
		lowPassOut_X = out[0];

		return out;
	}

	public static float averageData(ArrayList<Float> data) {
		float sum = 0;
		for (int i = 0; i < data.size(); i++)
			sum += data.get(i);
		return sum / data.size();
	}
	
	public static float pythagoras(float a, float b, float c) {
		float temp = 0.0f;
		
		temp = a*a + b*b + c*c;
		
		magnitude = Math.sqrt(temp);
		
		return magnitude;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false); // inflate
																						// layout
			LinearLayout rl = (LinearLayout) rootView.findViewById(R.id.rl);
			rl.setOrientation(LinearLayout.VERTICAL);

			final TextView zAccleration = new TextView(rootView.getContext());
			stepstv = new TextView(rootView.getContext());
			stepstv.setTextSize(20);
			stateTV = (TextView) rootView.findViewById(R.id.label1);
			refresh = (Button) rootView.findViewById(R.id.Refresh);
			refresh.setWidth(800);
			refresh.setOnClickListener(new View.OnClickListener() {
				@Override
			///*	public void onClick(View v) { // reset data on click
			//		float[] empty = { 0, 0, 0 };
			//		graph.addPoint(empty);
			//		zMaxAccel = 0;
			//		zMinAccel = 0;
			//		steps = 0;
			//		stepstv.setText("0");
			//	
			//	}
			//});*/

			//stateTV.setText("State:");
			zAccleration.setText("OVERRIDE");
			stepstv.setText("Steps: 0");

			rl.addView(zAccleration); // add textviews to layout
			rl.addView(stepstv);

			SensorManager sensorManager = (SensorManager) rootView.getContext().getSystemService(SENSOR_SERVICE); // request
																													// sensor
																													// manager
			Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); // get
																								// reference
																								// to
																								// acceleration
																								// sensor


			SensorEventListener a = new AccelerationEventListener(zAccleration, rootView.getContext());
			sensorManager.registerListener(a, accSensor, SensorManager.SENSOR_DELAY_FASTEST);

			return rootView;
		}
	}

	static public class AccelerationEventListener implements SensorEventListener {

		TextView output;
		Context context;

		public AccelerationEventListener(TextView outputView, Context mainContext) {
			output = outputView;
			context = mainContext;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				
				oldData = event.values;

				//output.setText("Acceleration(z);\nz: " + String.valueOf(event.values[2])
				//		+ "\nMaximum acceleration(z);\nz: " + zMaxAccel + "\nMinumum Acceleration(z): " + zMinAccel);
			}

			if (event.values[2] >= zMaxAccel) // update max and min values
				zMaxAccel = event.values[2];

			if (event.values[2] < zMinAccel)
				zMinAccel = event.values[2];

			dataPoints.add(lowPassOut);
			// dataPoints.add(event.values[2]); //add data points to arraylist
			// to be averaged

			if (dataPoints.size() > 4) { // begin when enough data is collected
				previousAverage = averagedData; // record previous average
				averagedData = averageData(dataPoints); // calculate new average
				refresh.setText("Reset");
				if (Math.abs(averagedData) > 13){
					state = -1;
					stateTV.setText("State: SHAKING");

				}
				switch (state) {
				case -1:{
					if (Math.abs(averagedData) < 1){
						state = 0;
						stateTV.setText("State: 0");
					}
				}
				case 0: {
					if (previousAverage > averagedData && averagedData > 2) {
						state = 1;
						stateTV.setText("State: 1");
					}
					break;
				}
				case 1: {
					if (previousAverage < averagedData && averagedData < 0) {
						state = 2;
						stateTV.setText("State: 2");
					}
					break;
				}
				case 2: {
					if (previousAverage < averagedData && averagedData < -0.5f) {
						state = 3;
						stateTV.setText("State: 3");
					}
					break;
				}
				case 3: {
					if (averagedData > 0 && averagedData > 0.5f) {
						state = 0;
						stateTV.setText("State: 0");
						steps++;
						stepstv.setText("Steps: " + String.valueOf(steps));
					}

					break;
				}
				}
				dataPoints.clear(); // clear data points
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

	}

}