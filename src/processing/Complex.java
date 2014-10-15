package processing;

public class Complex
{
    double x;
    double y;

    public Complex(double x) {
        this.x = x;
    }

    public Complex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Complex conj() {
        return new Complex(x, -y);
    }

    public Complex sub(Complex b) {
        return new Complex(x - b.x, y - b.y);
    }

    public Complex add(Complex b) {
        return new Complex(x + b.x, y + b.y);
    }

    public Complex mul(Complex b) {
        return new Complex(x * b.x - y * b.y, x * b.y + y * b.x);
    }

    public Complex div(Complex b) {
        return this.mul(b.conj()).mul(1 / b.len2());
    }

    public Complex mul(double b) {
        return new Complex(x * b, y * b);
    }

    public double len2() {
        return x * x + y * y;
    }

    public double abs() {
        return Math.sqrt(x * x + y * y);
    }

    public Complex norm() {
        return abs() == 0 ? new Complex(0, 0) : mul(1 / abs());
    }

    public double cross(Complex b) {
        return x * b.y - y * b.x;
    }

    public double dot(Complex b) {
        return x * b.x + y * b.y;
    }

    public static Complex polar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }

    public static Complex exp(Complex a) {
        return polar(Math.exp(a.x), a.y);
    }

    public double arg() {
        return Math.atan2(y, x);
    }

    public Complex rot90() {
        return new Complex(-y, x);
    }

    public Complex rotate(Complex p, double angle) {
        return p.sub(this).mul(exp(new Complex(0, angle))).add(this);
    }

    public Complex rotate2(Complex p, double angle) {
        p = p.sub(this);
        double cs = Math.cos(angle);
        double sn = Math.sin(angle);
        return new Complex(p.x * cs - p.y * sn, p.x * sn + p.y * cs).add(this);
    }

    public Complex reflect(Complex p, Complex q) {
        Complex s = q.sub(p);
        return this.sub(p).div(s).conj().mul(s).add(p);
    }

    public double proj(Complex p) {
        return dot(p) / abs();
    }

    @Override
    public String toString() {
        return "Complex [x=" + x + ", y=" + y + "]";
    }
}
