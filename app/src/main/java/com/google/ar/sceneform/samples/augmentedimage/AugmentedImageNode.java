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

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

    // Add a member variable to hold the maze model.
    private Node mazeNode;
    private Anchor anchor;
    // Add a variable called mazeRenderable for use with loading
    // GreenMaze.sfb.
    private CompletableFuture<ModelRenderable> mazeRenderable;

    private float maze_scale = 0.0f;

    private ModelRenderable ballRenderable;

    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "AugmentedImageNode";

    // The augmented image represented by this node.
    private AugmentedImage image;

    private Context context;
    TextView popup_textView;
    MqttHelper mqttHelper;
    final String logTagTest = "mqttdave";

    private boolean flag = true;



    public AugmentedImageNode(Context context) {
        mazeRenderable =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("GreenMaze.sfb"))
                        .build();


        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            ballRenderable =
                                    ShapeFactory.makeSphere(0.01f, new Vector3(0, 0, 0), material); });

        this.context = context;

    }

    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image. The corners are then positioned based on the
     * extents of the image. There is no need to worry about world coordinates since everything is
     * relative to the center of the image, which is the parent node of the corners.
     */
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image) {
        this.image = image;

        // Initialize mazeNode and set its parents and the Renderable.
        // If any of the models are not loaded, process this function
        // until they all are loaded.
        if (!mazeRenderable.isDone()) {
            CompletableFuture.allOf(mazeRenderable)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        // Set the anchor based on the center of the image.
        setAnchor(image.createAnchor(image.getCenterPose()));

        /*mazeNode = new Node();
        mazeNode.setParent(this);
        mazeNode.setRenderable(mazeRenderable.getNow(null));*/
        anchor = image.createAnchor(image.getCenterPose());


        // Make sure the longest edge fits inside the image.
        final float maze_edge_size = 492.65f;
        final float max_image_edge = Math.max(image.getExtentX(), image.getExtentZ());
        maze_scale = max_image_edge / maze_edge_size;

        // Scale Y an extra 10 times to lower the maze wall.
//        mazeNode.setLocalScale(new Vector3(maze_scale, maze_scale * 0.1f, maze_scale));


        /*// Add the ball at the end of the setImage function.
        Node ballNode = new Node();
        ballNode.setParent(this);
        ballNode.setRenderable(ballRenderable);
        ballNode.setLocalPosition(new Vector3(0.1f, 0.1f, 0));*/


        ViewRenderable.builder().setView(context, R.layout.popup)
                .build()
                .thenAccept(
                        viewRenderable -> {

                            Node popup = new Node();
                            popup.setLocalPosition(new Vector3( 0,  0, 0));
                            popup.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            popup.setParent(this);
                            popup.setRenderable(viewRenderable);
//                            popup.setLocalScale(new Vector3(maze_scale, maze_scale * 0.1f, maze_scale));

                            //Set Text
                            popup_textView = (TextView) viewRenderable.getView();
                            Log.w("TrackPic","popup");


                            //Click on TextView to remove animal
                            /*popup_textView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    anchorNode.setParent(null);
                                }
                            });*/

                        }
                );

        if(flag){
            startMqtt();
            flag = false;
        }



    }

    public AugmentedImage getImage() {
        return image;
    }

    public Node getMazeNode() {
        return mazeNode;
    }

    public Anchor getMazeAnchor(){
        return anchor;
    }

    private void startMqtt() {
        mqttHelper = new MqttHelper(context);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w(logTagTest,"Connected! Successfully StartMqtt");

            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w(logTagTest,"Connection Lost!");

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w(logTagTest, mqttMessage.toString());
                popup_textView.setText(mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.w(logTagTest,"Delivery Complete");
            }
        });
    }

    /*public void onDestroy(){
        co.onDestroy();

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
    }*/
}