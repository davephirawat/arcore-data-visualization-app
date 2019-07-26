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
import org.json.JSONObject;

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
    private final String test_mqtt = "test_mqtt";
    private final String test_json = "test_json";

    private boolean flag = true;

    String message;
    JSONObject messageJSON;
    JSONObject result;

    double leftValue;
    double midValue;
    double rightValue;
    double backValue;
    double footValue;



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



    }

    private void createCylinderModel() {

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderableLeft =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });


    }

    private void createArrowModel() {

        arrowRenderableLeft =
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
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.07f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            leftSensor = new Node();
                            leftSensor.setLocalPosition(new Vector3(-0.3f, 0.2f, 0));
                            leftSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            leftSensor.setParent(this);
                            leftSensor.setRenderable(viewRenderable);

                            //Set Text
                            leftSensor_textView = (TextView) viewRenderable.getView();
                        }
                );

        //The Number in the middle.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.07f))
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
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.07f))
                .build()
                .thenAccept(
                        viewRenderable -> {

                            rightSensor = new Node();
                            rightSensor.setLocalPosition(new Vector3(0.3f, 0.2f, 0));
                            rightSensor.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90));
                            rightSensor.setParent(this);
                            rightSensor.setRenderable(viewRenderable);

                            //Set Text
                            rightSensor_textView = (TextView) viewRenderable.getView();

                        }
                );

        //The Number on the Right side.
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.07f))
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

        //The Number on the Right side
        ViewRenderable.builder().setView(context, R.layout.popup).setSizer(new FixedHeightViewSizer(0.07f))
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

        Vector3 initialScale = new Vector3(0.04f,0.04f,0.1f);                         // Scale Y from 0.001f - 0.1f (100 scale)
        float initialY = 0.2f;
        float initialZ = -0.18f;

        polygonNodeLeft = new Node();
        polygonNodeLeft.setParent(this);
        polygonNodeLeft.setRenderable(polygonRenderableLeft);
        polygonNodeLeft.setLocalScale(initialScale);
        polygonNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));

        polygonNodeMid = new Node();
        polygonNodeMid.setParent(this);
        polygonNodeMid.setRenderable(polygonRenderableLeft);
        polygonNodeMid.setLocalScale(initialScale);
        polygonNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));

        polygonNodeRight = new Node();
        polygonNodeRight.setParent(this);
        polygonNodeRight.setRenderable(polygonRenderableLeft);
        polygonNodeRight.setLocalScale(initialScale);
        polygonNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));

        polygonNodeBack = new Node();
        polygonNodeBack.setParent(this);
        polygonNodeBack.setRenderable(polygonRenderableLeft);
        polygonNodeBack.setLocalScale(initialScale);
        polygonNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));

        polygonNodeFoot = new Node();
        polygonNodeFoot.setParent(this);
        polygonNodeFoot.setRenderable(polygonRenderableLeft);
        polygonNodeFoot.setLocalScale(initialScale);
        polygonNodeFoot.setLocalPosition(new Vector3(0, 0.35f, 0.3f));


    }

    private void visualiseCylinderGraph(){

        Quaternion rotateValue = Quaternion.axisAngle(new Vector3(1f, 0, 0), 90);    // Rotate 90 degree around X axis
        Vector3 initialScale = new Vector3(0.04f,0.1f,0.04f);                         // Scale Y from 0.001f - 0.1f (100 scale)
        float initialY = 0.2f;
        float initialZ = -0.18f;

        cylinderNodeLeft = new Node();
        cylinderNodeLeft.setParent(this);
        cylinderNodeLeft.setRenderable(cylinderRenderableLeft);
        cylinderNodeLeft.setLocalScale(initialScale);
        cylinderNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));
        cylinderNodeLeft.setLocalRotation(rotateValue);


        cylinderNodeMid = new Node();
        cylinderNodeMid.setParent(this);
        cylinderNodeMid.setRenderable(cylinderRenderableLeft);
        cylinderNodeMid.setLocalScale(initialScale);
        cylinderNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));
        cylinderNodeMid.setLocalRotation(rotateValue);


        cylinderNodeRight = new Node();
        cylinderNodeRight.setParent(this);
        cylinderNodeRight.setRenderable(cylinderRenderableLeft);
        cylinderNodeRight.setLocalScale(initialScale);
        cylinderNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));
        cylinderNodeRight.setLocalRotation(rotateValue);


        cylinderNodeBack = new Node();
        cylinderNodeBack.setParent(this);
        cylinderNodeBack.setRenderable(cylinderRenderableLeft);
        cylinderNodeBack.setLocalScale(initialScale);
        cylinderNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));
        cylinderNodeBack.setLocalRotation(rotateValue);



        cylinderNodeFoot = new Node();
        cylinderNodeFoot.setParent(this);
        cylinderNodeFoot.setRenderable(cylinderRenderableLeft);
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
        float initialZ = -0.23f;

        arrowNodeLeft = new Node();
        arrowNodeLeft.setParent(this);
        arrowNodeLeft.setRenderable(arrowRenderableLeft.getNow(null));
        arrowNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));
        arrowNodeLeft.setLocalScale(initialScale);
        arrowNodeLeft.setLocalRotation(rotateValue);


        arrowNodeMid = new Node();
        arrowNodeMid.setParent(this);
        arrowNodeMid.setRenderable(arrowRenderableLeft.getNow(null));
        arrowNodeMid.setLocalPosition(new Vector3(0, initialY, -0.2f));
        arrowNodeMid.setLocalScale(initialScale);
        arrowNodeMid.setLocalRotation(rotateValue);


        arrowNodeRight = new Node();
        arrowNodeRight.setParent(this);
        arrowNodeRight.setRenderable(arrowRenderableLeft.getNow(null));
        arrowNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));
        arrowNodeRight.setLocalScale(initialScale);
        arrowNodeRight.setLocalRotation(rotateValue);


        arrowNodeBack = new Node();
        arrowNodeBack.setParent(this);
        arrowNodeBack.setRenderable(arrowRenderableLeft.getNow(null));
        arrowNodeBack.setLocalPosition(new Vector3(0, 0, -0.35f));
        arrowNodeBack.setLocalScale(initialScale);
        arrowNodeBack.setLocalRotation(rotateValue);


        arrowNodeFoot = new Node();
        arrowNodeFoot.setParent(this);
        arrowNodeFoot.setRenderable(arrowRenderableLeft.getNow(null));
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

        float multiplier = 2.0f;
        polygonNodeLeft.setLocalScale(new Vector3(0.03f,0.03f,value * multiplier));
        cylinderNodeLeft.setLocalScale(new Vector3(0.03f,value * multiplier,0.03f));
        arrowNodeLeft.setLocalScale(new Vector3( 0.31f,value * multiplier * 4.0f,0.31f));

    }

    private void scaleAllMidGraph(float value) {

        polygonNodeMid.setLocalScale(new Vector3(0.03f,0.03f,value));
        cylinderNodeMid.setLocalScale(new Vector3(0.03f,value,0.03f));
        arrowNodeMid.setLocalScale(new Vector3( 0.31f,value * 4.0f,0.31f));

    }

    private void scaleAllRightGraph(float value) {

        float multiplier = 2.0f;
        polygonNodeRight.setLocalScale(new Vector3(0.03f,0.03f,value * multiplier));
        cylinderNodeRight.setLocalScale(new Vector3(0.03f,value * multiplier,0.03f));
        arrowNodeRight.setLocalScale(new Vector3( 0.31f,value * multiplier * 4.0f,0.31f));

    }

    private void scaleAllBackGraph(float value) {

        float multiplier = 2.0f;
        polygonNodeBack.setLocalScale(new Vector3(0.03f,0.03f,value));
        cylinderNodeBack.setLocalScale(new Vector3(0.03f,value,0.03f));
        arrowNodeBack.setLocalScale(new Vector3( 0.31f,value * 4.0f,0.31f));

    }

    private void scaleAllFootGraph(float value) {

        polygonNodeFoot.setLocalScale(new Vector3(0.03f,0.03f,value));
        cylinderNodeFoot.setLocalScale(new Vector3(0.03f,value,0.03f));
        arrowNodeFoot.setLocalScale(new Vector3( 0.31f,value * 4.0f,0.31f));

    }

    private void startMqtt() {

        if (flag){
            mqttHelper = new MqttHelper(context);
            mqttHelper.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean b, String s) {
                    Log.w(test_mqtt,"Connected! Successfully StartMqtt");

                }

                @Override
                public void connectionLost(Throwable throwable) {
                    Log.w(test_mqtt,"Connection Lost!");

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

//                    Log.w(test_mqtt, "Received: "+topic+" - "+mqttMessage.toString());

                    message = mqttMessage.toString().trim().replaceAll("'","\"");

                    Log.w(test_json, "Received JSON: "+message);

                    messageJSON = new JSONObject(message);
                    result = messageJSON.getJSONObject("value");

                    leftValue = result.getDouble("left");
                    midValue = result.getDouble("mid");
                    rightValue = result.getDouble("right");
                    backValue = result.getDouble("back");
                    footValue = result.getDouble("foot");

                    Log.w(test_json, "Received double: "+leftValue + ", " + midValue + ", " + rightValue + ", " + backValue + ", " + footValue);


                    leftSensor_textView.setText(String.valueOf(leftValue));
                    midSensor_textView.setText(String.valueOf(midValue));
                    rightSensor_textView.setText(String.valueOf(rightValue));
                    backSensor_textView.setText(String.valueOf(backValue));
                    footSensor_textView.setText(String.valueOf(footValue));

                    leftValue = roundValue(leftValue);
                    midValue = roundValue(midValue);
                    rightValue = roundValue(rightValue);
                    backValue = roundValue(backValue);
                    footValue = roundValue(footValue);

//                    Log.w(test_json, "After round and convert to float: "+leftValue + ", " + midValue + ", " + rightValue + ", " + backValue + ", " + footValue);


                    scaleAllLeftGraph((float) leftValue);
                    scaleAllMidGraph((float) midValue);
                    scaleAllRightGraph((float) rightValue);
                    scaleAllBackGraph((float) backValue);
                    scaleAllFootGraph((float) footValue);

                    /*float value = Float.valueOf(mqttMessage.toString().trim());

                    if ( value <= 0.0f) value = 0.001f;
                    else if ( value > 100.0f) value = 0.1f;
                    else value = value / 1000.0f;

                    Log.w(test_mqtt, "Received value to float: "+value);*/

                    /*if (topic.equals("test/left"))  {
                        leftSensor_textView.setText(message);
                        scaleAllLeftGraph(value);
                    }

                    if (topic.equals("test/mid"))  {
                        midSensor_textView.setText(message);
                        scaleAllMidGraph(value);
                    }

                    if (topic.equals("test/right"))  {
                        rightSensor_textView.setText(message);
                        scaleAllRightGraph(value);
                    }

                    if (topic.equals("test/back"))  {
                        backSensor_textView.setText(message);
                        scaleAllBackGraph(value);
                    }

                    if (topic.equals("test/foot"))  {
                        footSensor_textView.setText(message);
                        scaleAllFootGraph(value);
                    }*/
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    Log.w(test_mqtt,"Delivery Complete");
                }
            });

            flag = false;
        }


    }

    private double roundValue(double value){
        if ( value <= 0.0) {
            value = 0.001;
            return  value;

        }
        else if ( value > 100.0) {
            value = 0.1;
            return  value;

        }
        else {
            value = value / 1000.0;
            return  value;

        }
    }

    public AugmentedImage getImage() {
        return image;
    }

    public Node getPolygonNodeLeft() {
        return polygonNodeLeft;
    }
}

