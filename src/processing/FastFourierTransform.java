package processing;

import java.util.ArrayList;

public class FastFourierTransform
{
    public static void FFT(ArrayList<Complex> a, boolean invert)
    {
        int n = a.size();
        if (n == 1)  return;

        ArrayList<Complex> a0 = new ArrayList<Complex>(n/2);
        ArrayList<Complex> a1 = new ArrayList<Complex>(n/2);

        for (int i=0, j=0; i<n; i+=2, ++j) {
            a0.add(a.get(i));
            a1.add(a.get(i + 1));
        }
        FFT(a0, invert);
        FFT(a1, invert);

        double ang = 2 * Math.PI/n * (invert ? 1 : -1);
        Complex w = new Complex(1);
        Complex wn = new Complex(Math.cos(ang), Math.sin(ang));
        for (int i=0; i<n/2; ++i) {
            a.set(i, a0.get(i).add(a1.get(i).mul(w)));
            a.set(i+n/2, a0.get(i).sub(a1.get(i).mul(w)));
            if (invert)
            {
                a.set(i, a.get(i).mul(0.5));
                a.set(i+n/2, a.get(i+n/2).mul(0.5));
            }
            w = w.mul(wn);
        }
    }
}

