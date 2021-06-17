package com.andreiliphd.katherine.scichart.viewer;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.charting.model.dataSeries.IXyDataSeries;
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries;
import com.scichart.extensions.builders.SciChartBuilder;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.github.cdimascio.dotenv.Dotenv;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Dotenv dotenv = Dotenv.configure()
                .directory("/assets")
                .filename("env").load();
        setContentView(R.layout.activity_main);
        try {
            // set license key before using SciChart
            SciChartSurface.setRuntimeLicenseKey(dotenv.get("scichart"));
        } catch (Exception e) {
            Log.e("SciChart", "Error during setting license key", e);
        }

        // Added in Tutorial #1
        // Create a SciChartSurface
        final SciChartSurface surface = new SciChartSurface(this);
        HbdmHttpClient client = new HbdmHttpClient();

        // Get a layout declared in "activity_main.xml" by id
        LinearLayout chartLayout = (LinearLayout) findViewById(R.id.chart_layout);

        // Add the SciChartSurface to the layout
        chartLayout.addView(surface);

        // Initialize the SciChartBuilder
        SciChartBuilder.init(this);

        // Obtain the SciChartBuilder instance
        final SciChartBuilder sciChartBuilder = SciChartBuilder.instance();
        final IAxis xAxis = sciChartBuilder.newNumericAxis().withGrowBy(0.1d, 0.1d).withVisibleRange(1.1, 2.7).build();
        final IAxis yAxis = sciChartBuilder.newNumericAxis().withGrowBy(0.1d, 0.1d).build();

        final DoubleSeries fourierSeries = DataManager.getInstance().getFourierSeries(1.0, 0.1, 5000);
        final IXyDataSeries<Double, Double> dataSeries = sciChartBuilder.newXyDataSeries(Double.class, Double.class).build();

        final FastLineRenderableSeries rSeries = sciChartBuilder.newLineSeries().withDataSeries(dataSeries).withStrokeStyle(0xFF279B27, 1f, true).build();

        UpdateSuspender.using(surface, new Runnable() {
            @Override
            public void run() {
                Collections.addAll(surface.getXAxes(), xAxis);
                Collections.addAll(surface.getYAxes(), yAxis);
                Collections.addAll(surface.getRenderableSeries(), rSeries);
                Collections.addAll(surface.getChartModifiers(), sciChartBuilder.newModifierGroupWithDefaultModifiers().build());
            }
        });
        TimerTask updateDataTask = new TimerTask() {
            @Override
            public void run() {
                UpdateSuspender.using(surface, new Runnable() {
                    @Override
                    public void run() {
                        int x = dataSeries.getCount();
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("symbol", "btcusdt");
                        Map<String, String> query = new HashMap<String, String>();
                        String response = client.callJson(dotenv.get("key"), dotenv.get("secret"), "POST", "https://api.huobi.pro/market/trade", query, params);
                        Float price = JSON.parseObject(response).getJSONObject("tick").getJSONArray("data").getJSONObject(0).getFloat("price");
                        Log.i("HUOBI",price.toString());
                        dataSeries.append((double) x, (double) price);
                        if (x >= 1000) {
                            dataSeries.removeAt(0);
                            dataSeries.append((double) x, (double) price);
                        }
                        // Zoom series to fit the viewport
                        surface.zoomExtents();
                    }
                });
            }
        };

        Timer timer = new Timer();

        long delay = 0;
        long interval = 10;
        timer.schedule(updateDataTask, delay, interval);



//        // Create a numeric X axis
//        final IAxis xAxis = sciChartBuilder.newNumericAxis()
//                .withAxisTitle("X Axis Title")
//                .withVisibleRange(-5, 15)
//                .build();
//
//        // Create a numeric Y axis
//        final IAxis yAxis = sciChartBuilder.newNumericAxis()
//                .withAxisTitle("Y Axis Title").withVisibleRange(0, 100).build();
//
//        // Create a TextAnnotation and specify the inscription and position for it
//        TextAnnotation textAnnotation = sciChartBuilder.newTextAnnotation()
//                .withX1(5.0)
//                .withY1(55.0)
//                .withText("Hello World!")
//                .withHorizontalAnchorPoint(HorizontalAnchorPoint.Center)
//                .withVerticalAnchorPoint(VerticalAnchorPoint.Center)
//                .withFontStyle(20, ColorUtil.White)
//                .build();
//
//        // Added in Tutorial #3
//        // Add a bunch of interaction modifiers to a ModifierGroup
//        ModifierGroup chartModifiers = sciChartBuilder.newModifierGroup()
//                .withPinchZoomModifier().build()
//                .withZoomPanModifier().withReceiveHandledEvents(true).build()
//                .withZoomExtentsModifier().withReceiveHandledEvents(true).build()
//                .withXAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Scale).withClipModeX(ClipMode.None).build()
//                .withYAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Pan).build()
//                .build();
//
//        // Add the Y axis to the YAxes collection of the surface
//        Collections.addAll(surface.getYAxes(), yAxis);
//
//        // Add the X axis to the XAxes collection of the surface
//        Collections.addAll(surface.getXAxes(), xAxis);
//
//        // Add the annotation to the Annotations collection of the surface
//        Collections.addAll(surface.getAnnotations(), textAnnotation);
//
//        // Add the interactions to the ChartModifiers collection of the surface
//        Collections.addAll(surface.getChartModifiers(), chartModifiers);
//
//        // Added in Tutorial #6 - Appending data in realtime
//        // Create a couple of DataSeries for numeric (Int, Double) data
//        final XyDataSeries lineData = sciChartBuilder.newXyDataSeries(Integer.class, Double.class).build();
//        final XyDataSeries scatterData = sciChartBuilder.newXyDataSeries(Integer.class, Double.class).build();
//
//        // New code below
//        // Add new data on update
//        TimerTask updateDataTask = new TimerTask() {
//            @Override
//            public void run() {
//                UpdateSuspender.using(surface, new Runnable() {
//                    @Override
//                    public void run() {
//                        int x = lineData.getCount();
//
//                        lineData.append(x, Math.sin(x * 0.1));
//                        scatterData.append(x, Math.cos(x * 0.1));
//
//                        // Zoom series to fit the viewport
//                        surface.zoomExtents();
//                    }
//                });
//            }
//        };
//
//        Timer timer = new Timer();
//
//        long delay = 0;
//        long interval = 10;
//        timer.schedule(updateDataTask, delay, interval);
//
//        // Create and configure a line series
//        final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
//                .withDataSeries(lineData)
//                .withStrokeStyle(ColorUtil.LightBlue, 2f, true)
//                .build();
//
//        // Create an Ellipse PointMarker for the Scatter Series
//        EllipsePointMarker pointMarker = sciChartBuilder
//                .newPointMarker(new EllipsePointMarker())
//                .withFill(ColorUtil.LightBlue)
//                .withStroke(ColorUtil.Green, 2f)
//                .withSize(10)
//                .build();
//
//        // Create and configure a scatter series
//        final IRenderableSeries scatterSeries = sciChartBuilder.newScatterSeries()
//                .withDataSeries(scatterData)
//                .withPointMarker(pointMarker)
//                .build();
//
//        // Add a RenderableSeries onto the SciChartSurface
//        surface.getRenderableSeries().add(scatterSeries);
//        surface.getRenderableSeries().add(lineSeries);
//        surface.zoomExtents();
//
//        // Added in Tutorial #5
//        // Create a LegendModifier and configure a chart legend
//        ModifierGroup legendModifier = sciChartBuilder.newModifierGroup()
//                .withLegendModifier()
//                .withOrientation(Orientation.HORIZONTAL)
//                .withPosition(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 10)
//                .build()
//                .build();
//
//        // Add the LegendModifier to the SciChartSurface
//        surface.getChartModifiers().add(legendModifier);
//
//        // Create and configure a CursorModifier
//        ModifierGroup cursorModifier = sciChartBuilder.newModifierGroup()
//                .withCursorModifier().withShowTooltip(true).build()
//                .build();
//
//        // Add the CursorModifier to the SciChartSurface
//        surface.getChartModifiers().add(cursorModifier);
    }
}
