package processing.windowFunctions;

public class HannWindow implements IWindowFunction
{
    @Override
    public double getWindowValue(int k, int n)
    {
        return 0.5 * (1 - Math.cos( (2*Math.PI*k)/(n-1) ));
    }
}
