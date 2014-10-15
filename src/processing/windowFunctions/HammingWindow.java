package processing.windowFunctions;

public class HammingWindow implements IWindowFunction
{
    @Override
    public double getWindowValue(int k, int n)
    {
        return 0.54 - 0.46 * Math.cos((2*Math.PI*k)/(n-1));
    }
}
