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

@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

    /**
     * REMEMBER THIS NAMING
     * There are 5 parts of sensors which are named from the perspective where you look in the direction
     * toward to the chair NOT from the patient who sit on the chair perspective.
     */

    // All of these Node attributes will be added as a child node into the AR Scene
    // each node can be set its ModelRenderable ( 3D model, arrow, bar, cylinder, etc.),
    // ViewRenderable (2D UI of Android to be display in real world, like TextView in this app),
    // scale, rotation, position and etc. like a GameObject in Unity.
    private Node polygonNodeLeft;
    private Node polygonNodeMid;
    private Node polygonNodeRight;
    private Node polygonNodeBack;
    private Node polygonNodeFoot;

    private Node cylinderNodeLeft;
    private Node cylinderNodeMid;
    private Node cylinderNodeRight;
    private Node cylinderNodeBack;
    private Node cylinderNodeFoot;

    private Node arrowNodeLeft;
    private Node arrowNodeMid;
    private Node arrowNodeRight;
    private Node arrowNodeBack;
    private Node arrowNodeFoot;

    private Node leftSensor;
    private Node midSensor;
    private Node rightSensor;
    private Node backSensor;
    private Node footSensor;


    //TextView is an Android View (like a UI for display texts on the screen) which in this case will be
    //displayed on the real world for each value read from force sensors.
    private TextView leftSensor_textView;
    private TextView midSensor_textView;
    private TextView rightSensor_textView;
    private TextView backSensor_textView;
    private TextView footSensor_textView;


    // Each object is a 2D or 3D object can be built and rendered which will later be added into node
    // to be displayed on the screen
    private ModelRenderable polygonRenderable;
    private ModelRenderable cylinderRenderable;
    private CompletableFuture<ModelRenderable> arrowRenderable;

    //////////////////////////////////////////////////////////////////////////////////////////////

    // Just a tag for searching while debugging
    private static final String TAG = "AugmentedImageNode";

    // The augmented image represented by this node.
    private AugmentedImage image;

    // These attributes are used for MQTT part contains:
    // context is for mqtthelper to know which Activity are running right now
    // mqttHelper is for establishing connection to mqtt server.
    // two Strings is for debugging, to see if connected or fail, to see if json received are correct or not
    private Context context;
    MqttHelper mqttHelper;
    private final String test_mqtt = "test_mqtt";
    private final String test_json = "test_json";

    // To make mqttHelper establish connection only once
    private boolean flag = true;

    // message received from Mqtt and converted into JSON and the extracted JSON
    private String message;
    private JSONObject messageJSON;
    private JSONObject result;

    // Using double for scaling the graph
    private double leftValue;
    private double midValue;
    private double rightValue;
    private double backValue;
    private double footValue;


    // After the constructor are called, create each type of model
    public AugmentedImageNode(Context context) {
        this.context = context; // Saving context fot MQTT part
        createPolygonModel();
        createCylinderModel();
        createArrowModel();
    }

    // For the create and render each type of model
    private void createPolygonModel() {

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            polygonRenderable =
                                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), new Vector3(0, 0, 0), material); });

    }

    private void createCylinderModel() {

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.LTGRAY))
                .thenAccept(
                        material -> {
                            cylinderRenderable =
                                    ShapeFactory.makeCylinder(0.7f,1f, new Vector3(0, 0, 0), material); });

    }

    private void createArrowModel() {

        arrowRenderable =
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

        // If any of the models are not loaded, process this function
        // until they all are loaded.

        if (!arrowRenderable.isDone()) {
            CompletableFuture.allOf(arrowRenderable)
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

        // Initialize every type of node and set its parents and the Renderable.
        visualisePolygonGraph();
        visualiseCylinderGraph();
        visualiseArrowGraph();

        //This method render and display 3 Numbers which is measured value of Force from the sensors
        visualiseNumbers();

        startMqtt();

    }

    /**
     * For the localPosition use this scale for every type of node, "float 0.01f = 1 cm. in real world scale"
     * and also for LocalScale of Polygon node and Cylinder node(Renderable from ShapeFactory class) as well.
     *
     * Note that, Arrow graph node scale differently from the others because of its initial scale from .obj file
     *
     * For the ViewRenderable or Numbers node use setSizer to scale and the size also depends on textSize in R.layout.popup file as well
     * you can find the file in app/res/layout/popup.xml
     */
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

                            //Get the textView to set text later
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

                            //Get the textView to set text later
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

                            //Get the textView to set text later
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

                            //Get the textView to set text later
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

                            //Get the textView to set text later
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
        polygonNodeLeft.setRenderable(polygonRenderable);
        polygonNodeLeft.setLocalScale(initialScale);
        polygonNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));

        polygonNodeMid = new Node();
        polygonNodeMid.setParent(this);
        polygonNodeMid.setRenderable(polygonRenderable);
        polygonNodeMid.setLocalScale(initialScale);
        polygonNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));

        polygonNodeRight = new Node();
        polygonNodeRight.setParent(this);
        polygonNodeRight.setRenderable(polygonRenderable);
        polygonNodeRight.setLocalScale(initialScale);
        polygonNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));

        polygonNodeBack = new Node();
        polygonNodeBack.setParent(this);
        polygonNodeBack.setRenderable(polygonRenderable);
        polygonNodeBack.setLocalScale(initialScale);
        polygonNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));

        polygonNodeFoot = new Node();
        polygonNodeFoot.setParent(this);
        polygonNodeFoot.setRenderable(polygonRenderable);
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
        cylinderNodeLeft.setRenderable(cylinderRenderable);
        cylinderNodeLeft.setLocalScale(initialScale);
        cylinderNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));
        cylinderNodeLeft.setLocalRotation(rotateValue);


        cylinderNodeMid = new Node();
        cylinderNodeMid.setParent(this);
        cylinderNodeMid.setRenderable(cylinderRenderable);
        cylinderNodeMid.setLocalScale(initialScale);
        cylinderNodeMid.setLocalPosition(new Vector3(0, initialY, -0.15f));
        cylinderNodeMid.setLocalRotation(rotateValue);


        cylinderNodeRight = new Node();
        cylinderNodeRight.setParent(this);
        cylinderNodeRight.setRenderable(cylinderRenderable);
        cylinderNodeRight.setLocalScale(initialScale);
        cylinderNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));
        cylinderNodeRight.setLocalRotation(rotateValue);


        cylinderNodeBack = new Node();
        cylinderNodeBack.setParent(this);
        cylinderNodeBack.setRenderable(cylinderRenderable);
        cylinderNodeBack.setLocalScale(initialScale);
        cylinderNodeBack.setLocalPosition(new Vector3(0, 0, -0.3f));
        cylinderNodeBack.setLocalRotation(rotateValue);



        cylinderNodeFoot = new Node();
        cylinderNodeFoot.setParent(this);
        cylinderNodeFoot.setRenderable(cylinderRenderable);
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
        arrowNodeLeft.setRenderable(arrowRenderable.getNow(null));
        arrowNodeLeft.setLocalPosition(new Vector3(-0.3f, initialY, initialZ));
        arrowNodeLeft.setLocalScale(initialScale);
        arrowNodeLeft.setLocalRotation(rotateValue);


        arrowNodeMid = new Node();
        arrowNodeMid.setParent(this);
        arrowNodeMid.setRenderable(arrowRenderable.getNow(null));
        arrowNodeMid.setLocalPosition(new Vector3(0, initialY, -0.2f));
        arrowNodeMid.setLocalScale(initialScale);
        arrowNodeMid.setLocalRotation(rotateValue);


        arrowNodeRight = new Node();
        arrowNodeRight.setParent(this);
        arrowNodeRight.setRenderable(arrowRenderable.getNow(null));
        arrowNodeRight.setLocalPosition(new Vector3(0.3f, initialY, initialZ));
        arrowNodeRight.setLocalScale(initialScale);
        arrowNodeRight.setLocalRotation(rotateValue);


        arrowNodeBack = new Node();
        arrowNodeBack.setParent(this);
        arrowNodeBack.setRenderable(arrowRenderable.getNow(null));
        arrowNodeBack.setLocalPosition(new Vector3(0, 0, -0.35f));
        arrowNodeBack.setLocalScale(initialScale);
        arrowNodeBack.setLocalRotation(rotateValue);


        arrowNodeFoot = new Node();
        arrowNodeFoot.setParent(this);
        arrowNodeFoot.setRenderable(arrowRenderable.getNow(null));
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


    /**
     * For the left and right graph use a multiplier to scale the graph faster
     */
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

                    // replace all ' to " in the received message in order to covert it the JSON
                    message = mqttMessage.toString().trim().replaceAll("'","\"");

//                    Log.w(test_json, "Received JSON: "+message);

                    // Example of JSON
                    /*
                        {
                            "value":{
                                         "left":0.0,
                                         "mid":0.0,
                                         "right":0.0,
                                         "back":0.0,
                                         "foot":0.0
                                    }
                        }
                    */

                    messageJSON = new JSONObject(message);
                    result = messageJSON.getJSONObject("value");

                    leftValue = result.getDouble("left");
                    midValue = result.getDouble("mid");
                    rightValue = result.getDouble("right");
                    backValue = result.getDouble("back");
                    footValue = result.getDouble("foot");

//                    Log.w(test_json, "Received double: "+leftValue + ", " + midValue + ", " + rightValue + ", " + backValue + ", " + footValue);

                    leftSensor_textView.setText(String.valueOf(leftValue));
                    midSensor_textView.setText(String.valueOf(midValue));
                    rightSensor_textView.setText(String.valueOf(rightValue));
                    backSensor_textView.setText(String.valueOf(backValue));
                    footSensor_textView.setText(String.valueOf(footValue));

                    // These method use for setting the minimum or maximum value for the graph to scale
                    // in this case is 0.0 - 100.0, if more than 100.0 the number will be set to 100.0 and also 0.0 vice versa.
                    leftValue = roundValue(leftValue);
                    midValue = roundValue(midValue);
                    rightValue = roundValue(rightValue);
                    backValue = roundValue(backValue);
                    footValue = roundValue(footValue);

//                    Log.w(test_json, "After round and convert to float: "+leftValue + ", " + midValue + ", " + rightValue + ", " + backValue + ", " + footValue);

                    // Scale down the value to be a multiplier for each node scale
                    // Note that for the arrow will always have a 4 times scale more than the other
                    scaleAllLeftGraph((float) leftValue);
                    scaleAllMidGraph((float) midValue);
                    scaleAllRightGraph((float) rightValue);
                    scaleAllBackGraph((float) backValue);
                    scaleAllFootGraph((float) footValue);
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    Log.w(test_mqtt,"Delivery Complete");
                }
            });

            // Make it connect only once
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
}

