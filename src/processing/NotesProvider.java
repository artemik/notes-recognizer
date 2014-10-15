package processing;

import java.util.Iterator;
import java.util.LinkedList;

public class NotesProvider
{
    private static final LinkedList<Note> notes = new LinkedList<Note>();
    private static boolean ready = false;

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

    public static Note getNearestNote(double freq)
    {
        Iterator<Note> iter = notes.iterator();
        Note nearestNote = null;
        double minDist = Double.MAX_VALUE;
        boolean found = false;
        while (iter.hasNext() && !found)
        {
            Note curNote = iter.next();
            double curDist = Math.abs(freq - curNote.getFreq());
            found = true;
            if (curDist <= minDist)
            {
                minDist = curDist;
                nearestNote = curNote;
                found = false;
            }
        }
        return new Note(nearestNote.getName(), nearestNote.getFreq(), nearestNote.getCode());
    }


    public static void init()
    {
        if (!ready)
        {
            int startCode = 21; //Java code of A0
            int endCode = 119; // Java code of B8
            for (int i = startCode; i <= endCode; i++)
            {
                double freq = 27.5 * Math.pow(2, (i-(double)startCode)/12);
                notes.addLast(new Note(getName(i), freq, i));
            }
        }
        ready = true;
    }

    /*static
    {
        int startCode = 21; //Java code of A0
        int endCode = 119; // Java code of B8
        for (int i = startCode; i <= endCode; i++)
        {
            double freq = 27.5 * Math.pow(2, (i-(double)startCode)/12);
            notes.addLast(new Note(getName(i), freq, i));
        }
    }*/
}
