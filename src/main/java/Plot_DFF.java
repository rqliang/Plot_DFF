import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotDialog;
import ij.measure.ResultsTable;
import ij.plugin.JpegWriter;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.util.ArrayUtil;
import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class Plot_DFF implements PlugIn {

    @Override
    public void run(String s) {
        final ImagePlus imp = WindowManager.getCurrentImage();
        final LSMFileInfo openLSM = (LSMFileInfo) imp.getOriginalFileInfo();
        final CZLSMInfoExtended cz = (CZLSMInfoExtended) ((ImageDirectory) openLSM.imageDirectories.get(0)).TIF_CZ_LSMINFO;
        //final int n = new Long(cz.timeStamps.NumberTimeStamps).intValue();
        double[] timeStamps = cz.timeStamps.TimeStamps;
        final RoiManager roiManager = RoiManager.getInstance();
        ResultsTable rt = roiManager.multiMeasure(imp);
        final int nRow = rt.getCounter();
        final int nROI = roiManager.getCount();
        final int nCol = rt.getLastColumn();
        ArrayUtil arrayUtil = new ArrayUtil(nROI);
        double y[] = new double[nRow];
        double sd[] = new double[nRow];
        double[] yplusSD = new double[nRow];
        double[] yMinusSD = new double[nRow];


        for(int i=0, j=0; i<nCol; i++){
            String header = rt.getColumnHeading(i);
            if (header.contains("Mean")) {
                double[] thisCol = rt.getColumnAsDoubles(i);
                for(int k=0; k<nRow;k++) {
                    double t= thisCol[k] / thisCol[0] - 1;
                    rt.setValue(String.format("dFF%d",j+1),k,t);
                }
                j++;
            }
        }

        for(int i=0; i<nRow; i++){
            double m = 0;
            for(int j=0; j<nROI; j++)
                arrayUtil.putValue(j,(float) rt.getValue(String.format("dFF%d",j+1),i));
            y[i] = arrayUtil.getMean();
            sd[i] = Math.sqrt(arrayUtil.getVariance()/nROI);
            yplusSD[i] = y[i] + sd[i];
            yMinusSD[i] = y[i] - sd[i];
        }


        for(int i=0; i<nRow; i++){
            rt.setValue("MeanDff",i,y[i]);
            rt.setValue("SEM",i,sd[i]);
            rt.setValue("Time",i, timeStamps[i]);
        }
        String thePath = IJ.getDirectory("image");
        try {
            rt.show("Results");
            rt.saveAs(thePath +"/" + imp.getShortTitle() + "-data.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ActionEvent actionEvent = new ActionEvent(imp,0,"Save...");
        roiManager.actionPerformed(actionEvent);
        //roiManager.reset();


        Plot plot = new Plot("\u0394F/F of " + imp.getShortTitle(), "Time (s)" ,"\u0394F/F",
                timeStamps, rt.getColumnAsDoubles(rt.getColumnIndex("dFF1")),Plot.LINE);
        //plot.setLimits(timeStamps[0],timeStamps[timeStamps.length-1],Tools.getMinMax(yMinusSD)[0],Tools.getMinMax(yplusSD)[1]);
        /*plot.setFrameSize(1024,768);
        plot.setLineWidth(4);
        plot.setFont(0,48);*/
        plot.setXTicks(true);
        plot.setXMinorTicks(true);
        plot.setYTicks(true);
        plot.setYMinorTicks(true);

        plot.draw();
        for(int i=2; i<=nROI; i++){
            plot.addPoints(timeStamps, rt.getColumnAsDoubles(rt.getColumnIndex(String.format("dFF%d",i))),Plot.LINE);
            plot.draw();
        }
        plot.setColor(Color.RED);
        plot.addPoints(timeStamps,y,Plot.LINE);
        plot.draw();
        plot.setLimitsToFit(true);
        plot.show();
        PlotDialog plotDialog = new PlotDialog(plot, PlotDialog.AXIS_OPTIONS);
        plotDialog.showDialog(null);
        //PlotWindow plotWindow = plot.show();
        ImagePlus plotImg = plot.makeHighResolution("DFF",4,true,true);
        JpegWriter.save(plotImg,thePath +"/" + imp.getShortTitle()+"-plot.jpg",100);
        //plotWindow.close();
    }
}
