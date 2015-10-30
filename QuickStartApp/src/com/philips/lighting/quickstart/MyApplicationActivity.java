package com.philips.lighting.quickstart;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private static final int MAX_HUE=65535;
    public static final String TAG = "QuickStart";

    //listen to the party
    public boolean listen = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();
        Button randomButton;
        randomButton = (Button) findViewById(R.id.buttonRand);
        randomButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listen = true;
                partyLights();
            }

        });


        Button listenButton;
        listenButton = (Button) findViewById(R.id.buttonRand);
        listenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listen = !listen;
                partyLights();
            }

        });

    }

    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        Random rand = new Random();
        
        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(rand.nextInt(MAX_HUE));
            // To validate your lightstate is valid (before sending to the bridge) you can use:  
            // String validState = lightState.validateState();
            bridge.updateLightState(light, lightState, listener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
    }

    /**
     * Only good party colors
     */
    public void partyLights() {

        //setup hue connection
        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();

        //int audioSampleRate = 44100;
        int audioSampleRate = 8000;
        int audioChannels = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        // min size plus a kb enough?
        int audioBuffer = AudioRecord.getMinBufferSize(audioSampleRate, audioChannels, audioFormat) + 1024;
        //int audioBuffer = 2048;

        //record some audio and set lights accordingly
        //http://developer.android.com/guide/topics/media/audio-capture.html
        /*
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.prepare();
        recorder.start();
        recorder.stop();
        recorder.release();
        */

        AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                audioSampleRate,
                audioChannels,
                AudioFormat.ENCODING_PCM_16BIT,
                audioBuffer
            );
        recorder.startRecording();

        //read in the recording
        int read;
        //byte[] data = new byte[] {};
        byte data[] = new byte[audioBuffer];
        //short sData[] = new short[1024];
        do{
            read = recorder.read(data, 0, audioBuffer);
            //recorder.read(sData, 0, 1024);
            if(AudioRecord.ERROR_INVALID_OPERATION != read){

                //http://stackoverflow.com/questions/10324355/how-to-convert-16-bit-pcm-audio-byte-array-to-double-or-float-array
                short[] out = new short[data.length / 2]; // will drop last byte if odd number
                ByteBuffer bb = ByteBuffer.wrap(data);
                for (int i = 0; i < out.length; i++) {
                    out[i] = bb.getShort();
                }

                float[] floaters = new float[out.length];
                for (int i = 0; i < out.length; i++) {
                    floaters[i] = out[i];
                }

                float[] pcmAsFloats = floaters;

                Log.w(TAG, "audio: " + Arrays.toString(pcmAsFloats));

                for (PHLight light : allLights) {
                    PHLightState lightState = new PHLightState();
                    lightState.setHue((int) pcmAsFloats[0]);
/*
                    //instead of integer hue, using the x y value, see http://www.developers.meethue.com/documentation/hue-xy-values
                    //to have vibrant colors we need a big diff between x and y
                    while (x - y < 0.25){
                        x = rand.nextFloat();
                        y = rand.nextFloat();
                    }

                    Log.w(TAG, " x: " + pcmAsFloats[0] + " y: " + pcmAsFloats[1]);

                    //set 'em
                    lightState.setX(x);
                    lightState.setY(y);

                    // To validate your lightstate is valid (before sending to the bridge) you can use:
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
