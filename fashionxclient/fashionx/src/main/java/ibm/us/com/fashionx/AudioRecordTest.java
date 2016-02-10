package ibm.us.com.fashionx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechDelegate;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioRecordTest extends Activity implements OnClickListener {
    RecordAudio recordTask;
    Button startRecordingButton, stopRecordingButton;
    TextView statusText;

    boolean isRecording = false,isPlaying = false;

    int frequency = 11025,channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    String TAG = "STT";
    String mRecognitionResults = "";
    Handler mHandler = null;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) this.findViewById(R.id.StatusTextView);

        startRecordingButton = (Button) this
                .findViewById(R.id.StartRecordingButton);
        stopRecordingButton = (Button) this
                .findViewById(R.id.StopRecordingButton);


        startRecordingButton.setOnClickListener(this);
        stopRecordingButton.setOnClickListener(this);

        stopRecordingButton.setEnabled(false);

        /**
         File path = new File(
         Environment.getExternalStorageDirectory().getAbsolutePath()+"/AudioRecordTest.wav");

         try {
         path.createNewFile();
         //recordingFile = File.createTempFile("recording", ".pcm", path);
         } catch (IOException e) {
         throw new RuntimeException("Couldn't create file on SD card", e);
         }
         **/

        initSTT();
        mHandler = new Handler();
    }

    class SpeechDelegate implements ISpeechDelegate{
        // delegages ----------------------------------------------

        public void onOpen() {
            Log.d(TAG, "onOpen");
            displayStatus("successfully connected to the STT service");
            //setButtonLabel(R.id.buttonRecord, "Stop recording");
            //mState = ConnectionState.CONNECTED;
        }

        public void onError(String error) {

            Log.e(TAG, error);
            displayResult(error);
            //mState = ConnectionState.IDLE;
        }

        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG, "onClose, code: " + code + " reason: " + reason);
            displayStatus("connection closed");
            //setButtonLabel(R.id.buttonRecord, "Record");
            //mState = ConnectionState.IDLE;
        }

        public void onMessage(String message) {
            Log.d(TAG, "onMessage, message: " + message);
            try {
                JSONObject jObj = new JSONObject(message);
                // state message
                if(jObj.has("state")) {
                    Log.d(TAG, "Status message: " + jObj.getString("state"));
                }
                // results message
                else if (jObj.has("results")) {
                    //if has result
                    Log.d(TAG, "Results message: ");
                    JSONArray jArr = jObj.getJSONArray("results");
                    for (int i=0; i < jArr.length(); i++) {
                        JSONObject obj = jArr.getJSONObject(i);
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        String str = jArr1.getJSONObject(0).getString("transcript");
                        // remove whitespaces if the language requires it

                        String strFormatted = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                        if (obj.getString("final").equals("true")) {
                            mRecognitionResults += strFormatted.substring(0,strFormatted.length()-1);
                            displayResult(mRecognitionResults);
                        } else {
                            displayResult(mRecognitionResults + strFormatted);
                        }
                        break;
                    }
                } else {
                    displayResult("unexpected data coming from stt server: \n" + message);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON");
                e.printStackTrace();
            }
        }
        public void onAmplitude(double amplitude, double volume) {
            //Logger.e(TAG, "amplitude=" + amplitude + ", volume=" + volume);
        }
    }

    public void onClick(View v) {
        if (v == startRecordingButton) {
            record();
        } else if (v == stopRecordingButton) {
            stopRecording();
        }
    }

    public void record() {
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);
        recordTask = new RecordAudio();
        recordTask.execute();
    }


    public void stopRecording() {
        isRecording = false;
        startRecordingButton.setEnabled(true);
        stopRecordingButton.setEnabled(false);
        SpeechToText.sharedInstance().stopRecognition();
    }

    private class RecordAudio extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isRecording = true;
            SpeechToText.sharedInstance().recognize();
            return null;
        }
    }

    public void displayResult(final String result) {
        final Runnable runnableUi = new Runnable(){
            @Override
            public void run() {
                TextView textResult = (TextView)findViewById(R.id.textView);
                textResult.setText(result);
            }
        };

        new Thread(){
            public void run(){
                mHandler.post(runnableUi);
            }
        }.start();
    }


    public void displayStatus(final String status){
        new Thread(){
            public void run(){
                Log.d(TAG, "Status: " + status);
            }
        }.start();

    }

    public boolean initSTT(){
        try {

            URI uri = new URI("wss://stream.watsonplatform.net/speech-to-text/api");
            SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);
            SpeechToText.sharedInstance().initWithContext(uri,getApplicationContext(),sConfig);

        }
        catch(URISyntaxException e){
            Log.e("URISyntaxException", e.getMessage());
        }

        String username = "7adcf764-e02d-4370-86fd-e03fa2bcfe1b";
        String pwd = "f0Q8tmqpXLkF";

        SpeechDelegate delegate = new SpeechDelegate();
        SpeechToText.sharedInstance().setCredentials(username, pwd);
        SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
        SpeechToText.sharedInstance().setDelegate(delegate);

        new AsyncTask<Void,Void,JSONObject>(){
            @Override
            protected JSONObject doInBackground(Void... params) {
                return SpeechToText.sharedInstance().getModels();
            }
        }.execute();

        return true;
    }

}
