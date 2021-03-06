package com.philips.lighting.quickstart;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.media.AudioFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.media.MediaRecorder;
import android.media.AudioRecord;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App.
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 *
 * @author SteveyO
 *
 */
public class MyApplicationActivity extends Activity {
    private PHHueSDK phHueSDK;
    public static final String TAG = "Party";

    //listen to the party
    public boolean listen = false;

    //set the sensitivity multiplier for the mic input
    public double sensitivity = 0.6;

    //public int audioSampleRate = 44100;
    public int audioSampleRate = 8000;
    public int audioChannels = AudioFormat.CHANNEL_IN_MONO;
    public int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //public int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    // min size plus a kb enough?
    public int audioBuffer = AudioRecord.getMinBufferSize(audioSampleRate, audioChannels, audioFormat);
    //public int audioBuffer = 2048;
    //trying a really small audio buffer for quicker loops
    //public int audioBuffer = 128;

    AudioRecord recorder = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            audioSampleRate,
            audioChannels,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBuffer
    );
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();

        Button partyButton;
        partyButton = (Button) findViewById(R.id.buttonParty);
        partyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listen = !listen;
                partyLights();
            }

        });

        Button stopButton;
        stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stopParty();
            }

        });

    }

    /**
     * Only good party colors
     */
    public void partyLights() {

        //setup hue connection
        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();

        recorder.startRecording();

        //read in the recording
        int read;
        byte data[] = new byte[audioBuffer];
        do{
            read = recorder.read(data, 0, audioBuffer);
            if(AudioRecord.ERROR_INVALID_OPERATION != read){

                //http://stackoverflow.com/questions/10324355/how-to-convert-16-bit-pcm-audio-byte-array-to-double-or-float-array
                short[] out = new short[data.length / 2]; // will drop last byte if odd number
                ByteBuffer bb = ByteBuffer.wrap(data);
                for (int i = 0; i < out.length; i++) {
                    out[i] = bb.getShort();
                }

                float[] pcmAsFloats = new float[out.length];
                for (int i = 0; i < out.length; i++) {
                    pcmAsFloats[i] = out[i];
                }

                //Log.w(TAG, "audio: " + Arrays.toString(pcmAsFloats));

                int lightCount = 0;
                for (PHLight light : allLights) {
                    PHLightState lightState = new PHLightState();

                    //http://www.developers.meethue.com/documentation/core-concepts
                    //lightState.setHue((int) pcmAsFloats[0]);
                    /*
                    Reducing the saturation takes this hue and moves it in a straight line towards
                    the white point. So "sat":25 always gives the most saturated colors and reducing
                    it to "sat":200 makes them less intense and more white.
                    */
                    //lightState.setSaturation(255);

                    //take the first samples
                    //convert the pcm data sample to a value between 1.000 and 0.000, we can clip to values outside
                    double x = (Math.abs(pcmAsFloats[lightCount++]) / 10000) * sensitivity;
                    double y = (Math.abs(pcmAsFloats[lightCount++]) / 10000) * sensitivity;

                    if(x > 1){
                        x = x / 10;
                    }
                    if(y > 1){
                        y = y / 10;
                    }

                    //instead of integer hue, using the x y value, see http://www.developers.meethue.com/documentation/supported-lights
                    //don't fall into the white areas of Gamut B
                    while ((y > 0.125 && (x < 0.6 || x > 0.2))){
                        x = Math.abs(pcmAsFloats[lightCount++]) / 10000;
                        y = Math.abs(pcmAsFloats[lightCount++]) / 10000;

                        if(x > 1){
                            x = x / 10;
                        }
                        if(y > 1){
                            y = y / 10;
                        }
                    }

                    Log.w(TAG, " x: " + x + " y: " + y);

                    //set 'em
                    lightState.setX((float) x, true);
                    lightState.setY((float) y, true);

                    // To validate your lightstate is valid (before sending to the bridge) you can use:
                    /*
                    String validState = lightState.validateState();

                    if(validState != null && !validState.isEmpty()) {
                        Log.w(TAG, validState);
                    }
                    else{
                        Log.w(TAG, "state is empty!");
                    }
                    */

                    bridge.updateLightState(light, lightState, listener);
                    //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
                }

            }
        }
        while(listen);

    }

    public void stopParty(){
        listen = false;

        recorder.stop();
        recorder.release();
    }

    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {
        
        @Override
        public void onSuccess() {  
        }
        
        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
           Log.w(TAG, "Light has updated");
        }
        
        @Override
        public void onError(int arg0, String arg1) {}

        @Override
        public void onReceivingLightDetails(PHLight arg0) {}

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {}

        @Override
        public void onSearchComplete() {}
    };
    
    @Override
    protected void onDestroy() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {
            
            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }
            
            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }
}
