import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.util.ArrayUtil;
import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;

import java.awt.*;

public class Plot_DFF implements PlugIn {

    @Override
    public void run(String s) {
        final ImagePlus imp = WindowManager.getCurrentImage();
        final LSMFileInfo openLSM = (LSMFileInfo) imp.getOriginalFileInfo();
        final CZLSMInfoExtended cz = (CZLSMInfoExtended) ((ImageDirectory) openLSM.imageDirectories.get(0)).TIF_CZ_LSMINFO;
        //final int n = new Long(cz.timeStamps.NumberTimeStamps).intValue();
        double[] timeStamps = cz.timeStamps.TimeStamps;
        ResultsTable rt = Analyzer.getResultsTable();
        final int nRow = rt.getCounter();
        final RoiManager roiManager = RoiManager.getInstance();

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
            sd[i] = Math.sqrt(arrayUtil.getVariance());
            yplusSD[i] = y[i] + sd[i];
            yMinusSD[i] = y[i] - sd[i];
        }


        for(int i=0; i<nRow; i++){
            rt.setValue("MeanDff",i,y[i]);
            rt.setValue("std",i,sd[i]);
            rt.setValue("Time",i, timeStamps[i]);
        }

        rt.saveAs(IJ.getDirectory("image" + ));
        Plot plot = new Plot("\u0394F/F of " + imp.getShortTitle(), "Time (s)" ,"\u0394F/F",
                timeStamps, y);
        //plot.setLimits(timeStamps[0],timeStamps[timeStamps.length-1],Tools.getMinMax(yMinusSD)[0],Tools.getMinMax(yplusSD)[1]);
        plot.draw();
        plot.setColor(Color.BLUE);
        plot.addPoints(timeStamps,yplusSD,Plot.LINE);
        plot.draw();
        plot.addPoints(timeStamps,yMinusSD,Plot.LINE);
        plot.draw();
        plot.setLimitsToFit(false);
        //PlotWindow plotWindow = plot.show();
    }
}
