package graphics;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.ITracePoint2D;
import info.monitorenter.gui.chart.TracePoint2D;
import info.monitorenter.gui.chart.traces.Trace2DLtdReplacing;
import info.monitorenter.gui.chart.traces.Trace2DLtdSorted;
import info.monitorenter.gui.chart.traces.Trace2DReplacing;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

public class GraphicPanel extends JPanel
{
    private Chart2D chart;
    private ITrace2D trace;
    private ITrace2D threshTrace;

    private double threshTraceX1;
    private double threshTraceX2;
    private double threshTraceY;

    private double logBasis;

    private boolean decibelView;

    public GraphicPanel()
    {
        // Create chart
        chart = new Chart2D();
        trace = null;
        threshTrace = null;
        decibelView = true;

        // Setting layout
        SpringLayout layout = new SpringLayout();
        Container contentPane = this;
        setLayout(layout);
        layout.putConstraint(SpringLayout.WEST, chart, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, chart, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, contentPane, 5, SpringLayout.EAST, chart);
        layout.putConstraint(SpringLayout.SOUTH, contentPane, 5, SpringLayout.SOUTH, chart);

        add(chart);
    }

    public void getReadyToPaint(int sampleRate)
    {
        chart.removeAllTraces();

        trace = new Trace2DLtdSorted(sampleRate/2);
        trace.setColor(Color.RED);

        threshTrace = new Trace2DLtdSorted(2);
        threshTrace.setColor(Color.BLUE);

        chart.addTrace(trace);
        chart.addTrace(threshTrace);
    }

    public void cleanTraces()
    {
        if (trace != null)
            trace.removeAllPoints();
        if (threshTrace != null)
            threshTrace.removeAllPoints();

    }
    public void cleanTraces2()
    {
        Iterator<ITracePoint2D> i = trace.iterator();
        while (i.hasNext())
        {
            ITracePoint2D p = i.next();
            p.setScaledY(0);
        }
    }

    public void updatePoint(double x, double y)
    {
        if (y != 0) //why?
        {
            if (decibelView)
                y = 10*Math.log10(y / logBasis);

            //if ((y != Double.NEGATIVE_INFINITY) && (y != Double.POSITIVE_INFINITY))
                trace.addPoint(x, y);
        }
        /*if (y == 0)
            y = 0.01;

        if (decibelView)
            y = 10*Math.log10(y / logBasis);

        trace.addPoint(x, y);*/
    }

    public void updateThreshTrace(double x1, double x2, double y)
    {
        if (threshTrace != null)
        {
            threshTraceX1 = x1;
            threshTraceX2 = x2;
            threshTraceY = y;

            if (decibelView)
                y = 10*Math.log10(y / logBasis);

            threshTrace.removeAllPoints();
            threshTrace.addPoint(x1, y);
            threshTrace.addPoint(x2, y);
        }
    }

    public void setDecibelView(boolean flag)
    {
        if (trace != null)
            trace.removeAllPoints();
        decibelView = flag;
        updateThreshTrace(threshTraceX1, threshTraceX2, threshTraceY);
    }

    public void setLogBasis(double logBasis) { this.logBasis = logBasis; }
}
