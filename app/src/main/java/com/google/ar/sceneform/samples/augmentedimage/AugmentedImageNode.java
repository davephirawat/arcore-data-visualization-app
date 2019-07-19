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
import android.widget.TextView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.FixedHeightViewSizer;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

    // Add a member variable to hold the maze model.
     Node polygonNodeLeft;
     Node polygonNodeMid;
     Node polygonNodeRight;
     Node polygonNodeBack;
     Node polygonNodeFoot;

     Node cylinderNodeLeft;
     Node cylinderNodeMid;
     Node cylinderNodeRight;
     Node cylinderNodeBack;
     Node cylinderNodeFoot;

     Node arrowNodeLeft;
     Node arrowNodeMid;
     Node arrowNodeRight;
     Node arrowNodeBack;
     Node arrowNodeFoot;

    private Node leftSensor;
    private Node midSensor;
    private Node rightSensor;
    private Node backSensor;
    private Node footSensor;

    TextView leftSensor_textView;
    TextView midSensor_textView;
    TextView rightSensor_textView;
    TextView backSensor_textView;
    TextView footSensor_textView;

    // Add a variable called polygonRenderableLeft for use with loading
    // GreenMaze.sfb.
    private ModelRenderable polygonRenderableLeft;
    private ModelRenderable polygonRenderableMid;
    private ModelRenderable polygonRenderableRight;
    private ModelRenderable polygonRenderableBack;
    private ModelRenderable polygonRenderableFoot;

    private ModelRenderable cylinderRenderableLeft;
    private ModelRenderable cylinderRenderableMid;
    private ModelRenderable cylinderRenderableRight;
    private ModelRenderable cylinderRenderableBack;
    private ModelRenderable cylinderRenderableFoot;

    private CompletableFuture<ModelRenderable> arrowRenderableLeft;
    private CompletableFuture<ModelRenderable> arrowRenderableMid;
    private CompletableFuture<ModelRenderable> arrowRenderableRight;
    private CompletableFuture<ModelRenderable> arrowRenderableBack;
    private CompletableFuture<ModelRenderable> arrowRenderableFoot;

    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "AugmentedImageNode";

    // The augmented image represented by this node.
    private AugmentedImage image;

    private Context context;
    MqttHelper mqttHelper;
    private final String logTagTest = "test_mqtt";

    private boolean flag = true;



    public AugmentedImageNode(Context context) {
        this.context = context; // Saving context fot MQTT part

        createPolygonModel();
        createCylinderModel();
        createArrowModel();
    }

    private void createPolygonModel() {

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderableLeft =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderableMid =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });


        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderableRight =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderableBack =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderableFoot =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });

    }

    private void createCylinderModel() {

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableLeft =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableMid =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });


        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableRight =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableBack =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableFoot =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });

    }

    private void createArrowModel() {

        arrowRenderableLeft =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("arrow.sfb"))
                        .build();

        arrowRenderableMid =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("arrow.sfb"))
                        .build();

        arrowRenderableRight =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("arrow.sfb"))
                        .build();

        arrowRenderableBack =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("arrow.sfb"))
                        .build();

        arrowRenderableFoot =
                ModelRenderable.builder()
                        .setSource(context, Uri.parse("arrow.sfb"))
                        .build();

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

        // Initialize polygonNodeLeft and set its parents and the Renderable.
        // If any of the models are not loaded, process this function
        // until they all are loaded.

        if (!arrowRenderableLeft.isDone()) {
            CompletableFuture.allOf(arrowRenderableLeft)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (!arrowRenderableMid.isDone()) {
            CompletableFuture.allOf(arrowRenderableMid)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }
/*
        if (!arrowRenderableRight.isDone()) {
            CompletableFuture.allOf(arrowRenderableRight)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (!arrowRenderableBack.isDone()) {
            CompletableFuture.allOf(arrowRenderableBack)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (!arrowRenderableFoot.isDone()) {
            CompletableFuture.allOf(arrowRenderableFoot)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }*/


        // Set the anchor based on the center of the image.
        setAnchor(image.createAnchor(image.getCenterPose()));


        visualisePolygonGraph();

        visualiseCylinderGraph();

        visualiseArrowGraph();

        //This method render and display 3 Numbers which is measured value of Force from the sensors
        visualiseNumbers();


        startMqtt();

    }

    private void visualiseNumbers() {

        //The Number on the Left side.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.03f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            leftSensor = new Node();
                            leftSensor.setLocalPosition(new Vector3(-0.2f, 0.2f, -0.15f));
                            leftSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            leftSensor.setParent(this);
                            leftSensor.setRenderable(viewRenderable);

                            //Set Text
                            leftSensor_textView = (TextView) viewRenderable.getView();
                        }
                );

        //The Number in the middle.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.03f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            midSensor = new Node();
                            midSensor.setLocalPosition(new Vector3(0, 0.2f, -0.05f));
                            midSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            midSensor.setParent(this);
                            midSensor.setRenderable(viewRenderable);

                            //Set Text
                            midSensor_textView = (TextView) viewRenderable.getView();

                        }
                );

        //The Number on the Right side.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.03f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            rightSensor = new Node();
                            rightSensor.setLocalPosition(new Vector3(0.2f, 0.2f, -0.15f));
                            rightSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            rightSensor.setParent(this);
                            rightSensor.setRenderable(viewRenderable);

                            //Set Text
                            rightSensor_textView = (TextView) viewRenderable.getView();

                        }
                );

        //The Number on the Right side.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.03f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            backSensor = new Node();
                            backSensor.setLocalPosition(new Vector3(0, 0, -0.2f));
                            backSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            backSensor.setParent(this);
                            backSensor.setRenderable(viewRenderable);

                            //Set Text
                            backSensor_textView = (TextView) viewRenderable.getView();

                        }
                );

        //The Number on the Right side.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.03f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            footSensor = new Node();
                            footSensor.setLocalPosition(new Vector3(0, 0.35f, 0.40f));
                            footSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            footSensor.setParent(this);
                            footSensor.setRenderable(viewRenderable);

                            //Set Text
                            footSensor_textView = (TextView) viewRenderable.getView();

                        }
                );

    }

    private void visualisePolygonGraph(){

        Vector3 initialScale = new Vector3(0.01f,0.01f,0.1f);                         // Scale Y from 0.001f - 0.1f (100 scale)
        float initialY = 0.2f;
        float initialZ = -0.25f;

        polygonNodeLeft = new Node();
        polygonNodeLeft.setParent(this);
        polygonNodeLeft.setRenderable(polygonRenderableLeft);
        polygonNodeLeft.setLocalScale(initialScale);
        polygonNodeLeft.setLocalPosition(new Vector3(-0.2f, initialY, initialZ));

        polygonNodeMid = new Node();
        polygonNodeMid.setParent(this);
        polygonNodeMid.setRenderable(polygonRenderableMid);
        polygonNodeMid.setLocalScale(initialScale);
        polygonNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));

        polygonNodeRight = new Node();
        polygonNodeRight.setParent(this);
        polygonNodeRight.setRenderable(polygonRenderableRight);
        polygonNodeRight.setLocalScale(initialScale);
        polygonNodeRight.setLocalPosition(new Vector3(0.2f, initialY, initialZ));

        polygonNodeBack = new Node();
        polygonNodeBack.setParent(this);
        polygonNodeBack.setRenderable(polygonRenderableBack);
        polygonNodeBack.setLocalScale(initialScale);
        polygonNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));

        polygonNodeFoot = new Node();
        polygonNodeFoot.setParent(this);
        polygonNodeFoot.setRenderable(polygonRenderableMid);
        polygonNodeFoot.setLocalScale(initialScale);
        polygonNodeFoot.setLocalPosition(new Vector3(0, 0.35f, 0.3f));


    }

    private void visualiseCylinderGraph(){

        Quaternion rotateValue = Quaternion.axisAngle(new Vector3(1f, 0, 0), 90);    // Rotate 90 degree around X axis
        Vector3 initialScale = new Vector3(0.01f,0.1f,0.01f);                         // Scale Y from 0.001f - 0.1f (100 scale)
        float initialY = 0.2f;
        float initialZ = -0.25f;

        cylinderNodeLeft = new Node();
        cylinderNodeLeft.setParent(this);
        cylinderNodeLeft.setRenderable(cylinderRenderableLeft);
        cylinderNodeLeft.setLocalScale(initialScale);
        cylinderNodeLeft.setLocalPosition(new Vector3(-0.2f, initialY, initialZ));
        cylinderNodeLeft.setLocalRotation(rotateValue);


        cylinderNodeMid = new Node();
        cylinderNodeMid.setParent(this);
        cylinderNodeMid.setRenderable(cylinderRenderableMid);
        cylinderNodeMid.setLocalScale(initialScale);
        cylinderNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));
        cylinderNodeMid.setLocalRotation(rotateValue);


        cylinderNodeRight = new Node();
        cylinderNodeRight.setParent(this);
        cylinderNodeRight.setRenderable(cylinderRenderableRight);
        cylinderNodeRight.setLocalScale(initialScale);
        cylinderNodeRight.setLocalPosition(new Vector3(0.2f, initialY, initialZ));
        cylinderNodeRight.setLocalRotation(rotateValue);


        cylinderNodeBack = new Node();
        cylinderNodeBack.setParent(this);
        cylinderNodeBack.setRenderable(cylinderRenderableBack);
        cylinderNodeBack.setLocalScale(initialScale);
        cylinderNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));
        cylinderNodeBack.setLocalRotation(rotateValue);



        cylinderNodeFoot = new Node();
        cylinderNodeFoot.setParent(this);
        cylinderNodeFoot.setRenderable(cylinderRenderableMid);
        cylinderNodeFoot.setLocalScale(initialScale);
        cylinderNodeFoot.setLocalPosition(new Vector3(0, 0.35f, 0.3f));
        cylinderNodeFoot.setLocalRotation(rotateValue);


        //Show only Polygon graphs first
        hideCylinderGraph(true);
    }

    private void visualiseArrowGraph(){

        Quaternion rotateValue = Quaternion.axisAngle(new Vector3(1f, 0, 0), 90);    // Rotate 90 degree around X axis
        Vector3 initialScale = new Vector3( 0.28f,0.4f,0.28f);                        // Scale Y from 0.004f - 0.4f (100 scale, 0.04/scale)
        float initialY = 0.2f;
        float initialZ = -0.30f;

        arrowNodeLeft = new Node();
        arrowNodeLeft.setParent(this);
        arrowNodeLeft.setRenderable(arrowRenderableMid.getNow(null));
        arrowNodeLeft.setLocalPosition(new Vector3(-0.2f, initialY, initialZ));
        arrowNodeLeft.setLocalScale(initialScale);
        arrowNodeLeft.setLocalRotation(rotateValue);


        arrowNodeMid = new Node();
        arrowNodeMid.setParent(this);
        arrowNodeMid.setRenderable(arrowRenderableMid.getNow(null));
        arrowNodeMid.setLocalPosition(new Vector3(0, initialY, -0.2f));
        arrowNodeMid.setLocalScale(initialScale);
        arrowNodeMid.setLocalRotation(rotateValue);


        arrowNodeRight = new Node();
        arrowNodeRight.setParent(this);
        arrowNodeRight.setRenderable(arrowRenderableMid.getNow(null));
        arrowNodeRight.setLocalPosition(new Vector3(0.2f, initialY, initialZ));
        arrowNodeRight.setLocalScale(initialScale);
        arrowNodeRight.setLocalRotation(rotateValue);


        arrowNodeBack = new Node();
        arrowNodeBack.setParent(this);
        arrowNodeBack.setRenderable(arrowRenderableMid.getNow(null));
        arrowNodeBack.setLocalPosition(new Vector3(0, 0, -0.35f));
        arrowNodeBack.setLocalScale(initialScale);
        arrowNodeBack.setLocalRotation(rotateValue);


        arrowNodeFoot = new Node();
        arrowNodeFoot.setParent(this);
        arrowNodeFoot.setRenderable(arrowRenderableMid.getNow(null));
        arrowNodeFoot.setLocalPosition(new Vector3(0, 0.35f, 0.25f));
        arrowNodeFoot.setLocalScale(initialScale);
        arrowNodeFoot.setLocalRotation(rotateValue);


        //Show only Polygon graphs first
        hideArrowGraph(true);
    }


    public void hidePolygonGraph(Boolean hide){

        if (hide){
            polygonNodeLeft.setEnabled(false);
            polygonNodeMid.setEnabled(false);
            polygonNodeRight.setEnabled(false);
            polygonNodeBack.setEnabled(false);
            polygonNodeFoot.setEnabled(false);
        } else {
            polygonNodeLeft.setEnabled(true);
            polygonNodeMid.setEnabled(true);
            polygonNodeRight.setEnabled(true);
            polygonNodeBack.setEnabled(true);
            polygonNodeFoot.setEnabled(true);
        }

    }

    public void hideCylinderGraph(Boolean hide){

        if (hide){
            cylinderNodeLeft.setEnabled(false);
            cylinderNodeMid.setEnabled(false);
            cylinderNodeRight.setEnabled(false);
            cylinderNodeBack.setEnabled(false);
            cylinderNodeFoot.setEnabled(false);

        } else {
            cylinderNodeLeft.setEnabled(true);
            cylinderNodeMid.setEnabled(true);
            cylinderNodeRight.setEnabled(true);
            cylinderNodeBack.setEnabled(true);
            cylinderNodeFoot.setEnabled(true);
        }

    }

    public void hideArrowGraph(Boolean hide){

        if (hide){
            arrowNodeLeft.setEnabled(false);
            arrowNodeMid.setEnabled(false);
            arrowNodeRight.setEnabled(false);
            arrowNodeBack.setEnabled(false);
            arrowNodeFoot.setEnabled(false);
        } else {
            arrowNodeLeft.setEnabled(true);
            arrowNodeMid.setEnabled(true);
            arrowNodeRight.setEnabled(true);
            arrowNodeBack.setEnabled(true);
            arrowNodeFoot.setEnabled(true);
        }

    }


    private void scaleAllLeftGraph(float value) {

        polygonNodeLeft.setLocalScale(new Vector3(0.01f,0.01f,value));
        cylinderNodeLeft.setLocalScale(new Vector3(0.01f,value,0.01f));
        arrowNodeLeft.setLocalScale(new Vector3( 0.28f,value * 4.0f,0.28f));

    }

    private void scaleAllMidGraph(float value) {

        polygonNodeMid.setLocalScale(new Vector3(0.01f,0.01f,value));
        cylinderNodeMid.setLocalScale(new Vector3(0.01f,value,0.01f));
        arrowNodeMid.setLocalScale(new Vector3( 0.28f,value * 4.0f,0.28f));

    }

    private void scaleAllRightGraph(float value) {

        polygonNodeRight.setLocalScale(new Vector3(0.01f,0.01f,value));
        cylinderNodeRight.setLocalScale(new Vector3(0.01f,value,0.01f));
        arrowNodeRight.setLocalScale(new Vector3( 0.28f,value * 4.0f,0.28f));

    }

    private void scaleAllBackGraph(float value) {

        polygonNodeBack.setLocalScale(new Vector3(0.01f,0.01f,value));
        cylinderNodeBack.setLocalScale(new Vector3(0.01f,value,0.01f));
        arrowNodeBack.setLocalScale(new Vector3( 0.28f,value * 4.0f,0.28f));

    }

    private void scaleAllFootGraph(float value) {

        polygonNodeFoot.setLocalScale(new Vector3(0.01f,0.01f,value));
        cylinderNodeFoot.setLocalScale(new Vector3(0.01f,value,0.01f));
        arrowNodeFoot.setLocalScale(new Vector3( 0.28f,value * 4.0f,0.28f));

    }

    private void startMqtt() {

        if (flag){
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
                    Log.w(logTagTest, "Received: "+topic+" - "+mqttMessage.toString());

                    float value = Float.valueOf(mqttMessage.toString().trim());

                    if ( value <= 0.0f) value = 0.001f;
                    else if ( value > 100.0f) value = 0.1f;
                    else value = value / 1000.0f;

                    Log.w(logTagTest, "Received value to float: "+value);

                    if (topic.equals("test/left"))  {
                        leftSensor_textView.setText(mqttMessage.toString().trim());
                        scaleAllLeftGraph(value);
                    }

                    if (topic.equals("test/mid"))  {
                        midSensor_textView.setText(mqttMessage.toString().trim());
                        scaleAllMidGraph(value);
                    }

                    if (topic.equals("test/right"))  {
                        rightSensor_textView.setText(mqttMessage.toString().trim());
                        scaleAllRightGraph(value);
                    }

                    if (topic.equals("test/back"))  {
                        backSensor_textView.setText(mqttMessage.toString().trim());
                        scaleAllBackGraph(value);
                    }

                    if (topic.equals("test/foot"))  {
                        footSensor_textView.setText(mqttMessage.toString().trim());
                        scaleAllFootGraph(value);
                    }
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    Log.w(logTagTest,"Delivery Complete");
                }
            });

            flag = false;
        }


    }

    public AugmentedImage getImage() {
        return image;
    }

    public Node getPolygonNodeLeft() {
        return polygonNodeLeft;
    }
}

