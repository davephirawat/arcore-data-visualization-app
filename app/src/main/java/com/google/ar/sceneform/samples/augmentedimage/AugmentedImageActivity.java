/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ImageView fitToScanView;

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

    private Config config;
    private Session session;
    private AugmentedImageDatabase augmentedImageDatabase;
    private AugmentedImageDatabase augmentedImageDatabase2;
    private  boolean isTrackedFirstTime = true;
    private  boolean isTrackedSecondTime = false;
    private  boolean needToDisable = true;

    private static final String SAMPLE_IMAGE_DATABASE = "new_sample_database.imgdb";
    private static final String SAMPLE_IMAGE_DATABASE2 = "sample_database.imgdb";
    private static final String TAG = "AugmentedImageFragment";

    private ModelRenderable mazeRenderable;

    MqttHelper mqttHelper;
    final String logTagTest = "DaveDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (isTrackedFirstTime){
            enableImageDb();
           isTrackedFirstTime = false;
        }


        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);
        AugmentedImageNode node = new AugmentedImageNode(this);
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image " + augmentedImage.getIndex();
                    Log.w("TrackPic","Status: PAUSED");
                    Log.w("TrackPic","Tracking Method: "+augmentedImage.getTrackingMethod().toString());

                    SnackbarHelper.getInstance().showMessage(this, text);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    fitToScanView.setVisibility(View.GONE);
//                    Log.w("TrackPic","Tracking Method: "+augmentedImage.getTrackingMethod().toString());

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage) ) {

                        node.setImage(augmentedImage);
                        augmentedImageMap.put(augmentedImage, node);
                        arFragment.getArSceneView().getScene().addChild(node);
                    }
//                    disableImageDb();

                    break;

                case STOPPED:
//                    augmentedImageMap.remove(augmentedImage);
                    Log.w("TrackPic","Status: STOPPED");
                    Log.w("TrackPic","Tracking Method: "+augmentedImage.getTrackingMethod().toString());
                    break;
            }
        }
    }

    private void enableImageDb(){
        session = arFragment.getArSceneView().getSession();
        config = session.getConfig();
        try (InputStream is = this.getAssets().open(SAMPLE_IMAGE_DATABASE)) {
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image database.", e);
        }
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        session.configure(config);

    }

    private void disableImageDb(){
        if(needToDisable){
            session = arFragment.getArSceneView().getSession();
            config = session.getConfig();
            config.setAugmentedImageDatabase(null);
            session.configure(config);
            needToDisable = false;

            Log.w("TrackPic","Disable Imagedb");
        }
    }

    private void renderMaze(){
        ModelRenderable.builder()
                .setSource(this,R.raw.mazegreen)
                .build().thenAccept(renderable -> mazeRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load cat model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );
    }

    private void addAnchorFromNode(AugmentedImageNode node){


        AnchorNode anchorNode = new AnchorNode(node.getMazeAnchor());
        TransformableNode tempNode = new TransformableNode(arFragment.getTransformationSystem());
        tempNode.setParent(anchorNode);
        tempNode.setRenderable(mazeRenderable);
        tempNode.select();

    }

    public void onDestroy(){
        super.onDestroy();

        try {
            IMqttToken disconToken = mqttHelper.mqttAndroidClient.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(logTagTest,"Successfully Disconnected");
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.w(logTagTest,"Cannot disconnect");
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
