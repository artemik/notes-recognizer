package processing.windowFunctions;

public interface IWindowFunction
{
    public abstract double getWindowValue(int k, int n);
}
