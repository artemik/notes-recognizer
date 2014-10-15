package processing;

import java.util.LinkedList;

public class Note
{
    private String           name;
    private double           freq;
    private int              code;
    private int              duration;
    private int              lIdx;
    private int              rIdx;
    private double           lMean;
    private double           rMean;
    private double           prevMagnitude;
    private LinkedList<Note> harmonics;

    public Note(String name, double freq, int code)
    {
        this.name = name;
        this.freq = freq;
        this.code = code;
        this.duration = 0;
        this.prevMagnitude = -1;
    }

    public Note(String name, double freq, int code, int lIdx, int rIdx, double lMean, double rMean)
    {
        this.name = name;
        this.freq = freq;
        this.code = code;
        this.lIdx = lIdx;
        this.rIdx = rIdx;
        this.lMean = lMean;
        this.rMean = rMean;
        harmonics = new LinkedList<Note>();
        this.prevMagnitude = -1;
        this.duration = 0;
    }

    public void addHarmonicNote(Note n)
    {
        if (!harmonics.contains(n))
            harmonics.add(n);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getFreq() { return freq; }
    public void setFreq(double freq) { this.freq = freq; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getLeftIdx() { return lIdx; }
    public void setLeftIdx(int lIdx) { this.lIdx = lIdx; }

    public int getRightIdx() { return rIdx; }
    public void setRightIdx(int rIdx) { this.rIdx = rIdx; }

    public double getRightMean() { return rMean; }
    public void setRightMean(double rMean) { this.rMean = rMean; }

    public double getLeftMean() {  return lMean; }
    public void setLeftMean(double lMean) {this.lMean = lMean; }

    public double getPrevMagnitude() { return prevMagnitude; }
    public void setPrevMagnitude(double prevMagnitude) { this.prevMagnitude = prevMagnitude; }

    public LinkedList<Note> getHarmonicsList() { return harmonics; }

    @Override
    public boolean equals(Object obj)
    {
        Note n = (Note)obj;
        return getName().equals(n.getName());
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }


}