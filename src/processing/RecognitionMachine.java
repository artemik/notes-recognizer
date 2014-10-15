package processing;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import graphics.GraphicPanel;
import graphics.NotesPanel;
import processing.windowFunctions.HammingWindow;
import processing.windowFunctions.HannWindow;
import processing.windowFunctions.IWindowFunction;

import javax.sound.midi.*;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;

public class RecognitionMachine
{
    private GraphicPanel                     graphicPanel;
    private NotesPanel                       notesPanel;

    private HashMap<String, IWindowFunction> windowFunctions;
    private IWindowFunction                  currentWindow;
    private int                              wSize;
    private int                              step;

    private double                           noiseThreshold;
    private double                           minSoundThreshold;
    private double                           spikeFactor;
    private int                              minNoteDuration; // Each window = 1 tick

    private WaveFile                         wf;
    private int                              sampleRate;
    private int                              framesCount;
    private int                              sampleSize;

    private double[]                         xValues;

    private LinkedList<Note>                 notes;
    private final int                        startCode;
    private final int                        endCode;

    private double                           dZero; // For freq graphic to be starting at zero
    private final ReentrantLock              recogParamChangesLock;


    public RecognitionMachine(GraphicPanel graphicPanel, NotesPanel notesPanel, ReentrantLock recogParamChangesLock)
    {
        this.graphicPanel = graphicPanel;
        this.notesPanel = notesPanel;
        windowFunctions = new HashMap<String, IWindowFunction>();
        windowFunctions.put("Hamming", new HammingWindow());
        windowFunctions.put("Hann", new HannWindow());
        currentWindow = windowFunctions.get("Hamming"); // Default
        wSize = 8192; // Default
        step = wSize / 2; // Default
        noiseThreshold = 2000*1000; // Default
        minSoundThreshold = 150*1000; // Defualt
        spikeFactor = 1.15; // Default
        minNoteDuration = 3; // Default
        wf = null;
        sampleRate = -1;
        framesCount = -1;
        sampleSize = -1;
        xValues = null;
        notes = null;
        startCode = 45; //Java code of A2
        endCode = 119; // Java code of B8
        this.recogParamChangesLock = recogParamChangesLock;
    }

    private void genXValues()
    {
        recogParamChangesLock.lock();
        xValues = new double[wSize / 2];
        dZero = Math.log10(  ((double)sampleRate)/wSize  );
        for (int i = 0; i < xValues.length; i++)
        {
            xValues[i] =  Math.log10(  ((double)(i+1)*sampleRate)/wSize  ) - dZero;
        }
        xValues[0] = 0;
        recogParamChangesLock.unlock();
    }

    private double getF(int javaCode)
    {
        return 27.5 * Math.pow(2, ((double)javaCode-21)/12);
    }

    private static String getName(int i)
    {
        String name = "";
        // Octave
        switch (i % 12)
        {
            case 0:
                name = "C";
                break;
            case 1:
                name = "C#";
                break;
            case 2:
                name = "D";
                break;
            case 3:
                name = "D#";
                break;
            case 4:
                name = "E";
                break;
            case 5:
                name = "F";
                break;
            case 6:
                name = "F#";
                break;
            case 7:
                name = "G";
                break;
            case 8:
                name = "G#";
                break;
            case 9:
                name = "A";
                break;
            case 10:
                name = "A#";
                break;
            case 11:
                name = "H";
                break;

        }
        name += String.valueOf(i / 12 - 1); //Octave number
        return name;
    }

    private void genNotes()
    {
        notes = new LinkedList<Note>();
        for (int i = startCode; i <= endCode; i++)
        {
            //double f = 27.5 * Math.pow(2, (i-21)/12);
            //double f = 27.5 * Math.pow(2, (i+(double)48-(double)startCode)/12);
            double f = getF(i);
            int leftIdx;
            int rightIdx;
            double leftMean;
            double rightMean;

            if (i != startCode)
            {
                leftMean = (f + getF(i-1)) / 2;
                leftIdx = (int) Math.floor(leftMean / ((double)sampleRate / wSize) + 1) - 1; // round up
            }
            else
            {
                leftMean = f;
                leftIdx = (int) Math.round(f / ((double)sampleRate / wSize)) - 1;
            }

            if (i != endCode)
            {
                rightMean = (f + getF(i+1)) / 2;
                rightIdx = (int) Math.floor(rightMean / ((double)sampleRate / wSize)) - 1; // round down
            }
            else
            {
                rightMean = f;
                rightIdx = (int) Math.round(f / ((double)sampleRate / wSize)) - 1;
            }
            notes.addLast(new Note(getName(i), f, i, leftIdx, rightIdx, leftMean, rightMean));
        }

        //Generate harmonics lists
        for (Note n : notes)
        {
            double curH = n.getFreq();
            //start with the second harmonic and go higher until reach the maximum possible freq value
            while (curH < 4187)
            {
                curH += n.getFreq();
                int i = 0;
                boolean found = false;

                //if matches current note then it's our harmonic note
                while ((i < notes.size()) && !found)
                {
                    if ((curH >= notes.get(i).getLeftMean()) && (curH < notes.get(i).getRightMean()))
                        found = true;
                    i++;
                }

                if (found)
                    n.addHarmonicNote(notes.get(i-1));
            }
        }
    }

    private double getWeight(double x)
    {
        //This weight function represents some sort of exponential weight.
        //The higher frequency, the less the weigh coefficient.
        //Unless we get to the 340hz, then the coefficient equals to 1 (no changes).
        double w = 1;
        if (x < 340)
            w = 7-0.007*x*0.007*x;
        return w;
    }

    private void weighFreqs(ArrayList<Complex> freq)
    {
        //Weigh
        for (int i = 0; i < freq.size(); i++)
        {
            if (freq.get(i).abs() >= noiseThreshold)
            {
                freq.get(i).x = freq.get(i).x * getWeight(i * ((double) sampleRate / wSize));
                freq.get(i).y = freq.get(i).y * getWeight(i * ((double) sampleRate / wSize));
            }
        }
    }

    private void getFreq(ArrayList<Complex> counts, ArrayList<Complex> freq, int offset)
    {
        counts.clear();
        for (int i = 0+offset; i < wSize+offset; i++)
        {
            if (i < framesCount)
                counts.add(new Complex(wf.getSampleInt(i)));
            else
                for (; i < wSize+offset; i++)
                    counts.add(new Complex(0));
        }

        //APPLY WINDOW
        int q = 0;
        for (Complex c : counts)
        {
            c.x = c.x * currentWindow.getWindowValue(q, wSize);
            q++;
        }

        freq.clear();
        freq.addAll(counts);

        FastFourierTransform.FFT(freq, false);
    }

    private boolean checkForThresholdUp(Note n, ArrayList<Complex> freq, double threshold)
    {
        //Returns true if the note's magnitude is above(equal) the threshold value
        boolean exceeded = false;
        for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
        {
            if (freq.get(i).abs() >= threshold)
            {
                exceeded = true;
            }
        }
        return exceeded;
    }

    private void checkHarmonics(Note n, LinkedHashMap<Note, Boolean> checkNotesList, ArrayList<Complex> freq, HashSet<Note> curNotes)
    {
        if (!checkNotesList.get(n))
        {
            checkNotesList.put(n, true);
            if (checkForThresholdUp(n, freq, noiseThreshold))
            {
                curNotes.add(n);

                double curPeak = -1;
                for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
                {
                    if (freq.get(i).abs() > curPeak)
                    {
                        curPeak = freq.get(i).abs();
                    }
                }


                for (Note h : n.getHarmonicsList())
                {

                    if (!checkForThresholdUp(h, freq, curPeak))
                    {
                        checkNotesList.put(h, true);
                        double maxMag = -1;
                        for (int i = h.getLeftIdx(); i <= h.getRightIdx(); i++)
                        {
                            if (freq.get(i).abs() > maxMag)
                            {
                                maxMag = freq.get(i).abs();
                            }
                        }
                        curPeak = maxMag;
                    }
                    else
                    {
                        if (!checkNotesList.get(h))
                            checkHarmonics(h, checkNotesList, freq, curNotes);
                    }
                }
            }
        }

    }

    private void writeNoteToTrack(Note n, int offset, Track t)
    {
        // Ignore very short notes (probable noise)
        if (n.getDuration() > minNoteDuration)
        {
            ShortMessage myMsgOn = new ShortMessage();
            ShortMessage myMsgOff = new ShortMessage();
            try
            {
                myMsgOn.setMessage(ShortMessage.NOTE_ON, 0, n.getCode(), 100);
                myMsgOff.setMessage(ShortMessage.NOTE_OFF, 0, n.getCode(), 100);
            } catch (InvalidMidiDataException e) { e.printStackTrace(); }


            int curPos = offset / step;
            MidiEvent me1 = new MidiEvent(myMsgOn, curPos - n.getDuration());
            MidiEvent me2 = new MidiEvent(myMsgOff, curPos);
            n.setDuration(0);

            t.add(me1);
            t.add(me2);
        }
    }


    public void setFilePath(String filePath) throws UnsupportedAudioFileException, IOException
    {
        if (wf != null)
            wf.getClip().close();
        wf = new WaveFile(new File(filePath)); // Throws exception if unknown file
        sampleRate = wf.getSampleRate();
        framesCount = (int)wf.getFramesCount(); // (!) Might not fit the integer. (!)
        sampleSize = wf.getSampleSize();
        double logBasis = ((double) Integer.MAX_VALUE) * Math.pow(10, sampleSize);
        graphicPanel.setLogBasis(logBasis);
        genXValues();
    }

    public void setWindow(String windowName)
    {
        recogParamChangesLock.lock();
        currentWindow = windowFunctions.get(windowName);
        recogParamChangesLock.unlock();
    }

    public void setWindowSize(int wSize)
    {
        if ((wSize >= 512) && (wSize <= 32768) && (wSize != this.wSize))
        {
            double power = Math.log(wSize) / Math.log(2);
            if (power == (int)power) // Numbers power of two pass only
            {
                recogParamChangesLock.lock();
                this.wSize = wSize;
                //step = wSize / 2;
                step = 2048;
                graphicPanel.cleanTraces();
                genXValues();
                genNotes();
                graphicPanel.updateThreshTrace(0, xValues[xValues.length - 1], noiseThreshold);
                recogParamChangesLock.unlock();
            }
        }
    }

    public void setThreshold(double noiseThreshold)
    {
        if (noiseThreshold >= 100) // Some logic limit
        {
            noiseThreshold = noiseThreshold * 1000;
            if ((this.noiseThreshold != noiseThreshold) && (xValues != null))
            {
                recogParamChangesLock.lock();
                this.noiseThreshold = noiseThreshold;
                graphicPanel.updateThreshTrace(0, xValues[xValues.length - 1], noiseThreshold);
                recogParamChangesLock.unlock();
            }
        }
    }

    public boolean hasInputFile() { return  !(wf == null); }




    public void startCalculation2()
    {
        // If no file was picked
        if (wf == null)
            return;

        // TO-DO: Extra work here. Should've stored frequencies of the file somewhere before playing/saving and use them here instead of calculation every time.
        ArrayList<Complex> counts = new ArrayList<Complex>(0);
        ArrayList<Complex> freq = new ArrayList<Complex>(0);

        graphicPanel.getReadyToPaint(sampleRate);
        graphicPanel.updateThreshTrace(0, xValues[xValues.length - 1], noiseThreshold);

        // Play audio if available
        wf.getClip().setFramePosition(0);
        wf.play();

        genNotes();

        HashSet<Note> prevNotes = new HashSet<Note>();

        int offset = 0;
        while (!Thread.currentThread().isInterrupted() && (offset < framesCount))
        {
            while (recogParamChangesLock.hasQueuedThreads())
                ;
            recogParamChangesLock.lock();

            // Get data and perform FFT
            getFreq(counts, freq, offset);
            // Weigh frequencies (only that are above the noiseThreshold)
            weighFreqs(freq);

            //RECOGNITION SECTION ITSELF
            // Create list of notes to check
            final LinkedHashMap<Note, Boolean> checkNotesList = new LinkedHashMap(endCode - startCode + 1);
            for (Note n : notes)
            {
                checkNotesList.put(n, false);
            }

            //If some playing note spikes treat it as a release of note.
            // !!! Only for startSaving (old) function !!! ///
            /*Iterator<Note> it = prevNotes.iterator();
            while (it.hasNext())
            {
                Note n = it.next();
                if (checkForThresholdUp(n, freq, n.getPrevMagnitude()*spikeFactor))
                {
                    writeNoteToTrack(n, offset-step, t);
                    it.remove();
                }
            }*/

            HashSet<Note> curNotes = new HashSet<Note>();
            //Still watch the notes if it's above minSoundThreshold. Add them to the current playing notes hashSet.
            for (Note n : prevNotes)
            {
                // Since previous playing notes could possibly become quieter than noiseThreshold they haven't been weighted going through the weighFreqs(). Do it now.
                if (!checkForThresholdUp(n, freq, noiseThreshold))
                {
                    for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
                    {
                        freq.get(i).x = freq.get(i).x * getWeight(i * ((double) sampleRate / wSize));
                        freq.get(i).y = freq.get(i).y * getWeight(i * ((double) sampleRate / wSize));
                    }
                }

                if (checkForThresholdUp(n, freq, minSoundThreshold))
                {
                    curNotes.add(n);
                }
            }

            for (Map.Entry<Note, Boolean> e : checkNotesList.entrySet())
            {
                // If should check this note
                if (!e.getValue())
                    checkHarmonics(e.getKey(), checkNotesList, freq, curNotes);
            }

            // Update the plot
            //graphicPanel.cleanTraces2();
            /*for (int i = 0; i < 744; i++)
            {
                graphicPanel.updatePoint((i+1)*((double)sampleRate/wSize), freq.get(i).abs());
            }*/
            for (int i = 0; i < freq.size()/2; i++)
            {
                graphicPanel.updatePoint(xValues[i], freq.get(i).abs());
            }


            // Update "currently being played notes" label.
            final StringBuilder notestStr2 = new StringBuilder();
            for (Note n : curNotes)
            {
                notestStr2.append(n.getName() + " ");
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {notesPanel.setNotesLabel(notestStr2.toString());}
            });


            // Increase the duration of notes currently being played. And set the previous magnitude (for the next iteration).
            for (Note n : curNotes)
            {
                // !!! Only for saving!!! //
                //n.setDuration(n.getDuration() + 1);

                double maxMag = -1;
                for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
                {
                    if (freq.get(i).abs() > maxMag)
                    {
                        maxMag = freq.get(i).abs();
                    }
                }
                n.setPrevMagnitude(maxMag);
            }
            prevNotes.clear();
            prevNotes.addAll(curNotes);


            offset += step;

            // Following the music. Some sort of way.
            if (wf.isCanPlay() && wf.getClip().isActive())
            {
                //System.out.println(offset + " " + wf.getClip().getLongFramePosition());
                while (true)
                {
                    long pos = wf.getClip().getLongFramePosition();
                    if ((offset < pos) || (!wf.getClip().isActive()))
                        break;
                }
            }
            else
            {
                try {
                    Thread.sleep(50); // Approximate value
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            recogParamChangesLock.unlock();
        }
        wf.stop();
        graphicPanel.cleanTraces();
    }

    public void startSaving2(String outputFileFullPath, final ProgressMonitor pg)
    {
        // If no file was picked
        if (wf == null)
            return;

        // TO-DO: Extra work here. Should've stored frequencies of the file somewhere before playing/saving and use them here instead of calculation every time.
        ArrayList<Complex> counts = new ArrayList<Complex>(0);
        ArrayList<Complex> freq = new ArrayList<Complex>(0);

        if (notes == null)
            genNotes();

        HashSet<Note> prevNotes = new HashSet<Note>();
        Sequence s = null;
        try { s = new Sequence(0, Math.round((float)sampleRate / (2*step))); }
        catch (InvalidMidiDataException e) { e.printStackTrace(); }
        Track t = s.createTrack();

        int offset = 0;
        while (!Thread.currentThread().isInterrupted() && (offset < framesCount) && !pg.isCanceled())
        {
            // Get data and perform FFT
            getFreq(counts, freq, offset);
            // Weigh frequencies (only that are above the noiseThreshold)
            weighFreqs(freq);

            //RECOGNITION SECTION ITSELF
            // Create list of notes to check
            final LinkedHashMap<Note, Boolean> checkNotesList = new LinkedHashMap(endCode - startCode + 1);
            for (Note n : notes)
            {
                checkNotesList.put(n, false);
            }

            //If a playing note spikes treat it as a release of note.
            Iterator<Note> it = prevNotes.iterator();
            while (it.hasNext())
            {
                Note n = it.next();
                if (checkForThresholdUp(n, freq, n.getPrevMagnitude()*spikeFactor))
                {
                    writeNoteToTrack(n, offset-step, t);
                    it.remove();
                }
            }

            HashSet<Note> curNotes = new HashSet<Note>();
            //Still watch the notes if it's above minSoundThreshold. Add them to the current playing notes hashSet.
            for (Note n : prevNotes)
            {
                // Since previous playing notes could possibly become quieter than noiseThreshold they haven't been weighted going through the weighFreqs(). Do it now.
                if (!checkForThresholdUp(n, freq, noiseThreshold))
                {
                    for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
                    {
                        freq.get(i).x = freq.get(i).x * getWeight(i * ((double) sampleRate / wSize));
                        freq.get(i).y = freq.get(i).y * getWeight(i * ((double) sampleRate / wSize));
                    }
                }

                if (checkForThresholdUp(n, freq, minSoundThreshold))
                {
                    curNotes.add(n);
                }
            }


            for (Map.Entry<Note, Boolean> e : checkNotesList.entrySet())
            {
                // If should check this note
                if (!e.getValue())
                    checkHarmonics(e.getKey(), checkNotesList, freq, curNotes);
            }

            // Compare previous playing notes and current ones.
            // Those that don't match are treated as released.
            if (prevNotes.size() > 0)
            {
                for (Note n : prevNotes)
                {
                    if (!curNotes.contains(n))
                    {
                        writeNoteToTrack(n, offset, t);
                    }
                }
            }
            // Increase the duration of notes currently being played. And set previous magnitude (for next iteration).
            for (Note n : curNotes)
            {
                n.setDuration(n.getDuration() + 1);

                double maxMag = -1;
                for (int i = n.getLeftIdx(); i <= n.getRightIdx(); i++)
                {
                    if (freq.get(i).abs() > maxMag)
                    {
                        maxMag = freq.get(i).abs();
                    }
                }
                n.setPrevMagnitude(maxMag);
            }
            prevNotes.clear();
            prevNotes.addAll(curNotes);

            // Update the progress bar
            final int progressPercentage = (int)(((double)offset / framesCount) * 100);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pg.setNote("Completed " + progressPercentage + "%.");
                    pg.setProgress(progressPercentage);
                }
            });

            offset += step;
        }

        // If saving wasn't cancelled
        if (!pg.isCanceled())
        {
            // Try to save the output data to file.
            try
            {
                File outputFile = new File(outputFileFullPath);
                if (!outputFile.exists() | (outputFile.exists() && outputFile.delete()))
                    MidiSystem.write(s, 0, outputFile);
            }
            catch (IOException e) { e.printStackTrace(); }
            pg.setProgress(pg.getMaximum());
        }
    }







/*

    public void startCalculation()
    {
        // If no file was picked
        if (wf == null)
            return;

        ArrayList<Complex> counts = new ArrayList<Complex>(0);
        ArrayList<Complex> freq = new ArrayList<Complex>(0);

        graphicPanel.getReadyToPaint(xValues, sampleRate);
        graphicPanel.updateThreshTrace(0, xValues[xValues.length - 1], threshold);

        // Play audio if available
        wf.getClip().setFramePosition(0);
        wf.play();

        int offset = 0;
        HashSet<Note> prevNotes = new HashSet<Note>();
        while (!Thread.currentThread().isInterrupted() && (offset < framesCount))
        {
            while (recogParamChangesLock.hasQueuedThreads())
                ;

            recogParamChangesLock.lock();
            counts.clear();
            for (int i = 0+offset; i < wSize+offset; i++)
            {
                if (i < framesCount)
                    counts.add(new Complex(wf.getSampleInt(i)));
                else
                    for (; i < wSize+offset; i++)
                        counts.add(new Complex(0));
            }

            //APPLY WINDOW
            int q = 0;
            for (Complex c : counts)
            {
                c.x = c.x * currentWindow.getWindowValue(q, wSize);
                q++;
            }

            freq.clear();
            freq.addAll(counts);

            FastFourierTransform.FFT(freq, false);

            for (int i = 0; i < freq.size()/2; i++)
            {
                graphicPanel.updatePoint(xValues[i], freq.get(i).abs());
            }



            HashSet<Note> curNotes = new HashSet<Note>();
            double locMax = -999;
            int locMaxIdx = -1;
            for (int i = 0; i < freq.size()/2; i++)
            {
                if ((freq.get(i).abs() > threshold) && (freq.get(i).abs() >= locMax))
                {
                    */
/*if (freq.get(i).abs() >= locMax)
                    {*//*

                        locMax = freq.get(i).abs();
                        locMaxIdx = i;
                    */
/*}*//*

                }
                else
                {
                    if (locMax > 0)
                    {
                        double curFreq = ((double)(locMaxIdx+1)*sampleRate)/wSize;
                        curNotes.add(NotesProvider.getNearestNote(curFreq));
                    }
                    locMax = -999;
                }
            }


            final StringBuilder notestStr2 = new StringBuilder();

            for (Note n : curNotes)
            {
                n.setDuration(n.getDuration() + 1);
                notestStr2.append(n.getName() + " ");
            }
            prevNotes.clear();
            prevNotes.addAll(curNotes);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {notesPanel.setNotesLabel(notestStr2.toString());}
            });


            offset += step;

            // Following the music. Some sort of way.
            if (wf.isCanPlay() && wf.getClip().isActive())
            {
                //System.out.println(offset + " " + wf.getClip().getLongFramePosition());
                while (true)
                {
                    long pos = wf.getClip().getLongFramePosition();
                    if ((offset < pos) || (!wf.getClip().isActive()))
                        break;
                }
            }
            else
            {
                try {
                    Thread.sleep(50); // Approximate value
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            recogParamChangesLock.unlock();
        }
        wf.stop();
        graphicPanel.cleanTraces();
    }

    public void startSaving(String outputFileFullPath, final ProgressMonitor pg)
    {
        // If no file was picked
        if (wf == null)
            return;

        ArrayList<Complex> counts = new ArrayList<Complex>(0);
        ArrayList<Complex> freq = new ArrayList<Complex>(0);

        int offset = 0;

        HashSet<Note> prevNotes = new HashSet<Note>();
        Sequence s = null;
        try { s = new Sequence(0, Math.round((float)sampleRate / (2*step))); }
        catch (InvalidMidiDataException e) { e.printStackTrace(); }
        Track t = s.createTrack();

        while (!Thread.currentThread().isInterrupted() && (offset < framesCount) && !pg.isCanceled())
        {
            counts.clear();
            for (int i = 0+offset; i < wSize+offset; i++)
            {
                if (i < framesCount)
                    counts.add(new Complex(wf.getSampleInt(i)));
                else
                    for (; i < wSize+offset; i++)
                        counts.add(new Complex(0));
            }

            //APPLY WINDOW
            int q = 0;
            for (Complex c : counts)
            {
                c.x = c.x * currentWindow.getWindowValue(q, wSize);
                q++;
            }

            freq.clear();
            freq.addAll(counts);

            FastFourierTransform.FFT(freq, false);



            HashSet<Note> curNotes = new HashSet<Note>();
            double locMax = -999;
            int locMaxIdx = -1;
            for (int i = 0; i < freq.size()/2; i++)
            {
                if ((freq.get(i).abs() > threshold) && (freq.get(i).abs() >= locMax))
                {

                    locMax = freq.get(i).abs();
                    locMaxIdx = i;
                }
                else
                {
                    if (locMax > 0)
                    {
                        double curFreq = ((double)(locMaxIdx+1)*sampleRate)/wSize;
                        curNotes.add(NotesProvider.getNearestNote(curFreq));
                    }
                    locMax = -999;
                }
            }

            if (prevNotes.size() > 0)
            {
                for (Note n : prevNotes)
                {
                    if (!curNotes.contains(n))
                    {
                        ShortMessage myMsgOn = new ShortMessage();
                        ShortMessage myMsgOff = new ShortMessage();
                        try
                        {
                            myMsgOn.setMessage(ShortMessage.NOTE_ON, 0, n.getCode(), 127);
                            myMsgOff.setMessage(ShortMessage.NOTE_OFF, 0, n.getCode(), 127);
                        } catch (InvalidMidiDataException e) { e.printStackTrace(); }


                        int curPos = offset / step;
                        MidiEvent me1 = new MidiEvent(myMsgOn, curPos - n.getDuration());
                        MidiEvent me2 = new MidiEvent(myMsgOff, curPos);

                        t.add(me1);
                        t.add(me2);
                    }
                }
            }
            for (Note n : curNotes)
            {
                n.setDuration(n.getDuration() + 1);
            }
            prevNotes.clear();
            prevNotes.addAll(curNotes);

            final int progressPercentage = (int)(((double)offset / framesCount) * 100);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pg.setNote("Completed " + progressPercentage + "%.");
                    pg.setProgress(progressPercentage);
                }
            });

            offset += step;
        }

        if (!pg.isCanceled())
        {
            try
            {
                File outputFile = new File(outputFileFullPath);
                if (!outputFile.exists() | (outputFile.exists() && outputFile.delete()))
                    MidiSystem.write(s, 0, outputFile);
            }
            catch (IOException e) { e.printStackTrace(); }
            pg.setProgress(pg.getMaximum());
        }
    }
*/

}
